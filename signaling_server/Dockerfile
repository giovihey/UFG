FROM golang:1.23-alpine AS build
WORKDIR /app
COPY go.mod go.sum ./
RUN go mod download
COPY . .
RUN go build -o signaling .

FROM alpine:3.19
COPY --from=build /app/signaling /signaling
EXPOSE 8080
CMD ["/signaling"]
