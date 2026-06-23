# Project title

- [Author 1](mailto:author1@studio.unibo.it)
- [Author 2](mailto:author2@studio.unibo.it)
- [Author 3](mailto:author3@studio.unibo.it)

### AI Disclaimer (if needed)

```
"During the preparation of this work, the author(s) used [NAME TOOL /
SERVICE] to [REASON].
After using this tool/service, the author(s) reviewed and edited the
content as needed and take(s) full responsibility for the content of the
final report/artifact."
```

## Abstract

Brief description of the project, its goals, and its achievements.

## Concept

Here you should explain:

- The type of product developed with that project, for example (non-exhaustive):
  - Application (with GUI, be it mobile, web, or desktop)
  - Command-line application (CLI could be used by humans or scripts)
  - Library
  - Web-service(s)

- Use case collection
  - _where_ are the users?
  - _when_ and _how frequently_ do they interact with the system?
  - _how_ do they _interact_ with the system? which _devices_ are they using?
  - does the system need to _store_ user's **data**? _which_? _where_?
  - most likely, there will be _multiple_ **roles**

## Requirements

- The requirements must explain **what** (not how) the software being produced should do.
  - you should not focus on the particular problems, but exclusively on what you want the application to do.

- Requirements must be clearly identified, and possibly numbered

- Requirements are divided into:
  - **Functional**: some functionality the software should provide to the user
  - **Non-functional**: requirements that do not directly concern behavioural aspects, such as consistency, availability, etc.
  - **Implementation**: constrain the entire phase of system realization, for instance by requiring the use of a specific programming language and/or a specific software tool
    - these constraints should be adequately justified by political / economic / administrative reasons...
    - ... otherwise, implementation choices should emerge _as a consequence of_ design

- If there are domain-specific terms, these should be explained in a glossary

- Each requirement must have its own **acceptance criteria**
  - these will be important for the validation phase

## Design

This chapter explains the strategies used to meet the requirements identified in the analysis.
Ideally, the design should be the same, regardless of the technological choices made during the implementation phase.

> You can re-order the sections as you prefer, but all the sections must be present in the end

### Architecture

- Which architectural style?
  - why?

### Infrastructure

- are there _infrastructural components_ that need to be introduced? _how many_?
  - e.g. _clients_, _servers_, _load balancers_, _caches_, _databases_, _message brokers_, _queues_, _workers_, _proxies_, _firewalls_, _CDNs_, _etc._

- how do components _distribute_ over the network? _where_?
  - e.g. do servers / brokers / databases / etc. sit on the same machine? on the same network? on the same datacenter? on the same continent?

- how do components _find_ each other?
  - how to _name_ components?
  - e.g. DNS, _service discovery_, _load balancing_, _etc._

> Component diagrams are welcome here

### Modelling

- which **domain entities** are there?
  - e.g. _users_, _products_, _orders_, _etc._

- how do _domain entities_ **map to** _infrastructural components_?
  - e.g. state of a video game on central server, while inputs/representations on clients
  - e.g. where to store messages in an IM app? for how long?

- which **domain events** are there?
  - e.g. _user registered_, _product added to cart_, _order placed_, _etc._

- which sorts of **messages** are exchanged?
  - e.g. _commands_, _events_, _queries_, _etc._

- what information does the **state** of the system comprehend
  - e.g. _users' data_, _products' data_, _orders' data_, _etc._

> Class diagram are welcome here

### Interaction

- how do components _communicate_? _when_? _what_?
- _which_ **interaction patterns** do they enact?

> Sequence diagrams are welcome here

### Behaviour

- how does _each_ component **behave** individually (e.g. in _response_ to _events_ or messages)?
  - some components may be _stateful_, others _stateless_

- which components are in charge of updating the **state** of the system? _when_? _how_?

> State diagrams are welcome here

### Data and Consistency Issues

- Is there any data that needs to be stored?
  - _what_ data? _where_? _why_?

- how should _persistent data_ be **stored**?
  - e.g. relations, documents, key-value, graph, etc.
  - why?

- Which components perform queries on the database?
  - _when_? _which_ queries? _why_?
  - concurrent read? concurrent write? why?

- Is there any data that needs to be shared between components?
  - _why_? _what_ data?

### Fault-Tolerance

- Is there any form of data **replication** / federation / sharing?
  - _why_? _how_ does it work?

- Is there any **heart-beating**, **timeout**, **retry mechanism**?
  - _why_? _among_ which components? _how_ does it work?

- Is there any form of **error handling**?
  - _what_ happens when a component fails? _why_? _how_?

### Availability

- Is there any **caching** mechanism?
  - _where_? _why_?

- Is there any form of **load balancing**?
  - _where_? _why_?

- In case of **network partitioning**, how does the system behave?
  - _why_? _how_?

### Security

- Is there any form of **authentication**?
  - _where_? _why_?

- Is there any form of **authorization**?
  - which sort of _access control_?
  - which sorts of users / _roles_? which _access rights_?

- Are **cryptographic schemas** being used?
  - e.g. token verification,
  - e.g. data encryption, etc.

---

<!-- Riparti da qui  -->

## Implementation

- which **network protocols** to use?
  - e.g. UDP, TCP, HTTP, WebSockets, gRPC, XMPP, AMQP, MQTT, etc.
- how should _in-transit data_ be **represented**?
  - e.g. JSON, XML, YAML, Protocol Buffers, etc.
- how should _databases_ be **queried**?
  - e.g. SQL, NoSQL, etc.
- how should components be _authenticated_?
  - e.g. OAuth, JWT, etc.
- how should components be _authorized_?
  - e.g. RBAC, ABAC, etc.

### Technological details

- any particular _framework_ / _technology_ being exploited goes here

## Validation

### Automatic Testing

- how were individual components **_unit_-test**ed?
- how was communication, interaction, and/or integration among components tested?
- how to **_end-to-end_-test** the system?
  - e.g. production vs. test environment

- for each test specify:
  - rationale of individual tests
  - how were the test automated
  - how to run them
  - which requirement they are testing, if any

> recall that _deployment_ **automation** is commonly used to _test_ the system in _production-like_ environment

> recall to test corner cases (crashes, errors, etc.)

### Acceptance test

- did you perform any _manual_ testing?
  - what did you test?
  - why wasn't it automatic?

## Release

- how where components organized into _inter-dependant modules_ or just a single monolith?
  - provide a _dependency graph_ if possible

- were modules distributed as a _single archive_ or _multiple ones_?
  - why?

- how were archive versioned?

- were archive _released_ onto some archive repository (e.g. Maven, PyPI, npm, etc.)?
  - how to _install_ them?

## Deployment

- should one install your software from scratch, how to do it?
  - provide instructions
  - provide expected outcomes

## User Guide

- how to use your software?
  - provide instructions
  - provide expected outcomes
  - provide screenshots if possible

## Self-evaluation

- An individual section is required for each member of the group
- Each member must self-evaluate their work, listing the strengths and weaknesses of the product
- Each member must describe their role within the group as objectively as possible.
  It should be noted that each student is only responsible for their own section
