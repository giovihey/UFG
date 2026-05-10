package main

import (
	"encoding/json"
	"log"
	"net/http"
)

type Handler struct {
	repo *Repository
}

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
	// Log encoding errors — we can't change the status code at this point
	// (headers are already sent), but we at least get visibility in logs.
	if err := json.NewEncoder(w).Encode(body); err != nil {
		log.Printf("writeJSON: encode error: %v", err)
	}
}

// POST /auth/register
func (h *Handler) Register(w http.ResponseWriter, r *http.Request) {
	var req authRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		writeJSON(w, http.StatusBadRequest, errorResponse{"invalid JSON body"})
		return
	}

	if req.Username == "" || len(req.Password) < 6 {
		writeJSON(w, http.StatusBadRequest, errorResponse{
			"username required and password must be at least 6 characters",
		})
		return
	}

	// Pass r.Context() so that if the player disconnects, the DB query is
	// canceled immediately and the connection is returned to the pool.
	existing, err := h.repo.FindByUsername(r.Context(), req.Username)
	if err != nil {
		log.Printf("register: db error: %v", err)
		writeJSON(w, http.StatusInternalServerError, errorResponse{"internal error"})
		return
	}
	if existing != nil {
		writeJSON(w, http.StatusConflict, errorResponse{"username already taken"})
		return
	}

	hash, err := hashPassword(req.Password)
	if err != nil {
		log.Printf("register: bcrypt error: %v", err)
		writeJSON(w, http.StatusInternalServerError, errorResponse{"internal error"})
		return
	}

	// Pass r.Context() here too.
	user, err := h.repo.CreateUser(r.Context(), req.Username, hash)
	if err != nil {
		log.Printf("register: create user error: %v", err)
		writeJSON(w, http.StatusInternalServerError, errorResponse{"internal error"})
		return
	}

	token, err := GenerateToken(user.ID, user.Username)
	if err != nil {
		log.Printf("register: token error: %v", err)
		writeJSON(w, http.StatusInternalServerError, errorResponse{"internal error"})
		return
	}

	writeJSON(w, http.StatusCreated, authResponse{Token: token})
}

// POST /auth/login
func (h *Handler) Login(w http.ResponseWriter, r *http.Request) {
	var req authRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		writeJSON(w, http.StatusBadRequest, errorResponse{"invalid JSON body"})
		return
	}

	// Pass r.Context() so a dropped connection cancels the DB lookup.
	user, err := h.repo.FindByUsername(r.Context(), req.Username)
	if err != nil {
		log.Printf("login: db error: %v", err)
		writeJSON(w, http.StatusInternalServerError, errorResponse{"internal error"})
		return
	}
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
