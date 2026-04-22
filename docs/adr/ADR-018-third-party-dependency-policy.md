# ADR-018: Third-Party Dependency Policy

**Status:** Accepted
**Full rationale:** [rationale/ADR-018-third-party-dependency-policy.md](rationale/ADR-018-third-party-dependency-policy.md)

## Decision

Framework, api, and core modules limited to three third-party dependencies:

1. **Jackson** (`com.fasterxml.jackson`) — JSON serialization, native record support
2. **Apache Commons** (`commons-lang3`, `commons-collections4`) — `StringUtils`,
   `CollectionUtils`, etc.; extensions to the standard library without abstraction or
   framework coupling
3. **Micrometer** (`io.micrometer:micrometer-core`) — metrics facade (SLF4J of metrics);
   minimal transitive footprint; `Metric` builder delegates internally; domain never
   imports Micrometer types

**Sole exception:** JSpecify (`org.jspecify`) — `@Nullable` annotations, compile-time-only,
no runtime footprint.

Infrastructure libraries (Spring, Hibernate, messaging, HTTP clients) belong exclusively
in adapter modules (`repository-rdms`, `bootstrap`, etc.).

## Consequences

- New domain-module dependency requires an ADR amendment, not a quiet pom.xml edit
- Adapter modules unconstrained — they absorb infrastructure coupling
- Domain layer remains portable across application frameworks
