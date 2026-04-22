# ADR-004: Modular Monolith, No Microservices

**Status:** Accepted

## Context

The project models a living ecological system — chemistry, soil, zones, organisms, sensors,
climate events — with rich cross-domain relationships. An architectural strategy is needed
that enforces domain boundaries without introducing distributed systems complexity.

## Decision

The architecture is a **modular monolith with hexagonal (ports and adapters) structure**.
Microservices are explicitly rejected.

### Module Boundaries

Domain boundaries are enforced at the **Java module level** via compile-time visibility.
`package-private` is the primary enforcement mechanism within a module. `public` means
the type explicitly crosses a sub-context boundary.

The module DAG is strictly acyclic. Any cycle is a violation and must be corrected
immediately. ArchUnit tests verify the DAG at build time.

```
bootstrap                   →  application
<domain>-repository-test    →  <domain>-api
<domain>-repository-rdms    →  <domain>-api
<domain>-core               →  <domain>-api
<domain>-api                →  framework, identifiers, field-notes
<organism>-api              →  framework, identifiers, field-notes, taxonomy
identifiers                 →  framework
field-notes                 →  framework
taxonomy                    →  framework
framework                   →  (nothing — external libs only)
framework-test              →  framework
```

### Cross-Domain References

Domains reference each other by `EntityName` slug — never by `PersistenceId<Long>`. The numeric
surrogate stays within the domain that owns it. The slug is the stable, portable identifier.
This means a domain can be extracted to a separate service later without changing the
reference model.

### Why Not Microservices

Microservices impose costs that are not justified at this scale:

- Distributed transaction complexity across domain boundaries
- Network latency for what are currently in-process calls
- Versioned inter-service contracts that must be maintained
- Deployment orchestration overhead

The `EntityName` slug strategy preserves the option to extract any domain to a separate
service without requiring it. The option has value; exercising it prematurely does not.

## Consequences

- Domain isolation is enforced by the compiler, not by convention or documentation
- Cross-domain coupling is structurally impossible at the wrong layers
- The full system deploys and tests as a single unit — no distributed transaction or
  network failure modes during development
- Any future extraction to microservices is possible and non-destructive because
  cross-domain references are already slug-based
