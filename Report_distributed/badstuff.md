# Report Review To-Do

## Must Fix Before Submission

- [x] Replace the placeholder introduction in `sections/introduction.qd`; it currently only says "this is the last step" and gives no motivation, project scope, contribution summary, or report roadmap.
- [x] Decide whether `sections/abstract.qd` belongs in the submitted report. The file exists but is not included from `main.qd`, and its content is still only a placeholder comment.
- [x] Decide whether `sections/self-evaluation.qd` is required for the course submission. The file exists, contains placeholder sections for both members, and is not included from `main.qd`.
- [x] Add a real conclusion or closing section. The report currently ends at Future Work, so the narrative stops at limitations instead of summarizing what was achieved. => **The conclusion will be Self-Evaluation**
- [x] Fix the Future Work structure: its intro says there are four themes, including "netcode and tooling loose ends", but the section only contains three headings and never covers those loose ends.
- [x] Add the validation gaps mentioned in `sections/validation.qd` to Future Work. Validation says Go service unit tests and a `GameRules` round spec are future work, but `sections/future-works.qd` does not list them.

## Consistency Issues

- [x] Align the screen-flow diagrams. `sections/concept.qd` goes `Practice -> Game`, while `sections/user-guide.qd` includes a `Versus`/VS splash between practice and game. The code has `Screen.VsSplash`, so Concept should probably include it too.
- [x] Fix the auth-flow wording in the User Guide. It says "A new username creates an account; an existing one logs in", but the UI has separate login/register modes. Make it clear that the player must choose Register for a new account and Login for an existing one.
- [x] Rework the attack-frame examples in Requirements. FR4-FR6 describe one fixed JAB with 4/3/8 frames, 5 damage, and 12 hitstun, but the JSON character files define different punch values per character. Either describe a generic acceptance criterion or explicitly say the example is for one specific character/default attack.
- [x] Make the controls and move-list story consistent. Concept and User Guide list both `P` and `K`, Future Work says punch/kick with neutral/crouching variants are wired, but the "During a Match" guide only teaches `P`/JAB. Add `K` and crouching variants there, or reduce the earlier claims.
- [x] Clarify whether character selection is implemented or future work. Future Work says multiple characters and per-player loading already exist, while also saying character selection is missing. The report should distinguish "hardcoded different characters" from "player-selectable characters".
- [x] Reconcile the start-handshake timing. Design says the offerer picks `now + 500 ms` and both peers sleep until that time, but the code also has a minimum VS splash duration. Either document the splash delay accurately or remove exact timing from the design text.
- [x] Reconcile signaling lifetime and disconnect handling. Design says the signaling WebSocket is only needed through the start handshake, but Fault-Tolerance says the survivor is notified via `peer_left`. If mid-match disconnects are handled through WebRTC instead, remove or qualify the `peer_left` claim.
- [x] Fix the deployment story for remote play. Deployment says two machines can point at the same backend host, but the client constants shown in code use hardcoded localhost URLs. The report should explain how users override those URLs, or state that remote backend configuration is not yet packaged.
- [x] Avoid overclaiming scalability. Requirements says the signaling service supports an "unbounded" number of concurrent 1v1 matches, but the design also says it is one process with in-memory rooms. Replace "unbounded" with a resource-bounded claim.
- [x] Avoid overclaiming performance. The report uses phrases like "sub-frame latency is required" and says P2P removes server hops, but Validation does not include measured latency results. Add measurements or soften the claim.

## Repetition And Flow

- [x] Reduce repeated screen-flow explanations. Concept, Requirements, and User Guide all restate login -> matchmaking -> fight. Keep Concept high-level, Requirements testable, and User Guide player-facing.
- [x] Reduce repeated controls tables. Concept and User Guide both list keys. Keep the detailed controls in User Guide and only summarize interaction devices in Concept.
- [x] Reduce repeated architecture summaries. Concept, Requirements, Design, and Implementation all explain "auth service + signaling service + P2P clients". Use Concept for the product overview, Design for rationale, and Implementation for concrete technologies.
- [x] Move very low-level constants out of Requirements unless they are genuinely required by the assignment. Examples: exact jab frames, bcrypt cost, rate limit values, JWT lifetime, rollback window, and packet byte layout.
- [x] Consider moving the 36-byte packet layout to an appendix or shortening it. It is useful, but it interrupts the higher-level implementation narrative unless the report explicitly argues why byte layout matters to the distributed-system goal.
- [x] Add transitions between major chapters. The current sections are individually coherent, but the report jumps from use cases to requirements to design with little explanation of how each chapter narrows the problem.

