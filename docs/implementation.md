# Implementation

This section discusses technology-dependent implementation choices made during system realization.

## Network Protocols

### Protocols Used

**Protocol: [HTTP/REST]**

- **Why Chosen**: [Standard web protocol, easy integration, widespread support]
- **Use Cases**: [CRUD operations, user-facing APIs]
- **Version**: [HTTP/1.1 / HTTP/2 / HTTP/3]
- **Example Endpoint**:
  ```
  GET /api/users/123
  Content-Type: application/json
  
  Response:
  {
    "id": "123",
    "name": "John Doe",
    "email": "john@example.com"
  }
  ```

---

**Protocol: [WebSocket]**

- **Why Chosen**: [Real-time bidirectional communication]
- **Use Cases**: [Live notifications, real-time updates]
- **Message Format**: [JSON / Binary]
- **Example**:
  ```
  Client → Server: {"type": "subscribe", "channel": "notifications"}
  Server → Client: {"type": "event", "data": {...}}
  ```

---

**Protocol: [gRPC]**

- **Why Chosen**: [High-performance inter-service communication]
- **Use Cases**: [Service-to-service communication]
- **IDL Language**: [Protocol Buffers]

---

**Protocol: [AMQP / MQTT / NATS]**

- **Why Chosen**: [Asynchronous messaging]
- **Use Cases**: [Event publishing, task queuing]
- **Message Types**: [Events / Commands / Responses]

## Data Serialization Formats

### Format: JSON

- **Why Chosen**: [Human-readable, widely supported, native to JavaScript/web]
- **Use Cases**: [REST APIs, Configuration, Logging]
- **Example**:
  ```json
  {
    "event_type": "UserCreated",
    "timestamp": "2025-11-11T21:09:00Z",
    "data": {
      "user_id": "123",
      "name": "John Doe"
    }
  }
  ```
- **Schema Validation**: [JSON Schema / OpenAPI]

### Format: Protocol Buffers

- **Why Chosen**: [Compact, typed, efficient serialization]
- **Use Cases**: [gRPC communication, internal messages]
- **Schema**:
  ```protobuf
  message User {

  }
  ```

### Format: MessagePack

- **Why Chosen**: [Binary format, smaller than JSON, fast]
- **Use Cases**: [High-throughput messaging]

## Database Technology

### Primary Database: [PostgreSQL / MongoDB / DynamoDB / etc.]

**Why Chosen**: [Reliability, ACID compliance, rich querying, open-source]

**Version**: [14.x / 15.x]

**Setup**:
```bash
docker run -e POSTGRES_PASSWORD=secret \
  -p 5432:5432 \
  postgres:15
```

**Connection String**:
```
postgresql://user:password@localhost:5432/database
```

### Caching Layer: [Redis / Memcached]

**Why Chosen**: [In-memory, fast key-value store, persistence options]

**Configuration**:
```
maxmemory: 2gb
maxmemory-policy: allkeys-lru
```

### Search Engine: [Elasticsearch / Meilisearch / etc.]

**Why Chosen**: [Full-text search, faceted search]

**Index Configuration**:
```json
{
  "settings": {
    "number_of_shards": 3,
    "number_of_replicas": 2
  },
  "mappings": {
    "properties": {
      "title": {"type": "text"},
      "description": {"type": "text"}
    }
  }
}
```

## Authentication Implementation

### JWT (JSON Web Tokens)

**Token Structure**:
```
Header.Payload.Signature

Example:
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.
eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.
SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c
```

**Token Payload**:
```json
{
  "sub": "user123",
  "iss": "https://api.example.com",
  "aud": "https://example.com",
  "iat": 1699729800,
  "exp": 1699729860,
  "scopes": ["read:users", "write:posts"]
}
```

**Refresh Token Strategy**:
- Access token: 15 minutes
- Refresh token: 7 days
- Use refresh token to get new access token

### OAuth 2.0

**Authorization Code Flow**:

```mermaid
sequenceDiagram
    participant User
    participant Client as Application
    participant AuthSrv as Auth Server
    
    User->>Client: Click "Login"
    Client->>AuthSrv: Redirect with client_id
    AuthSrv->>User: Show consent screen
    User->>AuthSrv: Grant permission
    AuthSrv->>Client: Return authorization code
    Client->>AuthSrv: Exchange code for token
    AuthSrv->>Client: Return access token
    Client->>Client: Create session
```

## Authorization Implementation

### RBAC Implementation

**Database Schema**:

```sql
CREATE TABLE roles (
    role_id SERIAL PRIMARY KEY,
    role_name VARCHAR(50) NOT NULL
);

CREATE TABLE permissions (
    permission_id SERIAL PRIMARY KEY,
    permission_name VARCHAR(100) NOT NULL
);

CREATE TABLE role_permissions (
    role_id INT REFERENCES roles(role_id),
    permission_id INT REFERENCES permissions(permission_id),
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE user_roles (
    user_id UUID REFERENCES users(user_id),
    role_id INT REFERENCES roles(role_id),
    PRIMARY KEY (user_id, role_id)
);
```

**Authorization Check**:

```python
def has_permission(user_id, required_permission):
    # Get user roles
    roles = db.query("""
        SELECT r.role_name 
        FROM user_roles ur
        JOIN roles r ON ur.role_id = r.role_id
        WHERE ur.user_id = %s
    """, [user_id])
    
    # Check if any role has permission
    for role in roles:
        if check_role_permission(role, required_permission):
            return True
    return False
```

## Containerization

### Docker

**Dockerfile**:

```dockerfile
FROM python:3.11-slim

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

EXPOSE 8000

CMD ["python", "-m", "uvicorn", "main:app", "--host", "0.0.0.0"]
```

**Docker Compose**:

```yaml
version: '3.8'

services:
  app:
    build: .
    ports:
      - "8000:8000"
    environment:
      DATABASE_URL: postgresql://user:pass@db:5432/app
      REDIS_URL: redis://cache:6379
    depends_on:
      - db
      - cache
    
  db:
    image: postgres:15
    environment:
      POSTGRES_PASSWORD: secret
      POSTGRES_DB: app
    volumes:
      - postgres_data:/var/lib/postgresql/data
    
  cache:
    image: redis:7
    ports:
      - "6379:6379"

volumes:
  postgres_data:
```

## Message Queue/Broker

### Technology: [RabbitMQ / Kafka / AWS SQS]

**Configuration**:

```yaml
# RabbitMQ example
vhost: /
user: guest
password: guest

queues:
  - name: user.events
    durable: true
    arguments:
      x-message-ttl: 86400000

exchanges:
  - name: events
    type: topic
    durable: true

bindings:
  - exchange: events
    queue: user.events
    routing_key: 'user.*'
```

**Producer Example**:

```python
import pika

connection = pika.BlockingConnection(pika.ConnectionParameters('localhost'))
channel = connection.channel()

channel.exchange_declare(exchange='events', exchange_type='topic')

message = '{"user_id": "123", "action": "created"}'
channel.basic_publish(
    exchange='events',
    routing_key='user.created',
    body=message
)
```

## Monitoring and Observability

### Logging

**Technology**: [ELK Stack / Splunk / Grafana Loki / CloudWatch]

**Log Levels**: [DEBUG / INFO / WARN / ERROR / CRITICAL]

**Structured Logging**:

```python
import json
import logging

logging.basicConfig(
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# Structured log entry
log_entry = {
    "timestamp": "2025-11-11T21:09:00Z",
    "service": "auth-service",
    "level": "INFO",
    "message": "User login successful",
    "user_id": "123",
    "ip_address": "192.168.1.1"
}
logger.info(json.dumps(log_entry))
```

### Metrics

**Technology**: [Prometheus / StatsD / Datadog]

**Key Metrics**:

```
# Request metrics
http_requests_total{method="GET", path="/api/users"}
http_request_duration_seconds{path="/api/users"}

# Application metrics
user_registrations_total
active_sessions

# System metrics
system_cpu_percent
system_memory_bytes
```

### Tracing

**Technology**: [Jaeger / Zipkin / AWS X-Ray]

**Example Trace**:

```
User Request
├─ API Gateway (2ms)
├─ Authentication Service (5ms)
│  ├─ DB Query (3ms)
│  └─ Cache Check (1ms)
├─ User Service (8ms)
│  ├─ DB Query (6ms)
│  └─ Cache Write (1ms)
└─ Response (1ms)

Total: 16ms
```

## CI/CD Pipeline

### Build Pipeline

```yaml
stages:
  - build
  - test
  - deploy

build:
  image: python:3.11
  stage: build
  script:
    - pip install -r requirements.txt
    - python setup.py build

test:
  image: python:3.11
  stage: test
  script:
    - pip install -r requirements.txt
    - pytest --cov

deploy:
  image: docker:latest
  stage: deploy
  script:
    - docker build -t myapp:latest .
    - docker push myapp:latest
  only:
    - main
```

## Framework and Libraries

### Backend Framework: [FastAPI / Django / Spring / Node.js]

### Frontend Framework: [React / Vue / Angular / Svelte]

### Testing Framework: [pytest / Jest / JUnit]

### API Documentation: [Swagger/OpenAPI / GraphQL / AsyncAPI]
