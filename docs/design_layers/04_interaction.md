# Design - Interaction

## Communication Patterns

Describe how components communicate and interact:

### Pattern: [Pattern Name]

**Description**: [What is this pattern and when is it used?]

**Components Involved**: [Which components participate?]

**Message Flow**: 

```mermaid
sequenceDiagram
    participant A as Component A
    participant B as Component B
    participant C as Component C
    
    A->>B: Message 1
    B->>C: Message 2
    C-->>B: Response
    B-->>A: Response
```

**Timing**: [Synchronous/Asynchronous]

**Reliability**: [At-most-once / At-least-once / Exactly-once]

---

### Pattern: [Pattern Name]

[Continue with additional patterns...]

## Synchronous vs Asynchronous Communication

### Synchronous Communication

**When Used**: [Explain use cases]

**Examples**:
- Request-response over HTTP/REST
- RPC (Remote Procedure Call)
- WebSocket bidirectional communication

**Diagram**:

```mermaid
sequenceDiagram
    participant Client
    participant Server
    
    Client->>+Server: Request
    Note over Server: Processing
    Server-->>-Client: Response (Blocked)
```

### Asynchronous Communication

**When Used**: [Explain use cases]

**Examples**:
- Message queues
- Event pub/sub
- Fire-and-forget messages

**Diagram**:

```mermaid
sequenceDiagram
    participant Publisher
    participant Queue
    participant Subscriber
    
    Publisher->>+Queue: Publish Message
    Queue-->>-Publisher: Acknowledged
    Note over Queue: Message Stored
    Subscriber->>+Queue: Poll/Subscribe
    Queue-->>-Subscriber: Message
    Note over Subscriber: Process Async
```

## Interaction Scenarios

### Scenario: [Scenario Name]

**Preconditions**: [Initial state]

**Steps**:
1. [Step 1]
2. [Step 2]
3. [Step 3]

**Expected Outcome**: [Final state and results]

**Error Cases**: [What could go wrong?]

**Sequence Diagram**:

```mermaid
sequenceDiagram
    participant User
    participant UI
    participant API
    participant Service
    participant DB
    
    User->>UI: Action
    UI->>API: HTTP Request
    API->>Service: Process
    Service->>DB: Query
    DB-->>Service: Data
    Service-->>API: Result
    API-->>UI: Response
    UI-->>User: Display
```

---

### Scenario: [Scenario Name]

[Continue with additional scenarios...]

