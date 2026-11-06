# The amateur Naturalist

Modular-monolith domain application in `com.naturalist`. ADRs: [`docs/adr/`](docs/adr/README.md).

Area-specific conventions (load when working in that area):

- [`kernels/CLAUDE.md`](kernels/CLAUDE.md) — framework, observability, BehavioralCollection
- [`domains/CLAUDE.md`](domains/CLAUDE.md) — identity model, record conventions, field
  annotations, TestEntitySource, repository architecture, **api surface namespace
  patterns**, query rules, new-module scaffolding, PR size
- [`domains/<domain>/CLAUDE.md`](domains/) — per-domain vocabulary and invariants
- [`docs/measurement-standards.md`](docs/measurement-standards.md) — units

Cross-effort status (load when the user asks "where are we", "what's next",
or starts work that touches multiple plans):

- [`docs/work-tracker.md`](docs/work-tracker.md) — single index of every active
  and recently-completed effort, with status, source-doc links, dependency graph,
  and the decided ordering. Each row points to its source-of-truth plan; the
  tracker itself owns no scope.

Cold storage — **do not load, search, or grep by default.** Read only when
explicitly investigating historical context (a resolved fork, a completed
slice's mechanics, an ADR's deep argument). Surfacing these in routine
work pollutes context with content that no longer drives decisions:

- `docs/plans/archive/` — completed and superseded plans.
- `docs/notes/parking-lot-resolved.md` — resolved forks. Live forks are in
  the sibling `parking-lot.md`.
- `docs/adr/rationale/` — deep-argument companions to the canonical ADRs.
  Prefer `docs/adr/ADR-N-*.md` (short form) first; consult the rationale
  file only when the short form does not carry enough context.

When excluding directories from a broad search, pass these paths to
`grep --exclude-dir` / `find -prune` explicitly.

## Identity

Every domain class implements exactly one of `NamedEntity`, `Entity`, `Aggregate`,
`ReadModel`, `ValueObject`, `BehavioralCollection` (all from `kernels/framework`; all extend
`Observable` and declare `invariants()`). Signatures and rules in
[`domains/CLAUDE.md`](domains/CLAUDE.md).

Two identity branches share a common `Named<KEY>` data-layer port (ADR-022):

- **`NamedEntity<NAME extends EntityName>`** — natural-key slug identity.
  `EntityName` is never null, kebab-case, and the stable cross-domain reference
  (ADR-001). `EntityName` subclasses live in `domains/identifiers/`.
- **`Entity<ID extends EntityId>`** — surrogate UUIDv7 identity. `EntityId` is
  generated at record construction, validated `version() == 7` at the boundary,
  and never crosses a domain boundary by value. `EntityId` subclasses live in
  `domains/identifiers/`. Use a kernel-provided generator; `UUID.randomUUID()`
  is forbidden.

No domain record carries a `PersistenceId` component — it does not exist in
Java (ADR-022, superseding ADR-021).

## Module layout

```
kernels/
  framework/          — NamedEntity, Entity, EntityName, EntityId, Aggregate,
                        ReadModel, ValueObject, Observable, Observer,
                        BehavioralCollection, Resilience facade
  framework-test/     — TestEntitySource, TestEntitySourceTest, EntityRepositoryTest,
                        NaturalistDatabase, NaturalistTestExtension, TestDataHelper
  field-notes/        — Description (four-level Durrell description)
  taxonomy/           — TaxonomicClassification (organism domains only)
  clades/             — Clade sealed type (evolutionary tree of life, trait-bearing nodes)
  catalog/            — cross-domain reference resolution (DomainId, Catalog)
  catalog-inmem/      — in-memory reference adapter for catalog
  identification/     — IdentificationRefused (port-level refusal; pipeline joins later)
  image-store-filesystem/ — ImageStore over a directory root with classpath fallback for seed photos
domains/
  identifiers/        — typed names (EntityName subclasses) and typed ids
                        (EntityId subclasses) only
  <domain>/<domain>-api, <domain>-core, <domain>-repository-test
adapters/
  resilience-resilience4j/ — bridges kernel Resilience facade to Resilience4j
apps/
  management-console/ — composition-root deployment artifact (Spring Boot)
```

