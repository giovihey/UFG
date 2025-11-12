# Requirements Elicitation and Analysis

This section defines what the software should do, without focusing on implementation details.

## Principles

- Requirements explain **what** (not how) the software should do
- Requirements focus on application functionality, not on particular technical problems
- Requirements must be clearly identified and numbered
- Each requirement must have an **acceptance criterion** for validation

## Glossary

Define domain-specific terms used throughout the document:

| Term     | Definition   |
|----------|--------------|
| [Term 1] | [Definition] |
| [Term 2] | [Definition] |

## Functional Requirements

Functional requirements specify the functionality the software should provide to users.

### FR1: [Requirement Title]

**Description**: [Detailed description of the requirement]

**Acceptance Criteria**:
- [ ] Criterion 1
- [ ] Criterion 2
- [ ] Criterion 3

---

### FR2: [Requirement Title]

[Continue with additional functional requirements...]

## Non-Functional Requirements

Non-functional requirements do not directly concern behavioral aspects but rather system properties like consistency, availability, performance, security, etc.

### NFR1: [Requirement Title]

**Description**: [Detailed description]

**Acceptance Criteria**:
- [ ] Criterion 1
- [ ] Criterion 2

---

### NFR2: [Requirement Title]

[Continue with additional non-functional requirements...]

## Implementation Constraints

Implementation constraints restrict the system realization phase, such as:
- Required programming languages
- Specific software tools
- Development frameworks
- Technology stack

These should be adequately justified by political, economic, or administrative reasons. Otherwise, implementation choices should emerge as consequences of design decisions.

### Constraint 1: [Title]

**Justification**: [Why is this constraint necessary?]

**Details**: [Specific details about the constraint]

---

### Constraint 2: [Title]

[Continue with additional constraints...]

## Relevant Distributed System Features

This subsection motivates which distributed system features are relevant for your project and which are not.

### Transparency

**Relevant?** [ ] Yes [ ] No

Does your system need to hide distribution details from users or developers? Is it important that failures, location, or replication are invisible?

[Your analysis here]

---

### Fault Tolerance & Dependability

**Relevant?** [ ] Yes [ ] No

What happens if a component fails? Is uninterrupted service required? Is data loss or corruption unacceptable? How quickly must the system recover from faults?

[Your analysis here]

---

### Scalability

**Relevant?** [ ] Yes [ ] No

Will the system need to handle increasing numbers of users, requests, or data? Is it expected to grow over time?

[Your analysis here]

---

### Security & Trust

**Relevant?** [ ] Yes [ ] No

Is sensitive data being processed or stored? Are there multiple user roles with different permissions? Is authentication or authorization required?

[Your analysis here]

---

### Resource Sharing

**Relevant?** [ ] Yes [ ] No

Do multiple users or components need access to shared resources? Is coordination or synchronization needed?

[Your analysis here]

---

### Openness & Interoperability

**Relevant?** [ ] Yes [ ] No

Will your system interact with external systems or use components built with different technologies? Is standardization or compatibility important?

[Your analysis here]

---

### Evolvability & Maintainability

**Relevant?** [ ] Yes [ ] No

Will the system need to be updated or extended after deployment? Is long-term maintenance a concern?

[Your analysis here]

---

### Performance & Concurrency

**Relevant?** [ ] Yes [ ] No

Are there strict requirements on response time or throughput? Will many operations happen in parallel? Is network usage a concern?

[Your analysis here]

---

### Economy & Costs

**Relevant?** [ ] Yes [ ] No

Are there budget constraints for development, deployment, or operation? Is minimizing resource usage important?

[Your analysis here]
