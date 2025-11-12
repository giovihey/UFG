# Design - Security

## Authentication

### Authentication Mechanism

**Method**: [Username/Password / OAuth / SAML / LDAP / Multi-factor / API Keys / JWT]

**Implementation**: [Select one or more approaches]

### Username and Password Authentication

**Password Requirements**:
- [Minimum length: 8 characters]
- [Complexity: Uppercase, lowercase, numbers, symbols]
- [Expiration: Change every 90 days]
- [History: Can't reuse last 5 passwords]

**Storage**:
- [Hash algorithm: bcrypt / scrypt / PBKDF2 / Argon2]
- [Salt: Unique salt per password]
- [Never store plain passwords]

**Password Reset**:
- [Email confirmation link]
- [Link expires after 1 hour]
- [One-time use only]

### OAuth / OpenID Connect

**Supported Providers**: [Google / GitHub / Microsoft / Facebook]

**Flow**:

```mermaid
sequenceDiagram
    participant User
    participant App
    participant Provider as OAuth Provider
    
    User->>App: Click "Sign in with Provider"
    App->>Provider: Redirect to login
    User->>Provider: Authenticate
    Provider->>App: Return auth code
    App->>Provider: Exchange code for token
    Provider->>App: Return access token
    App->>App: Create session
    App->>User: Redirect to app
```

### Multi-Factor Authentication (MFA)

**Factors**:
- Something you know (password)
- Something you have (phone, security key)
- Something you are (biometric)

**Supported Methods**: [SMS / TOTP / Email / Hardware token]

**Enforcement**: [Optional / Required for admins / Required for all users]

### API Authentication

**Method**: [API Keys / Bearer Tokens / mTLS]

**API Key Format**:

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Token Expiration**: [1 hour / 24 hours / 7 days]

**Token Refresh**: [Use refresh token to get new access token]

## Authorization

### Authorization Model

**Type**: [RBAC / ABAC / PBAC / Custom]

### Role-Based Access Control (RBAC)

**Roles**:

| Role      | Permissions                   | Description                   |
|-----------|-------------------------------|-------------------------------|
| Admin     | All                           | Full system access            |
| Moderator | View/Edit/Delete user content | Manage user-generated content |
| User      | View own data, Post content   | Standard user                 |
| Guest     | View public content           | Unauthenticated user          |

**Role Assignment**: [How are roles assigned to users?]

**Role Hierarchy**: [Admin > Moderator > User > Guest]

### Attribute-Based Access Control (ABAC)

**Attributes**:

```
User: {role: "admin", department: "finance", level: 3}
Resource: {classification: "confidential", owner: "finance"}
Action: "read"

Policy: User can read if:
  (role == "admin" AND level >= 3) OR
  (department == resource.owner)
```

### Permission Examples

```
FR1.1: View public posts
  - Allowed: All authenticated users
  - Required roles: [User, Moderator, Admin]

FR1.2: Moderate user posts
  - Allowed: Only moderators and admins
  - Required roles: [Moderator, Admin]

FR1.3: Delete user account
  - Allowed: Only admins
  - Required roles: [Admin]
```

## Data Encryption

### Encryption in Transit

**Protocol**: [HTTPS / TLS 1.2+ / mTLS]

**Certificate**:
- [Provider: LetsEncrypt / Self-signed / CA]
- [Expiration: Auto-renewal before expiry]
- [HSTS**: [Max-age: 31536000]]

**Example Header**:

```
Strict-Transport-Security: max-age=31536000; includeSubDomains
```

### Encryption at Rest

**What to Encrypt**: [Sensitive user data, Passwords, API keys]

**Encryption Algorithm**: [AES-256 / AES-128]

**Key Management**:

```
Master Key Store: [AWS KMS / HashiCorp Vault / HSM]
Rotation Policy: Rotate every 90 days
Key Access: Only application service account
```

**Database Encryption**:

```sql
-- Example: Encrypt sensitive fields
CREATE TABLE users (
    user_id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    ssn_encrypted BYTEA,  -- Encrypted
    created_at TIMESTAMP
);
```

### Key Management

**Key Storage**: [KMS / Vault / Environment variables / Secrets manager]

**Key Rotation**: [Frequency: 90 days / On compromise / During access review]

**Key Backup**: [Secure backup location / Multi-signature required for access]

## CORS and CSP

### Cross-Origin Resource Sharing (CORS)

**Allowed Origins**: [https://example.com / * (not recommended)]

**Allowed Methods**: [GET, POST, PUT, DELETE]

**Allowed Headers**: [Content-Type, Authorization]

**Example Configuration**:

```
Access-Control-Allow-Origin: https://example.com
Access-Control-Allow-Methods: GET, POST, PUT, DELETE
Access-Control-Allow-Headers: Content-Type, Authorization
Access-Control-Max-Age: 86400
```

### Content Security Policy (CSP)

**Purpose**: Prevent XSS attacks

**Example Policy**:

```
Content-Security-Policy: 
  default-src 'self';
  script-src 'self' 'unsafe-inline';
  style-src 'self' https://fonts.googleapis.com;
  img-src 'self' data: https:;
```

## Security Headers

### Common Security Headers

| Header                 | Value                           | Purpose                   |
|------------------------|---------------------------------|---------------------------|
| X-Content-Type-Options | nosniff                         | Prevent MIME sniffing     |
| X-Frame-Options        | DENY                            | Prevent clickjacking      |
| X-XSS-Protection       | 1; mode=block                   | Enable browser XSS filter |
| Referrer-Policy        | strict-origin-when-cross-origin | Control referrer info     |

### Implementation

```python
# Example: Flask application
@app.after_request
def add_security_headers(response):
    response.headers['X-Content-Type-Options'] = 'nosniff'
    response.headers['X-Frame-Options'] = 'DENY'
    response.headers['X-XSS-Protection'] = '1; mode=block'
    return response
```

## Secrets Management

### Secrets Types

- [Database credentials]
- [API keys]
- [OAuth tokens]
- [Encryption keys]
- [SSH keys]

### Storage Locations

**Development**: [.env file / Environment variables]

**Production**: [Secrets manager / Vault / Cloud KMS]

**Never**: [Hardcode in source code / Commit to git]

### Rotation Policy

- [Rotate every 90 days]
- [Rotate on employee departure]
- [Rotate on suspected compromise]
- [Audit all access to secrets]

## Audit and Logging

### Security Events to Log

| Event             | Log Data                | Retention |
|-------------------|-------------------------|-----------|
| Login attempt     | User, IP, Timestamp     | 90 days   |
| Failed login      | User, IP, Timestamp     | 90 days   |
| Permission change | Who, What, When         | 1 year    |
| Data access       | User, Resource, Time    | 90 days   |
| API key creation  | User, Key hash, Purpose | 1 year    |

### Log Security

- [Logs not modifiable after creation]
- [Encrypted in transit and at rest]
- [Centralized logging (ELK / Splunk / CloudWatch)]
- [Access restricted to security team]
- [Alerting on suspicious patterns]

## Vulnerability Management

### Security Scanning

**Tools**:
- [SAST: SonarQube / Checkmarx]
- [DAST: OWASP ZAP / Burp Suite]
- [Dependency scanning: Snyk / Dependabot]
- [Container scanning: Trivy / Clair]

**Frequency**: [On every commit / Daily / Weekly]

### Incident Response

**Plan**:
1. [Detect]
2. [Contain]
3. [Eradicate]
4. [Recover]
5. [Post-incident analysis]

**Contacts**: [Security team, Management, Legal]

**Communication**: [Affected users within 24 hours]

## Compliance

### Compliance Standards

- [GDPR]: EU user data protection
- [CCPA]: California user data protection
- [HIPAA]: Healthcare data protection
- [PCI-DSS]: Payment card data protection
- [SOC 2]: Security and availability audit

### Data Retention and Deletion

**Retention Policy**: [Keep for 1 year / as long as account active]

**Deletion Policy**: [On user request / On account deletion]

**Right to be Forgotten**: [Support deletion upon request]

**Data Portability**: [Export data in standard format]