## DAG (no cycles)

```
bootstrap                   →  application
<domain>-repository-test    →  <domain>-api
<domain>-repository-rdms    →  <domain>-api
<domain>-core               →  <domain>-api
<domain>-api                →  framework, identifiers, field-notes, and any kernel on
                               the rule-1 allow-list below
<organism>-api              →  the same, plus taxonomy and clades
identifiers                 →  framework
field-notes                 →  framework
taxonomy                    →  framework
habitat                     →  framework
measurements                →  framework
authority                   →  framework
clades                      →  framework, field-notes
biogeography                →  framework, field-notes
catalog                     →  framework, identifiers
observation                 →  framework, identifiers, taxonomy
identification              →  framework
vision                      →  framework
image-store-filesystem      →  framework, observation
framework                   →  Jackson, Commons, Micrometer, JSpecify only
framework-test              →  framework
```

Rules:

1. api modules depend only on `framework`, `identifiers` and `field-notes`, plus — as
   the domain needs them — `taxonomy`, `clades`, `observation`, `habitat`,
   `biogeography`, `measurements`, `catalog`, `authority`, `vision` and
   `identification`. Nothing else: never
   another domain's api, core, repository-test or repository-rdms. Who uses what today:
   `taxonomy` in arachnids, insects, library, microbes, plants and worms; `clades` in
   insects, library and plants; `observation` in insects and plants; `habitat` in
   insects; `biogeography` in plants; `measurements` in sensors, soil, weather and
   zone; `catalog` in chemistry, insects, library and plants; `authority` in insects,
   library and `eol-api`; `vision` and `identification` in insects.
2. core may import another domain's **api** only — never its core, repository-test, or
   repository-rdms.
3. repository modules depend only on their own api (+ framework).
4. bootstrap depends on everything; nothing depends on bootstrap.
5. Repository interfaces are package-private in api; cross-domain interaction goes
   through public query/service classes.
6. Repository behavior contracts live in repository-test; both in-memory and rdms
   adapters implement them.

Package-private visibility is the primary enforcement; ArchUnit tests may verify at build time.

## Sub-contexts

Within a module, `package-private` = internal to sub-context, `public` = crosses
boundary. Per-domain `CLAUDE.md` files show each domain's sub-context layout.

## Non-negotiables

- **Typed identifiers.** Never raw `String`, `Long`, or `UUID` as an entity reference
  across any boundary. `NamedEntity` records carry a concrete `EntityName` subclass;
  `Entity` records carry a concrete `EntityId` subclass. Cross-`NamedEntity` references
  are by `EntityName`; `Entity` records are never referenced cross-domain by value
  (ADR-022).
- **UUIDv7 only.** `EntityId.isValid()` enforces version 7. Do not call
  `UUID.randomUUID()` in domain or adapter code — use the kernel's generator.
- **Records for NamedEntity/Entity/Aggregate/ReadModel/ValueObject.** `BehavioralCollection` is
  a `final class` (see [ADR-011](docs/adr/ADR-011-behavioral-collections.md)). Record
  rules in [`domains/CLAUDE.md`](domains/CLAUDE.md).
- **Observations and Events are immutable** — records or final fields, no setters,
  equality by value. This is a domain invariant for `Entity` records and is documented
  in each event/observation domain's own `CLAUDE.md`; it is no longer encoded in a
  separate kernel subtype.
