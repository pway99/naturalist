# ADR-004: Modular Monolith

> [rationale](rationale/ADR-004-modular-monolith.md)

- Modular monolith with hexagonal (ports and adapters) structure. No microservices.
- Domain boundaries enforced at the Java module level via compile-time visibility.
  `package-private` = internal; `public` = crosses boundary. DAG strictly acyclic;
  ArchUnit may verify at build time.
- Module DAG, as decided. The shape still holds; the kernel allow-list has since
  grown — see the amendment below, and rule 1 in the root `CLAUDE.md` for the live
  list:

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

## Amendment 2026-10-06 — the kernel allow-list grew

Nothing above is reversed. The DAG is still strictly acyclic, api modules still
depend only on kernels, and a domain is still reached only through its api. What
changed is how many kernels exist: seven have been extracted since this ADR was
written and are now depended on by api modules — `clades`, `observation`,
`habitat`, `biogeography`, `measurements`, `catalog` and `authority`. The block
above lists four, so read it for the *rule*, not for the roster.

Verified 2026-10-06 against the whole reactor: 114 modules, 320 compile-scope
inter-module edges, **acyclic**. The two rules this ADR is really about both hold
with no exceptions — no `*-core` depends on another module's core or repository,
and nothing depends on an app module. Two deviations from the *repository* rule
are open and tracked, neither of them a cycle: `library-repository-test →
insects-api` (compile) and `library-repository-rdbms → insects-core` (test).

The live allow-list is rule 1 in the root `CLAUDE.md`; this ADR is not the place
to look it up.
