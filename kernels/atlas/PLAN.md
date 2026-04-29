# Atlas Kernel — Effort Plan

A naturalist's atlas is a cross-referenced body of knowledge that helps you navigate
between species, ranges, and ecological relationships. The `atlas` kernel plays the
same role inside this codebase: it is the cross-domain navigation surface the
management console renders against, and the routing layer through which one domain
discovers another's references to its entities.

This document is the durable plan for building it. It is structured so that any one
milestone can be picked up in a fresh session without reading prior chat history —
the architectural decisions are stated declaratively at the top, and each milestone
lists the exact files to read first before doing the work.

---

## Why this exists

Two concrete pains motivate the kernel.

The plants console renders `Description.university()` as a wall of dense academic
prose — Latin binomials, measurement units, parenthetical citations, inline numbered
enumerations — with no internal landmarks. The reader cannot scan it. The text
already carries latent structure (taxonomic header, quantitative claims, visitor
list, management note) that a renderer can surface.

That same prose is full of cross-domain entity references — `Aristolochia
californica`, `Bombus`, `aristolochic acid` — which are conceptually `EntityName`s
written in human form. The reader cannot click them. And on the inverse side, the
`Compound` detail page for `aristolochic-acid` has no way to enumerate the plants
or insects that reference it, because each domain knows its own outbound references
but no domain owns the inverse index.

Both problems share a shape: cross-domain entity-name resolution, contributed by
each domain, assembled per app, queried by the console. That is the atlas.

---

## Architectural decisions (declarative, do not re-derive)

**Module placement.** `kernels/atlas/`, sibling to `kernels/field-notes` and
`kernels/taxonomy`. Depends only on `framework` and `identifiers`. Nothing in the
DAG depends on atlas except domain `<domain>-core` modules (which contribute) and
console / app modules (which consume). The kernel itself contains contracts and a
default in-memory assembly — no domain knowledge.

**Two SPIs, one kernel.** Forward and inverse resolution are separate concepts that
share the contribution-and-assembly pattern. Both live in `kernels/atlas/`:

- `AtlasContribution` — forward direction. A domain declares the aliases (surface
  forms in prose) under which its own entities can be recognised. Used by the
  console to convert "Aristolochia californica" in description text into a link to
  the `california-pipevine` plant page.
- `EntityReferences<T extends EntityName>` — inverse direction. A domain declares
  it can answer "give me my entities that reference this foreign `EntityName`."
  Used by the console to render "Found in: california-pipevine (plants),
  pipevine-swallowtail (insects)" on the `aristolochic-acid` detail page.

Each domain implements zero or more of each. Contributions are collected in each
app's composition root — there is no shared module that depends on every domain.

**Registry shape: routing, not graph.** The atlas is a routing table, not a
knowledge graph. It does not ingest `(source, target, kind)` triples at startup.
For inverse queries, it knows only that "domain X has an `EntityReferences<Y>`
provider"; the actual lookup runs live against the domain's repository. This keeps
the registry's surface tiny, removes any startup index to maintain, and makes
domain data freshness automatic.

**Derive aliases, do not register.** A domain's `AtlasContribution.aliases()`
should be derived from the domain's own entity data — slug, scientific binomial,
genus, abbreviated binomial — not authored as a parallel registration list.
Non-derivable aliases (common names like "Pipevine Swallowtail" for *Battus
philenor*) live as a `commonNames` field on the entity itself, in the same data
file the entity is authored in. Adding an entity automatically contributes its
derivable aliases; adding a common name is one edit in one place.

**Soft validation via Observer, not exceptions.** Dangling cross-aggregate
references (a `PhytochemicalConstituent` referencing a `CompoundName` that does
not exist in the chemistry catalog) are observed, not thrown. The atlas emits an
`UnresolvedReferenceObservation` through the existing kernel `Observable` /
`Observer` pipeline. A `MicrometerObserver` translates these to a counter.
Diagnostic context (`source_name`, `target_name`) ships in tags, not in the metric
name, with a bounded LRU to defend against cardinality runaway.