- **Every third-party version lives in the root `pom.xml` `<properties>` — and nowhere else.**
  Sub-aggregators (`kernels/`, `adapters/`, `tooling/`) declare no `<properties>` block of
  their own, and leaf poms reference `${...}` only. A version literal outside the root pom is
  a defect: it drifts, and it makes "what are we on?" unanswerable without a repo-wide grep.
  Check with:

  ```bash
  grep -rn "<version>" --include=pom.xml . | grep -v 1.0.0-SNAPSHOT | grep -v '\${'
  ```

  This must return nothing outside the root pom. Two corollaries. First, when a library is
  managed by `spring-boot-dependencies:${spring-boot.version}`, pin the property to *that*
  version rather than the newest release on Maven Central — only some modules import the Boot
  BOM, and matching it keeps both halves of the reactor on one version instead of letting
  nearest-wins resolution pick per-module. Mark those properties `<!-- Boot-managed -->`.
  Second, prefer importing a library's own BOM in root `dependencyManagement` when its
  artifacts version independently (Jackson does: since 2.20 `jackson-annotations` releases on
  its own track, so no single flat property can name the whole set).
- **Never name a credential.** No code, test, config or doc names an API key or other
  credential by vendor or role, and none quotes a vendor key prefix. Secrets are resolved
  through the kernel `com.naturalist.secrets.SecretProvider` under a `SecretName` that does
  not say what it unlocks (the model credential is `model-credential`, read from
  `MODEL_CREDENTIAL` or `<dir>/model-credential`); test values are `not-a-real-secret`. Only
  `com.naturalist.secrets` reads the process environment (ADR-028). The
  `NoNamedCredentials` and `NoSystemGetenv` recipes enforce this for Java; for everything
  else this must return nothing:

  ```bash
  git ls-files -co --exclude-standard | xargs grep -nIiE '[a-z0-9]+[-_]api[-_]?key|sk-ant[-]'
  ```

- **Never weaken a test, gate, or enforcement to make code pass — fix the code.** The
  build-time invariants exist to be *satisfied*, not silenced: the N+1 no-fan-out gate
  (`kernels/framework-test` → `com.naturalist.test.query.nofanout`, surfaced as
  `RepeatedSelectException`), ArchUnit rules, and the `com.naturalist.Enforce*` OpenRewrite
  recipes. When one fails it is reporting a real defect in *your* code — fix that. Do NOT
  disable or relax the check, remove a test's `@RegisterExtension`, stop a
  `TestEntitySource` read from recording, delete/soften the failing assertion, or blanket-
  suppress to clear a red build. Suppression (e.g. `@SuppressRepeatedSelect`) is a last resort
  for a *single, genuinely un-satisfiable* case — applied per-site, with a written
  justification, never as a way to make a failing suite green.

## Completeness verification

Before declaring any development task complete, run the architectural-enforcement
gate in addition to `mvn verify`:

```bash
mvn install -DskipTests && mvn rewrite:dryRun -Drewrite.failOnDryRunResults=true
```

This runs the `com.naturalist.EnforceArchitecture` OpenRewrite recipes
(`tooling/naturalist-rewrite`) across the whole reactor's source and fails on any
pending fix or marker — the invariants ArchUnit cannot reach repo-wide (e.g. acquire
`TestEntitySource` via `NaturalistDatabase#getNamed`, obtain the test database from a
`@RegisterExtension NaturalistTestExtension` field, never cache a `TestEntitySource`
in a field). It also includes `com.naturalist.EnforceQueryHygiene` (`NoSelectInIteration`,
armed 2026-08-23), which fails on a repository/query select invoked inside a loop or a
per-element stream op — the static N+1 backstop to the runtime select-count gate. It also runs `NoSystemGetenv` and `NoNamedCredentials` (ADR-028): only
`com.naturalist.secrets` may read the process environment, and no Java source may name a
credential. Run the credential grep from the non-negotiables alongside it.
Developers auto-apply the fixable ones with `mvn rewrite:run`. Design:
[`docs/plans/2026-08-23-naturalist-rewrite-enforcement-design.md`](docs/plans/2026-08-23-naturalist-rewrite-enforcement-design.md).
