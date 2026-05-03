# ADR-006: Command Query Separation

> [rationale](rationale/ADR-006-command-query-separation.md)

Every method is a **Command** or a **Query**. Never mixed.

**Query**

- Return type never `void` — `Optional<T>`, `List<T>`, or value.
- Synchronous, idempotent.
- Empty result is information, not an error. Caller decides whether to throw, default, or branch.

**Command**

- Return type always `void`.
- Assumed potentially asynchronous — caller must not depend on completion before the next line.
- Never returns a result, including via exception for domain flow.
- Post-state requires a separate query.

**Argument validation**

- Null / structurally invalid args throw `InvariantViolationException` via
  `observer().arguments(...)`. Programming error, not domain flow. All violations collected
  in a single pass before throwing.

**Failure propagation — fail-fast, layer by layer.** Each layer throws what it knows.
A command that knows its repository call will fail throws; a repository that knows
the adapter (in-memory mock, RDBMS) will fail throws. Errors propagate up rather
than being swallowed. Concretely:

- `update` for a non-existent name throws `EntityNotFoundException` from the
  repository; the command lets it propagate.
- `insert` of a duplicate name throws `PrimaryKeyConstraintException` from the
  repository; the command lets it propagate.
- Argument validation throws `InvariantViolationException` at the layer that
  receives the bad argument (command at the api boundary, repository at the
  data port — same constraint graph, defense-in-depth).

Failure routing depends on how the command was invoked:

- **Synchronous (direct method reference)** — the exception surfaces to the
  caller with the most context available, all the way to the end user
  (controller → response). The command's `void` return type is preserved; the
  exception is the failure channel, never a domain-flow channel.
- **Asynchronous (event / message bus)** — the failure is captured in a
  dead-letter queue at the first point of failure, where a human can review
  and reconcile. The void-return contract is preserved; the bus owns delivery.

**Insert-and-retrieve.** `insert` assigns id and stores; id is not returned. Caller queries
by `EntityName` slug to retrieve persisted entity + id.
