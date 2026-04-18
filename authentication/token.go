package main

import (
	"errors"
	"os"
	"time"

	"github.com/golang-jwt/jwt/v5"
	"golang.org/x/crypto/bcrypt"
)

// Reading config from environment variables
// os.Getenv reads a variable from the process environment.
// These are set in docker-compose.yml and passed into the container.
// We read them once at package init time so the app crashes immediately
// on startup if they're missing — not silently later during a request.

var (
	jwtSecret   = mustGetenv("JWT_SECRET")   // signs and verifies tokens
	jwtIssuer   = getenvOr("JWT_ISSUER", "heyteam-auth")
	jwtAudience = getenvOr("JWT_AUDIENCE", "ufg-game")
)

func mustGetenv(key string) string {
	v := os.Getenv(key)
	if v == "" {
		// panic on startup is intentional — missing secret = broken service.
		// Better to crash loudly than silently produce invalid tokens.
		panic("required environment variable not set: " + key)
	}
	return v
}

func getenvOr(key, fallback string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return fallback
}

// Password hashing with BCrypt
// BCrypt is a one-way function: you can hash a password, but you cannot
// reverse a hash back to the original password.
//
// How verification works:
//   1. At register: hash("mypassword") → "$2a$12$xyz..." (stored in DB)
//   2. At login:    bcrypt.CompareHashAndPassword("$2a$12$xyz...", "mypassword")
//      BCrypt re-derives the hash from the candidate password using the salt
//      embedded in the stored hash, then compares. Returns nil if they match.
//
// Cost 12 means 2^12 = 4096 rounds of hashing. This makes brute-forcing slow.
// Even on fast hardware, 4096 rounds takes ~100ms per attempt.

func hashPassword(password string) (string, error) {
	// GenerateFromPassword returns the full BCrypt string including the salt.
	// The salt is random — two hashes of the same password will look different.
	bytes, err := bcrypt.GenerateFromPassword([]byte(password), 12)
	return string(bytes), err
}

func checkPassword(hash, candidate string) bool {
	// Returns nil if the password matches the hash, an error otherwise.
	err := bcrypt.CompareHashAndPassword([]byte(hash), []byte(candidate))
	return err == nil
}

// Claims defines what we put inside the JWT payload.
// jwt.RegisteredClaims handles standard fields (ExpiresAt, Issuer, Audience).
// We add our own custom fields on top.
type Claims struct {
	UserID   int    `json:"userId"`
	Username string `json:"username"`
	jwt.RegisteredClaims
}

// GenerateToken creates a signed JWT for the given user.
// The token is valid for 24 hours.
func GenerateToken(userID int, username string) (string, error) {
	claims := Claims{
		UserID:   userID,
		Username: username,
		RegisteredClaims: jwt.RegisteredClaims{
			Issuer:   jwtIssuer,
			Audience: jwt.ClaimStrings{jwtAudience},
			// time.Now().Add sets the expiry 24 hours from now.
			ExpiresAt: jwt.NewNumericDate(time.Now().Add(24 * time.Hour)),
			IssuedAt:  jwt.NewNumericDate(time.Now()),
		},
	}

	// jwt.NewWithClaims creates an unsigned token.
	// .SignedString signs it with our secret → produces the final "eyJ..." string.
	token := jwt.NewWithClaims(jwt.SigningMethodHS256, claims)
	return token.SignedString([]byte(jwtSecret))
}

// ValidateToken parses and verifies an incoming JWT string.
// Returns the claims (userId, username) if valid, an error if not.
// Used for protected routes — not needed for register/login themselves.
func ValidateToken(tokenStr string) (*Claims, error) {
	token, err := jwt.ParseWithClaims(tokenStr, &Claims{}, func(t *jwt.Token) (any, error) {
		// This callback verifies the signing method matches what we expect.
		// Without this check, an attacker could send a token signed with "none".
		if _, ok := t.Method.(*jwt.SigningMethodHMAC); !ok {
			return nil, errors.New("unexpected signing method")
		}
		return []byte(jwtSecret), nil
	})

	if err != nil {
		return nil, err
	}

	claims, ok := token.Claims.(*Claims)
	if !ok || !token.Valid {
		return nil, errors.New("invalid token")
	}

	return claims, nil
}
