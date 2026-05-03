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

**Update-not-found is silent.** `update` for non-existent id is a no-op. No
`EntityNotFoundException` in the framework. Contract test `doUpdate_unknownId` asserts
no throw and no phantom state via subsequent query.

**Insert-and-retrieve.** `insert` assigns id and stores; id is not returned. Caller queries
by `EntityName` slug to retrieve persisted entity + id.
