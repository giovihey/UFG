package main

import (
	"log"
	"net/http"
	"os"
)

// It's responsible for:
//   1. Reading config from environment variables
//   2. Connecting to the database
//   3. Setting up the HTTP routes
//   4. Starting the server
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

	// Use this to know if the service is alive.
	mux.HandleFunc("GET /health", func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
	})

	addr := ":8080"
	log.Printf("auth-service listening on %s", addr)

	if err := http.ListenAndServe(addr, mux); err != nil {
		log.Fatalf("server error: %v", err)
	}
}
