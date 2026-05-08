package main

import (
	"context"
	"log"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"
)

func main() {
	// Config
	dbURL := os.Getenv("DB_URL")
	if dbURL == "" {
		log.Fatal("DB_URL environment variable is required")
	}

	// Database
	repo, err := NewRepository(dbURL)
	if err != nil {
		log.Fatalf("failed to connect to database: %v", err)
	}
	log.Println("connected to database")

	// Rate limiter
	// 2 req/s steady state, burst of 5.
	limiter := newIPRateLimiter(2, 5)

	// Routes
	h := Handler{repo: repo}
	mux := http.NewServeMux()

	mux.HandleFunc("POST /auth/register", rateLimitMiddleware(h.Register, limiter))
	mux.HandleFunc("POST /auth/login", rateLimitMiddleware(h.Login, limiter))

	mux.HandleFunc("GET /health", func(w http.ResponseWriter, r *http.Request) {
		ctx, cancel := context.WithTimeout(r.Context(), 1*time.Second)
		defer cancel()

		if err := repo.Ping(ctx); err != nil {
			log.Printf("health: db ping failed: %v", err)
			writeJSON(w, http.StatusServiceUnavailable, errorResponse{"db unavailable"})
			return
		}

		w.WriteHeader(http.StatusOK)
	})

	// Server
	srv := &http.Server{
		Addr:         ":8080",
		Handler:      mux,
		ReadTimeout:  5 * time.Second,
		WriteTimeout: 10 * time.Second,
	}

	// Graceful shutdown
	go func() {
		log.Printf("auth-service listening on %s", srv.Addr)
		if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			log.Fatalf("server error: %v", err)
		}
	}()

	quit := make(chan os.Signal, 1)
	signal.Notify(quit, syscall.SIGTERM, syscall.SIGINT)
	<-quit

	log.Println("shutdown signal received, draining in-flight requests...")

	shutdownCtx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()

	if err := srv.Shutdown(shutdownCtx); err != nil {
		log.Fatalf("graceful shutdown failed: %v", err)
	}

	log.Println("server stopped cleanly")
}
