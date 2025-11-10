# Getting Started 

## ui and Kotlin Multi Platform

## backend and Hexagonal architecture in go
Go REST API development:

- Go Official Docs: https://go.dev/doc/
- Chi Router (popular for REST): https://github.com/go-chi/chi
- Go net/http: https://pkg.go.dev/net/http
- OpenAPI for Go: https://swagger.io/docs/specification/about/

### gRPC between Go and KMP:

- gRPC in Go: https://grpc.io/docs/languages/go/quickstart/
- gRPC in Kotlin (Android/KMP): https://grpc.io/docs/languages/kotlin/basics/
- Protocol Buffers (proto): https://developers.google.com/protocol-buffers

### Ktor for API Clients in KMP:

- Ktor Client Multiplatform: https://ktor.io/docs/getting-started-ktor-client.html
- Consuming HTTP APIs with Ktor: https://ktor.io/docs/http-client.html
- Ktor for Android/iOS: https://ktor.io/docs/multiplatform.html

### Kotlin Multiplatform official guides:

- KMP Official Docs: https://kotlinlang.org/docs/multiplatform.html
- Working with Gradle & dependency management: https://kotlinlang.org/docs/gradle.html

### API contract generation and testing:

- OpenAPI Specification: https://swagger.io/specification/
- Generating clients from OpenAPI: https://openapi-generator.tech/docs/generators/kotlin
- Mock server/testing: https://stoplight.io/open-source/prism/


### Final Structure
```Text
cmd/api/                    // Application entrypoint; wires everything
internal/
  domain/                   // Entities, value objects, business logic
  service/                  // Use-cases, orchestrators, business rules
  port/
    inbound/                // Interfaces for use-cases (services)
    outbound/               // Interfaces for repositories, external calls
  adapter/
    http/                   // HTTP REST/Gin/chi handler implementations
    grpc/                   // gRPC server implementations
    pg/                     // Postgres implementation of repositories
    external_api/           // Adapters to other APIs
pkg/                        // Utilities, logging, config, common libs
```