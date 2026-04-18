module github.com/heyteam/auth-service

go 1.22

require (
	github.com/golang-jwt/jwt/v5 v5.2.1
	// pgx: the best PostgreSQL driver for Go.
	// It talks directly to Postgres without any ORM — just raw SQL.
	// v5 is the current major version.
	github.com/jackc/pgx/v5 v5.5.5

	// bcrypt: one-way password hashing.
	golang.org/x/crypto v0.22.0
)

require (
	github.com/jackc/pgpassfile v1.0.0
	github.com/jackc/pgservicefile v0.0.0-20221227161230-091c0ba34f0a
	github.com/jackc/puddle/v2 v2.2.1
	golang.org/x/sync v0.1.0
	golang.org/x/text v0.14.0
)
