package main

import (
	"encoding/json"
	"log"
	"net/http"
)

// Handler struct
// We group both handlers into a struct so they share the repository.
// main.go creates one Handler{repo: repo} and passes it to the router.
type Handler struct {
	repo *Repository
}

// Request / Response shapes
// Go uses plain structs for JSON. The `json:"..."` tag tells the JSON encoder
// what field name to use when serializing/deserializing.
// Without tags, Go uses the field name as-is (capitalized) in JSON.

type authRequest struct {
	Username string `json:"username"`
	Password string `json:"password"`
}

type authResponse struct {
	Token string `json:"token"`
}

type errorResponse struct {
	Error string `json:"error"`
}

func writeJSON(w http.ResponseWriter, status int, body any) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	json.NewEncoder(w).Encode(body)
}

// POST /auth/register
//
// Flow:
//  1. Parse JSON body → authRequest
//  2. Validate (not blank, password long enough)
//  3. Check if username already exists
//  4. Hash the password with BCrypt
//  5. Store the user in Postgres
//  6. Generate and return a JWT
func (h *Handler) Register(w http.ResponseWriter, r *http.Request) {
	// Step 1: parse JSON body
	var req authRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		// The client sent malformed JSON
		writeJSON(w, http.StatusBadRequest, errorResponse{"invalid JSON body"})
		return
	}

	// Step 2: validate
	if req.Username == "" || len(req.Password) < 6 {
		writeJSON(w, http.StatusBadRequest, errorResponse{
			"username required and password must be at least 6 characters",
		})
		return
	}

	// Step 3: check for duplicate
	existing, err := h.repo.FindByUsername(req.Username)
	if err != nil {
		log.Printf("register: db error: %v", err)
		writeJSON(w, http.StatusInternalServerError, errorResponse{"internal error"})
		return
	}
	if existing != nil {
		writeJSON(w, http.StatusConflict, errorResponse{"username already taken"})
		return
	}

	// Step 4: hash the password
	hash, err := hashPassword(req.Password)
	if err != nil {
		log.Printf("register: bcrypt error: %v", err)
		writeJSON(w, http.StatusInternalServerError, errorResponse{"internal error"})
		return
	}

	// Step 5: persist
	user, err := h.repo.CreateUser(req.Username, hash)
	if err != nil {
		log.Printf("register: create user error: %v", err)
		writeJSON(w, http.StatusInternalServerError, errorResponse{"internal error"})
		return
	}

	// Step 6: generate JWT — the client is now logged in
	token, err := GenerateToken(user.ID, user.Username)
	if err != nil {
		log.Printf("register: token error: %v", err)
		writeJSON(w, http.StatusInternalServerError, errorResponse{"internal error"})
		return
	}

	// 201 Created: a new resource was created successfully
	writeJSON(w, http.StatusCreated, authResponse{Token: token})
}

// POST /auth/login
//
// Flow:
//  1. Parse JSON body
//  2. Look up the user by username
//  3. Verify the password against the stored BCrypt hash
//  4. Generate and return a JWT
func (h *Handler) Login(w http.ResponseWriter, r *http.Request) {
	var req authRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		writeJSON(w, http.StatusBadRequest, errorResponse{"invalid JSON body"})
		return
	}

	user, err := h.repo.FindByUsername(req.Username)
	if err != nil {
		log.Printf("login: db error: %v", err)
		writeJSON(w, http.StatusInternalServerError, errorResponse{"internal error"})
		return
	}
	// Both "user not found" and "wrong password" return 401 Unauthorized
	// with the same message.
	if user == nil || !checkPassword(user.PasswordHash, req.Password) {
		writeJSON(w, http.StatusUnauthorized, errorResponse{"invalid credentials"})
		return
	}

	token, err := GenerateToken(user.ID, user.Username)
	if err != nil {
		log.Printf("login: token error: %v", err)
		writeJSON(w, http.StatusInternalServerError, errorResponse{"internal error"})
		return
	}

	writeJSON(w, http.StatusOK, authResponse{Token: token})
}
