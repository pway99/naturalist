# ADR-018: Third-Party Dependency Policy

**Status:** Accepted

## Context

The framework, api, and core modules form the domain layer of the application. Every
dependency added here becomes a transitive obligation on every module in the system.
Unbounded third-party adoption leads to version conflicts, classpath bloat, and
framework lock-in — exactly the conditions hexagonal architecture is designed to prevent.

A deliberate allowlist keeps the domain layer stable and portable. Infrastructure
concerns (Spring, Hibernate, messaging) belong in adapter modules, not in the domain.

## Decision

Framework, api, and core modules are limited to three third-party dependencies:

1. **Jackson** (`com.fasterxml.jackson`) — JSON serialization is fundamental to the
   model/data separation pattern. Java defines schema, JSON defines instances. Jackson's
   native record support (`Class.getRecordComponents()`) makes it a zero-annotation fit
   for the domain model.

2. **Apache Commons** (`org.apache.commons:commons-lang3`, `commons-collections4`) — a
   practical extension to the Java standard library. `StringUtils`, `CollectionUtils`,
   and similar utilities eliminate boilerplate without introducing abstraction or
   framework coupling.

3. **Micrometer** (`io.micrometer:micrometer-core`) — the SLF4J of metrics. Micrometer
   is a facade over monitoring backends, not a backend itself. Its transitive footprint
   is minimal (two compile-scope modules, two runtime-only jars — lighter than Jackson).
   The `Metric` fluent builder delegates to Micrometer internally; domain code never
   imports Micrometer types directly.

No other third-party libraries are permitted in framework, api, or core modules.
JSpecify (`org.jspecify`) is the sole exception — it provides `@Nullable` annotations
that are compile-time-only with no runtime footprint.

Infrastructure libraries — Spring, Hibernate, messaging, HTTP clients — belong
exclusively in adapter modules (repository-rdbms, bootstrap, etc.).

## Consequences

- New domain module dependencies require an ADR amendment, not a quiet pom.xml edit.
- Adapter modules are unconstrained — they exist to absorb infrastructure coupling.
- The domain layer remains portable across application frameworks.