**Per-app composition, not a shared registry module.** The assembled `Atlas`
instance lives in each app's composition root (currently `naturalist-web/console`,
later `apps/<each>`). Each app collects the contributions of the domains it
includes. A registries-parent module that depends on every domain is explicitly
rejected — that creates the fan-in point we want to avoid.

---

## Module layout

```
kernels/atlas/
  pom.xml
  PLAN.md                       (this file)
  src/main/java/com/naturalist/atlas/
    Atlas.java                  — public-facing query interface
    AtlasContribution.java      — forward-direction SPI (aliases)
    EntityReferences.java       — inverse-direction SPI (back-references)
    EntityRef.java              — typed reference to an entity in some domain
    DomainId.java               — typed domain identifier (low-cardinality tag)
    UnresolvedReferenceObservation.java — observable event
    DefaultAtlas.java           — in-memory assembly used by all apps
  src/test/java/com/naturalist/atlas/
    DefaultAtlasTest.java
    ...
```

Each domain that participates ships its providers in `<domain>-core` (where
repository queries live). The chemistry domain only consumes — it queries the
atlas; it implements no atlas SPIs of its own.

---

## DAG impact

```
atlas              →  framework, identifiers
<domain>-core      →  ..., atlas        (when the domain contributes providers)
console / app      →  ..., atlas        (consumes the assembled Atlas)
```

No new arrows from `<domain>-api`. No arrows from atlas to any domain. Any
provider implementation that needs domain repository access lives in `-core`.

---

## Milestones

Each milestone is sized to fit a focused session with a small read-context. The
"Read first" lists are exhaustive — a fresh session should not need to grep the
codebase before starting.

### M0 — Scaffold the module ✅

**Done in the session that produced this plan.** Created `kernels/atlas/`,
registered in `kernels/pom.xml` and root `pom.xml`'s `dependencyManagement`,
empty `src/main/java/com/naturalist/atlas` and `src/test/java/com/naturalist/atlas`.

### M1 — Shared types: `EntityRef`, `DomainId`, `UnresolvedReferenceObservation`

**Goal.** Establish the small set of value types that every later milestone
depends on. No SPIs yet, no Atlas interface yet — just the types that flow
through the SPIs.

**Read first.**
- `kernels/framework/src/main/java/com/naturalist/ddd/EntityName.java`
- `kernels/framework/src/main/java/com/naturalist/observability/Observable.java`
- `kernels/framework/src/main/java/com/naturalist/observability/Constraints.java`
- one existing kernel value object: `kernels/field-notes/src/main/java/com/naturalist/fieldnotes/Description.java`
- this PLAN.md (the "Architectural decisions" section)

**Build.**
- `EntityRef` — record carrying `(DomainId domain, EntityName name)`. Implements
  `ValueObject`. Invariants: both non-null. Provides `displayLabel()` returning
  the slug.
- `DomainId` — record wrapping a kebab-case string. Implements `ValueObject`.
  Constants for known domains (`PLANTS`, `CHEMISTRY`, `INSECTS`, ...) declared as
  static fields. Adding a new domain is a one-line addition; this list is the
  closed set the metric `source_domain` tag draws from.
- `UnresolvedReferenceObservation` — record carrying `(EntityRef source,
  Class<? extends EntityName> targetType, String targetSlug, String reason)`.
  Implements `Observable`. The producer of this observation is whichever atlas
  call site detects the missing reference; the consumer is configured per app.

**Acceptance.** Each type has a unit test with valid + invalid invariant cases
following the pattern in any existing `*Test.java` in `kernels/framework-test/`
or `kernels/field-notes/`. Module compiles. No domain code touched.

### M2 — Forward SPI: `AtlasContribution` and `Atlas.resolveAlias` ✅

**Goal.** Define the forward-direction contract and the kernel's default
in-memory assembly. This is what the console's description renderer eventually
calls when scanning prose.

