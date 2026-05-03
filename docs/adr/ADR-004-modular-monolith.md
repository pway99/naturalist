# ADR-004: Modular Monolith

> [rationale](rationale/ADR-004-modular-monolith.md)

- Modular monolith with hexagonal (ports and adapters) structure. No microservices.
- Domain boundaries enforced at the Java module level via compile-time visibility.
  `package-private` = internal; `public` = crosses boundary. DAG strictly acyclic;
  ArchUnit may verify at build time.
- Module DAG:

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
framework                   →  external libs only
framework-test              →  framework
```

- Cross-domain references by `EntityName` slug only — never `PersistenceId<Long>`.
