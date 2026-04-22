package main

import (
	"context"
	"errors"
	"time"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
)

// A "pool" keeps several open database connections ready to use.
// Without a pool, every request would: open connection → query → close.
// Opening a TCP connection to Postgres takes ~10ms — too slow per request.
// The pool opens connections once at startup and reuses them.
// pgxpool is safe to use from many goroutines at the same time (concurrent requests).

// queryTimeout is the maximum time any single DB query is allowed to run.
// If Postgres is slow or hung, the query is canceled after this duration
// instead of blocking the goroutine (and the connection) forever.
// Tune this to 2–3× your normal p99 query latency.
const queryTimeout = 3 * time.Second

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

// NewRepository connects to Postgres, configures the connection pool,
// and creates the users table if it doesn't already exist.
// Called once at startup from main.go.
func NewRepository(connStr string) (*Repository, error) {
	// Parse the connection string into a config struct so we can
	// tune pool behavior before opening any connections.
	config, err := pgxpool.ParseConfig(connStr)
	if err != nil {
		return nil, err
	}

	// --- Pool tuning ---
	//
	// MaxConns: upper bound on open connections.
	// Without this, a traffic spike creates hundreds of connections and
	// can crash Postgres. Set it to match your DB's max_connections minus
	// headroom for admin tools (a common rule: total_max_connections * 0.8).
	config.MaxConns = 10

	// MinConns: connections kept open even when idle.
	// Avoids the latency spike of opening a fresh TCP connection on the
	// first request after a quiet period.
	config.MinConns = 2

	// MaxConnLifetime: recycle connections after this duration.
	// Prevents stale connections that have been silently closed by a
	// firewall or load balancer from sitting in the pool undetected.
	config.MaxConnLifetime = 30 * time.Minute

	// MaxConnIdleTime: close connections that haven't been used recently.
	// Frees DB-side resources during low-traffic periods.
	config.MaxConnIdleTime = 5 * time.Minute

	// HealthCheckPeriod: how often the pool pings idle connections in the
	// background to detect ones that have gone stale.
	// Complements MaxConnLifetime: that recycles by age, this detects dead ones.
	config.HealthCheckPeriod = 1 * time.Minute

	pool, err := pgxpool.NewWithConfig(context.Background(), config)
	if err != nil {
		return nil, err
	}

	// Ping verifies at least one connection is actually alive.
	// Without this, the app starts fine but crashes on the first query.
	// We give it a tight deadline — if Postgres isn't up in 5s at startup,
	// we fail fast rather than hanging.
	pingCtx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()
	if err := pool.Ping(pingCtx); err != nil {
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
	// Migration runs at startup so we give it a generous timeout.
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

// Ping checks whether the database is reachable right now.
// Used by the /health handler so load balancers get an accurate signal —
// a 200 from /health should mean "this instance can actually serve requests",
// not just "the process is alive".
func (r *Repository) Ping(ctx context.Context) error {
	return r.pool.Ping(ctx)
}

// FindByUsername looks up a user by their username.
//
// Returns (nil, nil)   if the user doesn't exist — not an error, just absent.
// Returns (nil, err)   on a real database error (including timeout).
// Returns (*User, nil) on success.
func (r *Repository) FindByUsername(username string) (*User, error) {
	// queryTimeout caps how long we wait for Postgres.
	// If the DB is overloaded or the network is partitioned, the query is
	// cancelled after queryTimeout and the caller gets an error instead of
	// blocking indefinitely.
	ctx, cancel := context.WithTimeout(context.Background(), queryTimeout)
	defer cancel()

	var u User
	err := r.pool.QueryRow(
		ctx,
		"SELECT id, username, password_hash FROM users WHERE username = $1",
		username,
	).Scan(&u.ID, &u.Username, &u.PasswordHash)

	if errors.Is(err, pgx.ErrNoRows) {
		return nil, nil // not found — not an error, just absent
	}
	if err != nil {
		return nil, err // real DB error or timeout
	}

	return &u, nil
}

// CreateUser inserts a new user and returns the created record with its
// DB-generated ID.
//
// RETURNING id tells Postgres to give us back the auto-generated primary key
// without needing a second SELECT query.
func (r *Repository) CreateUser(username, passwordHash string) (*User, error) {
	ctx, cancel := context.WithTimeout(context.Background(), queryTimeout)
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