**Read first.** M1 outputs (`EntityRef`, `DomainId`), `kernels/atlas/PLAN.md`
sections "Two SPIs, one kernel" and "Derive aliases".

**Build.**
- `AtlasContribution` — interface. `DomainId domain()`, `Stream<Alias> aliases()`.
  An `Alias` is a record `(String surfaceForm, EntityRef target)`.
- `Atlas` — interface. Declares `Optional<EntityRef> resolveAlias(String text)`.
  (The inverse method comes in M3.)
- `DefaultAtlas` — package-private constructor takes a list of contributions,
  precomputes a `Map<String, EntityRef>` for resolution. Surface-form matching
  is case-sensitive on the first character (genus capitalisation matters);
  precise matching rules documented in javadoc and pinned by tests.
- A small builder `AtlasAssembly.from(contributions...)` that constructs a
  `DefaultAtlas`. Used by app composition roots.

**Acceptance.** `DefaultAtlasTest` covers: exact match resolves; case-mismatched
match does not resolve; unknown surface form returns `Optional.empty()`;
overlapping contributions from two domains are detected at assembly with a
deterministic resolution rule (longest match wins; equal-length match is an
assembly error). No domain wiring yet — tests use synthetic contributions.

### M3 — Inverse SPI: `EntityReferences<T>` and `Atlas.findReferencesTo` ✅

**Goal.** Define the inverse-direction contract. This is what the chemistry
detail page eventually calls.

**Read first.** M1 + M2 outputs, this plan's "Registry shape: routing, not
graph" section.

**Build.**
- `EntityReferences<T extends EntityName>` — interface. `DomainId domain()`,
  `Class<T> referenceType()`, `Stream<EntityRef> referencesTo(T target)`.
- Extend `Atlas` with `Set<DomainId> domainsReferencing(Class<? extends EntityName>)`
  (the coarse routing answer) and `Map<DomainId, List<EntityRef>>
  findReferencesTo(EntityName target)` (the fan-out answer).
- `DefaultAtlas` indexes providers by `referenceType()` at assembly. Fan-out runs
  live against each provider; results are not cached at the atlas layer (each
  domain is responsible for its own caching if any).
- Extend `AtlasAssembly` to accept providers alongside contributions.

**Acceptance.** `DefaultAtlasTest` covers: routing returns the set of domains
whose providers handle a given target type; fan-out groups by `DomainId`;
provider that throws is observed (next milestone) but does not break peer
providers' results; empty providers yield empty result, not null.

### M4 — Plants `AtlasContribution` (derived aliases) ✅

**Goal.** First real producer. The plants domain contributes aliases for its own
plants — slug, scientific binomial, genus, abbreviated binomial.

**Read first.**
- `domains/plants/plants-api/src/main/java/com/naturalist/plants/Plant.java`
  (and its taxonomy field — not yet read in this plan; locate during the session)
- `domains/plants/plants-core` to identify the right place for the contribution
- `domains/plants/CLAUDE.md`

**Build.**
- New file in `plants-core` (sub-context: probably `plants/atlas/`). A class
  implementing `AtlasContribution`, taking the plants repository as a
  collaborator, deriving aliases on each call to `aliases()` from the live plant
  set. Aliases per plant: slug; `genus + " " + species`; `genus` alone;
  `genus.charAt(0) + ". " + species`. Skip the binomial form when species is
  null (some plants have genus-level identification).
- A common-name pass-through: if the plant model has a `commonNames` field,
  contribute each. (If not present yet, defer to M10 — note the deferral here.)

**Acceptance.** Test against `PlantsTestEntitySource`: Aristolochia californica
is recoverable through the contribution as `Aristolochia californica`,
`Aristolochia`, `A. californica`, and `california-pipevine`. The plants
contribution does not make the atlas depend on plants — assembled in plants-core
test scope, consumed via the atlas SPI only.

**Notes from execution.**

