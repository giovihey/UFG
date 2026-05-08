package main

import (
	"context"
	"errors"
	"time"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
)

const queryTimeout = 3 * time.Second

type User struct {
	ID           int
	Username     string
	PasswordHash string
}

type Repository struct {
	pool *pgxpool.Pool
}

func NewRepository(connStr string) (*Repository, error) {
	config, err := pgxpool.ParseConfig(connStr)
	if err != nil {
		return nil, err
	}

	config.MaxConns = 10
	config.MinConns = 2
	config.MaxConnLifetime = 30 * time.Minute
	config.MaxConnIdleTime = 5 * time.Minute
	config.HealthCheckPeriod = 1 * time.Minute

	pool, err := pgxpool.NewWithConfig(context.Background(), config)
	if err != nil {
		return nil, err
	}

	pingCtx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()
	if err := pool.Ping(pingCtx); err != nil {
		return nil, err
	}

	repo := &Repository{pool: pool}
	if err := repo.migrate(); err != nil {
		return nil, err
	}

	return repo, nil
}

func (r *Repository) migrate() error {
	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()

	_, err := r.pool.Exec(ctx, `
		CREATE TABLE IF NOT EXISTS users (
			id            SERIAL PRIMARY KEY,
			username      VARCHAR(50)  NOT NULL UNIQUE,
			password_hash VARCHAR(255) NOT NULL
		)
	`)
	return err
}

func (r *Repository) Ping(ctx context.Context) error {
	return r.pool.Ping(ctx)
}

// FindByUsername accepts the caller's context so that if the HTTP request
// is canceled (player disconnects, timeout), the DB query is canceled too
// and the connection is returned to the pool immediately.
func (r *Repository) FindByUsername(ctx context.Context, username string) (*User, error) {
	// We still cap with queryTimeout, but we also respect the parent context.
	// context.WithTimeout on an already-canceled ctx will itself be canceled
	// immediately — no DB round-trip is wasted.
	ctx, cancel := context.WithTimeout(ctx, queryTimeout)
	defer cancel()

	var u User
	err := r.pool.QueryRow(
		ctx,
		"SELECT id, username, password_hash FROM users WHERE username = $1",
		username,
	).Scan(&u.ID, &u.Username, &u.PasswordHash)

	if errors.Is(err, pgx.ErrNoRows) {
		return nil, nil
	}
	if err != nil {
		return nil, err
	}

	return &u, nil
}

// CreateUser accepts the caller's context for the same reason as FindByUsername.
func (r *Repository) CreateUser(ctx context.Context, username, passwordHash string) (*User, error) {
	ctx, cancel := context.WithTimeout(ctx, queryTimeout)
	defer cancel()

	var u User
	u.Username = username
	u.PasswordHash = passwordHash

	err := r.pool.QueryRow(
		ctx,
		"INSERT INTO users (username, password_hash) VALUES ($1, $2) RETURNING id",
		username,
		passwordHash,
	).Scan(&u.ID)

	if err != nil {
		return nil, err
	}

	return &u, nil
}