## Possible Verbosity Cuts

- [x] Shorten `sections/design.qd` first. It is the longest chapter by far (~2,150 words) and repeats the same core ideas: peer-to-peer gameplay, supporting client-server services, deterministic domain, and rollback consistency.
- [x] Merge or cut one of the Design architecture/component explanations. The architecture diagram, infrastructure table, and component diagram all restate "two clients + auth + signaling + Postgres + STUN"; keep the strongest diagram/table and reduce the surrounding prose.
- [x] Trim the Design modelling subsection. The domain class diagram, entity-to-infrastructure mapping table, and domain-events table are useful but dense; consider keeping only the class diagram plus one short paragraph, or move the mapping/events detail to Implementation.
- [x] Do not mention shorter names like NFR1, NGR2 or FR1, nobody cares about citing it. Inline citations were removed; IDs remain only inside the Requirements tables.
- [x] Compress the Design start-handshake paragraph after the sequence diagram. The sequence already shows `ready` and `start(at)`; the prose can probably be two or three sentences instead of a full explanation of every timing consequence.
- [x] Merge the Design consistency, fault-tolerance, and availability explanations where they repeat "no authoritative server", "no reconnection", "desync is logged", and "signaling is not on the gameplay path".
- [x] Shorten `sections/requirements.qd` by reducing long acceptance-criterion cells. FR3, FR7, FR8, NFR1, and NFR7 are especially wordy; keep them testable but remove implementation-like detail that is explained later.
- [x] Consider trimming the "Relevant Distributed System Features" table in Requirements. It overlaps heavily with Design's Consistency, Fault-Tolerance, Availability, and Security sections; the rationale column could be shortened to one-line relevance notes.
- [x] Consider moving the Requirements glossary to an appendix or shortening it to only terms that are essential for understanding requirements. Common terms such as JWT, SDP, ICE, STUN, and AABB may not need full definitions in the main report.
- [x] Reduce `sections/validation.qd` test inventory detail. The per-spec table, RollbackService property table, and quality-gate paragraphs are evidence-rich but long; keep the automated strategy and representative examples, and move exact per-spec coverage to an appendix if page count matters.
- [x] Shorten the manual acceptance table in Validation. The "Required evidence before submission" column is useful for the team, but it is verbose for a submitted report; it could become a short evidence checklist before the table.
- [x] Condense `sections/future-works.qd`. The feature tables already describe the work; several explanatory paragraphs around character hashing, rollback smoothing, the bot, and validation tooling could be shortened without losing the roadmap.
- [x] Trim Implementation's Persistence/Auth details. The SQL schema, bcrypt/JWT/rate-limit specifics, and query behaviour are precise but may read like source documentation; keep only the implementation choices that matter for distributed-systems evaluation.
- [x] Shorten Deployment's "How the Compose file is engineered" and Configuration Summary. These are useful operator notes but overlap with the command examples and may be too detailed for the main report.
- [x] Revisit Concept's Scope Boundaries table after trimming Requirements and Design. The same limitations also appear under distributed-system features and fault-tolerance; keep one canonical place and cross-reference it.

## Evidence And Specificity

- [ ] Add evidence for manual acceptance testing: environment, date, machines/NAT setup, pass/fail result, and maybe a short screenshot/log reference. Right now the validation table says checks were performed but does not show evidence.
- [x] Verify every exact test count before submission. The report says "roughly 85 Kotest cases" and lists per-spec counts; keep that updated or phrase it less precisely.
- [x] Verify exact dependency versions before submission. The implementation section lists Kotlin, Compose, Go, Java-WebSocket, library versions, and tool versions; these are easy to become stale.
- [x] Be careful with "solved" language in Future Work. "The core distributed problem is solved" is stronger than the validation evidence, especially with manual NAT testing, no reconnection, desync only logged, and no automated Go service tests.
- [x] Explain what is intentionally out of scope earlier. Items like no ranking, no replay, no reconnection, no spectators, no authoritative anti-cheat, and no load balancing appear across the report; collect them once so later limitations feel deliberate rather than discovered late.
- [x] Make the report's audience consistent. Some parts read like a user manual, some like source-code documentation, and some like a distributed-systems design report. Prefer the course-report voice in main chapters and keep player instructions in User Guide.

## Smaller Cleanup

- [x] Standardize naming: use "Future Work" consistently.
- [x] Standardize casing for "Self-Evaluation" versus other title-style headings.
- [x] Clean stale comments in report support files if they are visible to graders.
- [x] Check whether the title page needs date, course year, repository link, or group identifier.
