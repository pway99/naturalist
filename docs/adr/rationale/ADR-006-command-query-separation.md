# ADR-006: Command Query Separation

**Status:** Accepted

## Context

Two concerns drove this decision.

First, the principle established in the previous session: exceptions must not be used to
manage control flow. A method that throws `EntityNotFoundException` when an update target
does not exist is using the exception mechanism as a return channel — the caller must
catch to know what happened. This couples the caller to the exception type and forces
exception-based branching instead of data-based branching.

Second, the system must be positioned to move long-running or computationally expensive
operations off the request thread and onto an event bus for asynchronous processing on
a separate VM. A method that returns a result cannot be made asynchronous without
changing its signature. A method that is already void can be promoted to an async
command with no change to the caller.

These two concerns converge on the same solution: Command Query Separation.

## Decision

Every method in the system is classified as either a **Command** or a **Query**. The
two must never be mixed in a single method.

### Query

A query reads state and returns a result. It never mutates state.

- Return type is never `void` — always `Optional<T>`, `List<T>`, or a value type
- Synchronous — ties up the calling thread until the result is ready
- Idempotent — calling the same query twice with the same arguments returns the same result
- The caller is in complete control of data flow: an `Optional.empty()` or empty list is
  information, not an error. The caller decides whether to throw, default, count, or branch

```java
// Query — returns result, never throws for empty
Optional<Element> getByName(ElementName name);
List<Element> getByEntityNameSet(Set<ElementName> names);
Optional<Element> getById(ElementId id);
```

### Command

A command mutates state and returns nothing.

- Return type is always `void`
- Assumed potentially asynchronous — the caller must not depend on the command having
  completed before the next line executes. Today commands execute synchronously; tomorrow
  they may be dispatched to an event bus and processed on another VM
- Never returns a result via exception for domain flow. Exceptions are the failure
  channel — argument errors, persistence errors, adapter failures — not a substitute
  for a return value
- If the caller needs to know the state after a successful command, it issues a separate
  query

```java
// Command — void, no feedback, no domain exception
void insert(Element element);
void update(Element element);
```

### Argument Validation is Not Control Flow

The no-exceptions-for-control-flow rule applies to domain flow — business outcomes like
"entity not found" or "update had no effect." It does not apply to programming errors.

A null argument to any method is a programming error. The argument validation layer
(`observer().arguments(...)`) throws `InvariantViolationException` for null or structurally
invalid inputs, collecting all violations in a single pass before throwing. This is
enforced at the method boundary before any state access occurs, and it indicates a bug
in the caller, not a domain condition. Collecting all violations rather than failing fast
on the first one ensures the caller receives a complete error description — important at
API and form boundaries where iterative error discovery degrades the user experience.

### Failure Propagation — Fail-Fast, Layer by Layer

Each layer throws what it knows. A command that knows its repository call will fail
throws; a repository that knows the adapter (in-memory mock, RDBMS) will fail
throws. Errors propagate up rather than being swallowed.

- `update` for a non-existent name throws `EntityNotFoundException` from the
  repository; the command lets it propagate.
- `insert` of a duplicate name throws `PrimaryKeyConstraintException` from the
  repository; the command lets it propagate.
- Argument validation throws `InvariantViolationException` at the layer that
  receives the bad argument (command at the api boundary, repository at the data
  port — same constraint graph, defense-in-depth).

Exceptions remain *failure channels*, not domain-flow channels — a successful
command still returns `void`, and a caller that needs post-state issues a query.
What exceptions communicate is "this command did not succeed at this layer," and
that information is not a return value smuggled out through the exception type.

#### Failure Routing: Synchronous vs Asynchronous

How a failure surfaces depends on how the command was invoked.

- **Synchronous (direct method reference)** — the exception surfaces to the caller
  with the most context available, all the way to the end user
  (controller → response). The void return is preserved on the success path; the
  failure path uses the standard exception channel.
- **Asynchronous (event / message bus)** — the failure is captured in a
  dead-letter queue at the first point of failure, where a human can review and
  reconcile. The void-return contract is preserved end-to-end; the bus owns
  delivery, retry, and DLQ routing. Producers do not see the failure directly.

Both routes are compatible with the same command signature — the caller does not
change when a command is promoted from sync to async. What changes is *who*
observes failures: the caller in the sync case, the operator (via DLQ) in the
async case.

### Consequence for Insert-and-Retrieve

The `insert` command assigns a persistence id and stores the entity. The assigned id is
not returned — the caller must query by natural key (`EntityName` slug) to retrieve the
persisted entity and its assigned id. For catalog entities this is natural: the slug was
known before the insert and remains the stable identity.

### Positioning for the Event Bus

Commands are the natural unit of async work:

```
Synchronous (today):    caller → insert(entity)  → repository
Asynchronous (future):  caller → publish(InsertEntityCommand) → event bus → consumer → repository
```

No caller code changes when a command is promoted to async. The caller already expects
no return value and no exception. The event bus consumer picks up the command and
executes it on another VM.

Queries remain synchronous — a query that cannot return a result immediately is a
streaming or pagination concern, addressed separately.

## Consequences

- `doInsert` and `doUpdate` return `void` on success — this is correct, not a gap
- `EntityNotFoundException` is the repository's signal that an `update` target does
  not exist; the command lets it propagate. Synchronous callers see the exception;
  asynchronous callers see a DLQ entry
- Repository contract tests assert that `update` against a non-existent name throws
  `EntityNotFoundException` (`update_unknownName_throwsEntityNotFoundException`).
  Successful updates are confirmed by a subsequent query
- Callers that need post-command state verification must issue a separate query — this
  is explicit in code and enforces the CQS boundary at every call site
- The system is structurally ready for event bus promotion of any command with no
  interface changes. The signature is identical; the failure routing changes from
  exception-to-caller to dead-letter-queue-to-operator