- The `Plant` record does not yet carry a `commonNames` component; the
  common-name pass-through is deferred per the plan's open question on
  schema location. When `Plant` grows the field, extending
  `PlantAtlasContribution.candidateForms` is a one-line addition.
- The catalog naturally produces collisions on the genus-only surface form
  (two `Trifolium` species, two `Passiflora` species). `DefaultAtlas`
  rejects equal-length surface-form conflicts at assembly, which is correct
  for routing safety but fatal for a derived contribution. The contribution
  defends the assembly itself: it collects every candidate form, then
  emits only those that resolve to a single target — ambiguous genus or
  abbreviated-binomial forms are silently dropped. This is the contribution's
  rule, not the kernel's.

### M5 — Plants `EntityReferences<CompoundName>`

**Goal.** First real inverse provider. Plants answers "who in plants references
this compound?" by querying `PhytochemicalConstituentRepository`.

**Read first.**
- `domains/plants/plants-api/src/main/java/com/naturalist/plants/phytochemistry/PhytochemicalConstituent.java`
- `domains/plants/plants-api/src/main/java/com/naturalist/plants/phytochemistry/PhytochemicalConstituentQuery.java`
- `domains/plants/plants-core/src/main/java/com/naturalist/plants/phytochemistry/PhytochemicalConstituentQueryImpl.java`

**Build.**
- New file in `plants-core` `plants/atlas/` package. Implements
  `EntityReferences<CompoundName>`. `referencesTo(CompoundName)` queries the
  constituent repository and emits one `EntityRef` per matching constituent's
  `plantName` (deduplicated; a plant with three constituents of the same compound
  is one reference, not three).
- Decide whether to also emit the constituent's own `EntityName` separately.
  Recommendation: yes, both — the plant for navigation, the constituent for
  detail. Use two `EntityRef` results per match, distinguishable by their
  underlying name type. Document the choice in the provider's javadoc.

**Acceptance.** With a populated `PhytochemicalConstituentTestEntitySource`,
`atlas.findReferencesTo(aristolochicAcidName)` returns plants entries grouped
under `DomainId.PLANTS`. Empty for an unknown compound. Provider's `domain()`
returns `DomainId.PLANTS`; `referenceType()` returns `CompoundName.class`.

### M6 — Description renderer (formatting only, no atlas)

**Goal.** Address the legibility half independently. The renderer takes a
`Description` level string and emits HTML with paragraph splits, italicised
binomials, lifted numbered lists, and a typographic chip for the leading
classification header. No linking yet.

**Read first.**
- `kernels/field-notes/src/main/java/com/naturalist/fieldnotes/Description.java`
- `domains/plants/plants-console/src/main/jte/plants/detail.jte`
- `domains/chemistry/chemistry-console/src/main/java/com/naturalist/chemistry/console/DepictionRenderer.java`
  (analogous pattern)

