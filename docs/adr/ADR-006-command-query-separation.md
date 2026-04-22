# ADR-006: Command Query Separation

**Status:** Accepted
**Full rationale:** [rationale/ADR-006-command-query-separation.md](rationale/ADR-006-command-query-separation.md)

## Decision

Every method is classified as a **Command** or a **Query**. Never mixed.

### Query
- Return type never `void` — always `Optional<T>`, `List<T>`, or value type
- Synchronous, idempotent
- `Optional.empty()` / empty list is **information**, not an error
- Caller decides whether to throw, default, count, or branch

### Command
- Return type always `void`
- **Assumed potentially asynchronous** — caller must not depend on completion before the
  next line. Today sync; tomorrow event bus on another VM
- Never returns a result, including via exception for domain flow
- If caller needs post-state, it issues a separate query

### Argument validation ≠ control flow
- Null/structurally-invalid args throw `InvalidVariantException` via
  `observer().arguments(...)`. This is a **programming error**, not domain flow.
- Collects all violations in a single pass before throwing.

### Update-not-found is a no-op
- `update` for a non-existent id is silent. Caller queries to verify if needed.
- `EntityNotFoundException` is **not** added to the framework.
- `doUpdate_unknownId` contract test asserts no throw and no phantom state via subsequent query.

### Insert-and-retrieve
- `insert` assigns persistence id and stores entity. Assigned id is not returned.
- Caller queries by `EntityName` slug to retrieve persisted entity + id.

## Consequences

- `doInsert` / `doUpdate` return `void` — correct, not a gap
- `EntityNotFoundException` absent from framework by design
- System structurally ready for event bus promotion with zero interface changes
- Post-command state verification requires explicit query at every call site
