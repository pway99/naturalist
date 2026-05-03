# ADR: Date and Time Types

**Status:** Accepted

## Context

The codebase requires a consistent approach to representing dates, times, and timestamps. Java offers multiple date/time
APIs, and without a standard, usage becomes inconsistent and error-prone.

## Decision

Use `java.time` (JSR-310) exclusively for all date and time handling. Legacy types (`java.util.Date`,
`java.util.Calendar`) are not permitted.

Type selection follows these rules:

- **`Instant`** — for events and observations. Always carries a timezone (UTC), making it suitable for any moment that
  needs to be reproduced accurately across systems.
- **`LocalDate`** — for calendar dates without a time component (e.g. a due date, a birth date).
- **`LocalDateTime`** — for date and time values where timezone is not relevant or is implied by context.

## Consequences

- Temporal intent is explicit in the type: an `Instant` signals a recorded moment in time; a `LocalDate` signals a
  calendar date.
- Interoperability with external systems (databases, APIs) is straightforward via `java.time` converters.
- Legacy `java.util.Date` usage must be migrated when encountered.
