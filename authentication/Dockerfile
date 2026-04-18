FROM golang:1.22-alpine AS build
WORKDIR /app
COPY go.mod go.sum ./
RUN go mod download

# CGO_ENABLED=0 produces a fully static binary — no libc dependency.
# GOOS=linux ensures we target Linux even if you build on Mac.
# -o auth-service names the output binary.
COPY *.go ./
RUN CGO_ENABLED=0 GOOS=linux go build -o auth-service .

FROM scratch

WORKDIR /app
COPY --from=build /app/auth-service .

EXPOSE 8080

# Required env vars (set in docker-compose.yml):
#   DB_URL       postgres://user:pass@postgres:5432/ufg
#   JWT_SECRET   long-random-string

ENTRYPOINT ["./auth-service"]
