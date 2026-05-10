package main

import (
"net"
"net/http"
"sync"

"golang.org/x/time/rate"
)

// ipRateLimiter holds one token-bucket limiter per client IP.
// Each IP gets a fresh bucket the first time it's seen.
// The bucket allows a short burst (e.g. 5 rapid retries at launch)
// but then settles to a steady rate of 2 requests/second —
// plenty for a legitimate player, painful for a brute-force script.
type ipRateLimiter struct {
	mu       sync.Mutex
	limiters map[string]*rate.Limiter
	r        rate.Limit // steady-state tokens per second
	b        int        // burst size
}

func newIPRateLimiter(r rate.Limit, b int) *ipRateLimiter {
	return &ipRateLimiter{
		limiters: make(map[string]*rate.Limiter),
		r:        r,
		b:        b,
	}
}

func (l *ipRateLimiter) get(ip string) *rate.Limiter {
	l.mu.Lock()
	defer l.mu.Unlock()

	if lim, ok := l.limiters[ip]; ok {
		return lim
	}
	lim := rate.NewLimiter(l.r, l.b)
	l.limiters[ip] = lim
	return lim
}

// rateLimitMiddleware wraps a handler and rejects requests from IPs that
// exceed the configured rate. Returns 429 Too Many Requests.
func rateLimitMiddleware(next http.HandlerFunc, limiter *ipRateLimiter) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		// RemoteAddr is "ip:port" — strip the port before looking up the limiter.
		ip, _, err := net.SplitHostPort(r.RemoteAddr)
		if err != nil {
			// Shouldn't happen in practice, but fail open so legitimate
			// players aren't blocked by a parsing edge case.
			ip = r.RemoteAddr
		}

		if !limiter.get(ip).Allow() {
			writeJSON(w, http.StatusTooManyRequests, errorResponse{"too many requests, slow down"})
			return
		}

		next(w, r)
	}
}