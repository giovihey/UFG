package main

import (
	"context"
	"errors"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
)

// A "pool" keeps several open database connections ready to use.
// Without a pool, every request would: open connection → query → close.
// Opening a TCP connection to Postgres takes ~10ms — too slow per request.
// The pool opens connections once at startup and reuses them.
// pgxpool is safe to use from many goroutines at the same time (concurrent requests).

// User: the data we store
type User struct {
	ID           int
	Username     string
	PasswordHash string
}

// Repository: the only place that knows SQL exists
type Repository struct {
	pool *pgxpool.Pool // the shared connection pool
}

// NewRepository connects to Postgres and creates the users table if it
// doesn't already exist. Called once at startup from main.go.
func NewRepository(connStr string) (*Repository, error) {
	// pgxpool.New parses the connection string and creates the pool.
	// connStr looks like: "postgres://user:pass@host:5432/dbname"
	pool, err := pgxpool.New(context.Background(), connStr)
	if err != nil {
		return nil, err
	}

	// Ping verifies the connection is actually alive.
	// Without this, the app starts fine but crashes on the first query.
	if err := pool.Ping(context.Background()); err != nil {
		return nil, err
	}

	repo := &Repository{pool: pool}

	// Create the table if it doesn't exist yet.
	// This is simple for development — in production you'd use migrations.
	if err := repo.migrate(); err != nil {
		return nil, err
	}

	return repo, nil
}

func (r *Repository) migrate() error {
	// UNIQUE on username prevents duplicate accounts.
	// IF NOT EXISTS makes this safe to run every time the app starts.
	_, err := r.pool.Exec(context.Background(), `
		CREATE TABLE IF NOT EXISTS users (
			id            SERIAL PRIMARY KEY,
			username      VARCHAR(50)  NOT NULL UNIQUE,
			password_hash VARCHAR(255) NOT NULL
		)
	`)
	return err
}

// Returns (nil, nil) if the user doesn't exist.
// Returns (nil, err) on a real database error.
// Returns (*User, nil) on success.
func (r *Repository) FindByUsername(username string) (*User, error) {
	var u User

	err := r.pool.QueryRow(
		context.Background(),
		"SELECT id, username, password_hash FROM users WHERE username = $1",
		username,
	).Scan(&u.ID, &u.Username, &u.PasswordHash)

	if errors.Is(err, pgx.ErrNoRows) {
		return nil, nil // not found — not an error, just absent
	}
	if err != nil {
		return nil, err // real DB error
	}

	return &u, nil
}

// Inserts a new user and returns the created record with its DB-generated ID.
// RETURNING id tells Postgres to give us back the auto-generated primary key
// without needing a second SELECT query.
func (r *Repository) CreateUser(username, passwordHash string) (*User, error) {
	var u User
	u.Username = username
	u.PasswordHash = passwordHash

	err := r.pool.QueryRow(
		context.Background(),
		"INSERT INTO users (username, password_hash) VALUES ($1, $2) RETURNING id",
		username,
		passwordHash,
	).Scan(&u.ID)

	if err != nil {
		return nil, err
	}

	return &u, nil
}
