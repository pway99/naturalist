# ADR-004: Modular Monolith, No Microservices

**Status:** Accepted
**Full rationale:** [rationale/ADR-004-modular-monolith.md](rationale/ADR-004-modular-monolith.md)

## Decision

- **Modular monolith with hexagonal (ports and adapters) structure.** Microservices rejected.
- **Domain boundaries enforced at Java module level** via compile-time visibility.
  `package-private` = internal; `public` = explicit cross-boundary. DAG is strictly acyclic;
  ArchUnit verifies at build time.
- **Module DAG:**
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
  framework                   →  (external libs only)
  framework-test              →  framework
  ```
- **Cross-domain references by `EntityName` slug only** — never `PersistenceId<Long>`.
  Numeric surrogate stays within the owning domain.

## Consequences

- Domain isolation enforced by compiler, not convention
- Cross-domain coupling structurally impossible at wrong layers
- Full system deploys/tests as single unit — no distributed failure modes during development
- Future microservice extraction non-destructive: cross-domain references already slug-based
