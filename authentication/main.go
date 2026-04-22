package main

import (
	"context"
	"log"
	"net/http"
	"os"
	"time"
)

// main is responsible for:
//  1. Reading config from environment variables
//  2. Connecting to the database
//  3. Setting up the HTTP routes
//  4. Starting the server
//
// It wires together the Repository and the Handler.

func main() {
	// 1. Read config
	// We read DB config here (not in repository.go) so all config reading
	// lives in one place.
	dbURL := os.Getenv("DB_URL")
	if dbURL == "" {
		log.Fatal("DB_URL environment variable is required")
	}

	// 2. Connect to database
	// NewRepository opens the connection pool and runs the migration.
	// If Postgres isn't ready yet, this fails — docker-compose's
	// depends_on + healthcheck ensures Postgres is up before we start.
	repo, err := NewRepository(dbURL)
	if err != nil {
		log.Fatalf("failed to connect to database: %v", err)
	}
	log.Println("connected to database")

	h := Handler{repo: repo}

	mux := http.NewServeMux()

	mux.HandleFunc("POST /auth/register", h.Register)
	mux.HandleFunc("POST /auth/login", h.Login)

	// /health is used by load balancers, Docker, and Kubernetes to decide
	// whether to route traffic to this instance.
	//
	// Returning 200 when the DB is down would be a lie — the service cannot
	// actually handle auth requests without a DB. We Ping Postgres with a
	// short deadline so a hung DB doesn't make the health check hang too.
	//
	// 200 OK           → instance is healthy, send traffic
	// 503 Unavailable  → instance cannot serve requests, stop sending traffic
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

	addr := ":8080"
	log.Printf("auth-service listening on %s", addr)

	if err := http.ListenAndServe(addr, mux); err != nil {
		log.Fatalf("server error: %v", err)
	}
}