**Build.**
- `naturalist-web/console/src/main/java/com/naturalist/console/render/DescriptionRenderer.java`.
  Single public method `String render(String level)`. Internal pipeline:
  paragraph-split on semantic cues (sentence break followed by "At Oak Vista",
  "Management constraint", "Practical significance", "Critical timing", "Bloom
  period at", and a documented closed list); italicise binomials by regex;
  lift `(1) ... (2) ...` enumerations; pull leading `Genus species (Authority) —
  Family: Subfamily.` into a header chip.
- Plug the renderer into the plants detail template via a small JTE helper.
- The renderer does not yet call any atlas — atlas linking is M7.

**Acceptance.** Visual review of plants detail page in dev: the university
description for `crimson-clover` (the worked example shown to the user) renders
as ≥3 short paragraphs with the leading taxonomic header above the body and
binomials italicised. Renderer is unit-tested with golden-file fixtures over
all four levels of every plant in `plants.json` (no exceptions thrown, no
content lost — character count of the input is conserved or strictly grows).

### M7 — Description renderer atlas integration (forward linking)

**Goal.** Wire `Atlas.resolveAlias` into the renderer. Surface forms that
resolve become anchor tags pointing at the target's console URL; surface forms
that don't resolve stay as italicised text.

**Read first.** M6 output, M2 output, the console's URL conventions
(`PlantsController`, `ChemistryController` route definitions).

**Build.**
- A `LinkResolver` collaborator inside the renderer. Given an `EntityRef`, it
  produces the canonical URL for that entity's console page. Implementation
  reads from a small `Map<Class<? extends EntityName>, Function<EntityName,
  String>>` populated at composition time — each domain's console module
  contributes the URL builder for its own EntityName types.
- The renderer's binomial-italics pass becomes a binomial-italics-and-resolve
  pass: italicise always; wrap in `<a>` only on `Optional.isPresent()`.

**Acceptance.** Visual review: in `crimson-clover.university`, "T. pratense"
remains italic with no link (no red clover entity exists), while "Apis
mellifera" becomes a link to the apiary console once that domain's
contribution is wired (or remains italic-only until then — graceful
degradation is a tested behaviour).

### M8 — Chemistry detail page back-references

**Goal.** Render the inverse panel on `CompoundDetails`. "Found in: plants
(california-pipevine), insects (pipevine-swallowtail)."

**Read first.** M3 + M5 outputs, `domains/chemistry/chemistry-console/`
templates and controller.

**Build.**
- A small `BackReferencesViewModel` populated from `atlas.findReferencesTo(
  compoundName)`. Grouped by `DomainId`, each `EntityRef` rendered as a link
  via the same `LinkResolver` introduced in M7.
- JTE include rendering the panel below the existing compound detail body.
- If the result is empty, render nothing (not "no references found"). Empty
  is the default state for most compounds and a banner per page is noise.

**Acceptance.** Visual review: aristolochic acid detail page lists the plant
and insect entries; an arbitrary compound with no plants/insects references
shows no panel.

### M9 — Observer wiring and metrics

**Goal.** Translate `UnresolvedReferenceObservation` into the operational
surface. Counter for monitoring; structured log line for forensics.

**Read first.** M1's `UnresolvedReferenceObservation`, the existing observer
infrastructure in `kernels/framework/observability/`, `kernels/framework`'s
`Metric.java`.

**Build.**
- `naturalist-web/console/src/main/java/com/naturalist/console/atlas/MicrometerUnresolvedReferenceObserver.java`
  subscribing to `UnresolvedReferenceObservation`. Increments counter
  `naturalist.atlas.unresolved_reference_total` with tags
  `(source_domain, source_name, target_type, target_name)`. Bounded-LRU
  defends `source_name` and `target_name` cardinality, folding overflow
  into `__overflow__`. Cap is configurable; default 256.
- A parallel `LoggingUnresolvedReferenceObserver` emitting one structured log
  per event with full context.
- Wire both observers in the console's composition root.

**Acceptance.** Synthesise an unresolved reference in a test app context;
verify the counter increments with the expected tags and the log line is
emitted. Verify cardinality cap by injecting 300 distinct unresolved
references and confirming the overflow tag captures the surplus.

### M10 — Eager startup validation pass

(NOTE TO CLAUDE: Lets make this a scheduled task for continuos monitoring and fast startup)

**Goal.** Surface deploy-time regressions early. At app startup, walk every
known cross-aggregate reference once through the atlas; the observer
infrastructure from M9 catches any unknowns.

**Read first.** M5 output (the plants `EntityReferences<CompoundName>`
provider), M9 output.

**Build.**
- An `AtlasStartupValidator` invoked from the app's composition root after the
  atlas is assembled. Walks each `EntityReferences<T>` provider's *source side*
  — i.e., for plants, every `PhytochemicalConstituent`'s `compoundName` — and
  calls `atlas.exists(name)` to resolve. Misses fire
  `UnresolvedReferenceObservation` through the same pipeline as runtime misses.
- The walker requires each provider that wants startup validation to also
  implement an `iterateOutboundReferences()` method (an extension interface
  `ValidatableReferences extends EntityReferences<T>` keeps the base SPI
  minimal). Providers that opt out remain runtime-only.

**Acceptance.** Boot the console app with a deliberately broken
`plants.json` referencing a non-existent compound; the metrics and log
observers fire during startup; the app does not fail to boot. Restore the
data and verify zero observations on a clean boot.

### M11 — ArchUnit guard for missing contributions

**Goal.** Build-time safety net for the registration step. A new `EntityName`
subclass without an associated atlas contribution somewhere is a build error.

**Read first.** Existing ArchUnit tests in the project (search for usages of
`com.tngtech.archunit`); `domains/identifiers/`.

**Build.**
- An `AtlasCoverageTest` in a test-scope module the build can run. Discovers
  all concrete `EntityName` subclasses in `identifiers/` and asserts that for
  each, at least one `AtlasContribution` or `EntityReferences<T>`
  implementation exists in some `<domain>-core` module. Failure message names
  the missing class.
- This test runs in app composition test scope (where everything is on the
  classpath together), not in the kernel module.

**Acceptance.** Add a synthetic `EntityName` subclass with no contribution;
the build fails with the expected message. Remove the synthetic class; the
build passes.

### M12 — Documentation and ADR

**Goal.** Capture the architectural decisions in a permanent ADR so future
contributors find them in `docs/adr/` rather than re-deriving them.

**Build.**
- New ADR (next sequence number under `docs/adr/`). Records: the atlas kernel,
  the two SPIs, the routing-not-graph decision, the soft-validation-via-observer
  decision, the per-app composition decision.
- Update `kernels/CLAUDE.md` to mention atlas alongside the other four kernels.
- Update root `CLAUDE.md` module-layout section.

**Acceptance.** ADR merged; kernel and root CLAUDE.md reflect the new module.

---

## How to resume across sessions

A fresh session picking up this work should:

1. Read this PLAN.md in full (it is sized to fit easily).
2. Run `git log --oneline kernels/atlas/` to see which milestones have shipped.
3. Look at the `Read first` list of the next pending milestone and read only
   those files.
4. Do the milestone. Update the milestone's checkbox in this file as part of
   the same commit. Do not edit milestones beyond the one being executed —
   if a discovery during work changes a later milestone, append a note under
   the relevant milestone, do not rewrite it.

If a milestone proves bigger than expected mid-session, split it: leave the
original milestone partially done, add a new milestone immediately after for
the leftover, and surface the split in the commit message.

---

## Open questions

Worth deciding before M9, but not blocking earlier work:

- **Domain enumeration.** `DomainId` constants are defined in
  `kernels/atlas/`. Adding a new domain requires editing the kernel.
  Alternative: make `DomainId` an open value type and discover domains from
  contributions at assembly time. The constant approach is simpler and gives
  us a closed set for low-cardinality tags; the open approach is more
  consistent with the kernel's "no domain knowledge" rule. **Tentative pick:**
  closed constants, accept the small kernel edit when adding a domain.
- **Provider exception policy.** When an `EntityReferences<T>` throws during
  fan-out, do we fail the page render or render a partial result with an
  observation? **Tentative pick:** partial render plus observation (graceful
  degradation matches the soft-validation philosophy).
- **`commonNames` schema location.** Adding `commonNames: string[]` to
  `Plant`, `Insect`, `Compound`, etc. is a coordinated change across api
  modules. **Tentative pick:** add it to each entity that wants it lazily,
  driven by milestones M4 and successors as each domain wires its
  contribution.

---

## Out of scope

These are explicitly *not* part of this effort. Each is a worthy follow-up:

- A graph-shaped query API (`atlas.shortestPath(a, b)` or similar). The
  routing model does not preclude this but does not deliver it.
- Full-text search. The atlas resolves known surface forms, not free text.
- Persistence. The atlas is a memory-resident projection; durable storage is
  the responsibility of each domain's repository.
- Cross-app eventing. Notifying domain A that domain B's reference set
  changed is out of scope; the atlas re-reads on each query and accepts the
  cost.
