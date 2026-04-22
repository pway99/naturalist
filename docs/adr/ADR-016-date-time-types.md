# ADR-016: Date and Time Types

**Status:** Accepted
**Full rationale:** [rationale/ADR-016-date-time-types.md](rationale/ADR-016-date-time-types.md)

## Decision

`java.time` (JSR-310) exclusively. `java.util.Date` and `java.util.Calendar` not permitted.

- **`Instant`** — events and observations (UTC; reproducible across systems)
- **`LocalDate`** — calendar dates without time component (due date, birth date)
- **`LocalDateTime`** — date+time where timezone is irrelevant or contextual

## Consequences

- Temporal intent explicit in type
- Interop with DBs/APIs via `java.time` converters
- Legacy `java.util.Date` migrated when encountered
