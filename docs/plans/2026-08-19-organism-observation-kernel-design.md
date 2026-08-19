# OrganismObservation kernel — unifying the per-domain FieldObservation

Date: 2026-08-19
Status: **SHIPPED 2026-08-19** (12 commits `d1a9ca7f..2b4bb7f6`, full `mvn clean
install` green). Naming per ADR-020: the shared entity is `OrganismObservation<ID,
R>`; per-domain **nested** ports are bare EntitySubject (`ObservationQuery`,
`ObservationRepository`); **standalone** adapters carry the domain prefix
(`InsectObservationQueryImpl`, `PlantObservationTestEntitySource`) — a deliberate
ADR-020 refinement that still needs an ADR update + a broad sweep to prefix the
other standalone adapters (`SpeciesQueryImpl`, …) for consistency (deferred).
Originally: Design (approved for planning) — **REVISED to Design B mid-execution
(2026-08-19): the observation entity is GENERIC over BOTH its id and its subject
rank — `OrganismObservation<ID extends EntityId, R extends RankName>` — with
per-domain ids (`InsectObservationId` / `PlantObservationId`) and per-domain
subject types (`InsectRankName` / `PlantRankName`), not a single shared concrete
`OrganismObservationId` / kernel-`RankName` subject.** Rationale: a single shared id erases the codebase's
typed-identifier discipline — a bare `OrganismObservationId` would not name which
domain's observation it identifies. An interface is impossible (`EntityId` is a
class). Generics keep one shared record structure while each domain binds its own
typed id. Sections below are updated for Design B; the "shared concrete id"
phrasing is superseded wherever it survives.

## 1. Problem

Insects was the reference organism domain; plants conforms to it by mirroring.
That mirroring has produced type-name duplication across domain packages. The
authored entity that stands out is `FieldObservation`, which exists as two
independent records:

- `com.naturalist.insects.FieldObservation` (7 components)
- `com.naturalist.plants.FieldObservation` (6 components — no `identification`)

They are structurally near-identical; the only real difference is the taxonomic
`subject` reference (`InsectRankName` vs `PlantRankName`) and insects' optional
vision `identification`. There is no compile-time collision (distinct packages),
but the duplication costs grep noise, IDE auto-import ambiguity, and ambiguous
review/conversation ("the FieldObservation"). It compounds: `worms`,
`arachnids`, and `microbes` are organism domains that have not yet grown a
`FieldObservation` — unifying now prevents three more copies, not just de-duping
two.

The taxonomic backbone already solved the same problem by domain-prefixing its
entities (`InsectSpecies`/`PlantSpecies`, `InsectImage`/`PlantImage`, …).
`FieldObservation` simply never followed that convention. Rather than prefix it
(which keeps N copies, just tidier), we unify the concept into one shared kernel
type — the concept is genuinely organism-agnostic.

## 2. Goals / non-goals

**Goals**
- One shared observation entity, replacing both per-domain `FieldObservation`
  records, with no loss of type safety on the taxonomic `subject`.
- Establish the reusable **kernel + per-domain codec** pattern that later
  duplicated concepts (Image, breadcrumb trails) will follow.

**Non-goals (explicit, deferred to follow-up specs)**
- `InsectImage`/`PlantImage` unification. Same pattern; separate spec.
- Console breadcrumb / clade-trail unification (a different tier, and the two
  domains have *diverged* — `CladeTrail` record vs `PlantCladeTree` navigator —
  so it is a reconciliation job, not a de-dup).
- Generalizing `subject` beyond taxonomy (a future `SoilObservation` /
  `WeatherObservation` would not have a `RankName` subject). The module is named
  neutrally to leave room, but this spec keeps `subject: RankName`.
- Prefixing the per-domain repository/query plumbing.

## 3. Decisions (locked during brainstorming)

| Decision | Choice |
| --- | --- |
| Scope | Domain-tier kernel only |
| Rank seam | Promote a non-sealed `RankName` to the **taxonomy** kernel |
| Kernel contents | generic `OrganismObservation<ID extends EntityId>` + `Identification`/`Candidate` (NO kernel id type) |
| Id | first type parameter `ID extends EntityId`; per-domain `InsectObservationId` / `PlantObservationId` in `identifiers` (today's `FieldObservationId` renamed), preserving typed-id clarity |
| `subject` representation | second type parameter `R extends RankName`; the field is `R` (domain sealed type, e.g. `InsectRankName`), so subject is domain-typed and exhaustively switchable. Codec stays via field-level annotations |
| Image | Deferred |
| Module name | `kernels/observation` (neutral for future non-organism observations) |
| Entity class name | `OrganismObservation` (coupled to `LinealRank`, hence not bare `FieldObservation`) |
| Per-domain plumbing | Stays per-domain; renamed `OrganismObservation*` only so it does not misdescribe what it queries |

## 4. Module & DAG

New kernel `kernels/observation`, package `com.naturalist.observation`.

```
observation → framework, taxonomy, identifiers
```

Acyclic: `taxonomy → framework`, `identifiers → framework`. Precedent for a
kernel holding a concrete shared type (not just an abstraction): `clades`
(`Clade`) and `field-notes` (`Description`).

**Why not `field-notes`.** `field-notes` is a value-object building block
(`Description`, `CommonName`) with `framework`-only deps, depended on by
`clades`, `biogeography`, and six domain apis. `OrganismObservation` is a
top-level Entity needing `taxonomy` + `identifiers`; folding it into
`field-notes` would force those deps onto every `field-notes` consumer. Keep it
a sibling kernel.

The observation ids are **per-domain** and stay in `domains/identifiers`
alongside the other typed ids: `InsectObservationId` / `PlantObservationId`
(surrogate UUIDv7, `extends EntityId`) — these are today's per-domain
`FieldObservationId` classes **renamed**, not deleted. The kernel holds no
concrete observation id; the entity is generic over it (§5.2). This preserves
the codebase's rule that a typed id names exactly which entity it identifies.

## 5. Types

### 5.1 `RankName` — promoted to `taxonomy`

```java
package com.naturalist.taxonomy;

public interface RankName {          // non-sealed
    String value();
    LinealRank rank();
}
```

`InsectRankName` / `PlantRankName` become `sealed interface … extends RankName`,
keeping their own permit seals. They already declare `value()`/`rank()`; those
declarations move up to `RankName`. Leaf permits (`InsectSpeciesName`, …) are
unchanged — they already implement both methods and keep their `@JsonValue` /
`@JsonCreator`.

**Why the typed permit is preserved (not a generic slug+rank reference).**
Consumers depend on the concrete permit at runtime:
- FK extraction in `*TestEntitySource`: `subject() instanceof PlantOrderName x`
- mock filters / equality: `.equals(PlantSpeciesName.of("solanum-lycopersicum"))`
- console grouping/casts: `(InsectSpeciesName) image.parentName()`
- a tested Jackson contract (`InsectRankNameJacksonTest`) asserting decode →
  `instanceof InsectSpeciesName`.

Widening the *static* field type to `RankName` keeps all of these working: the
*runtime* object is still an `InsectSpeciesName`, so `instanceof`/cast/`equals`
are unaffected.

### 5.2 `OrganismObservation`

```java
package com.naturalist.observation;

public record OrganismObservation<ID extends EntityId, R extends RankName>(
        ID id,
        NaturalistName observedBy,
        @JsonSerialize(using = RankNameSerializer.class)
        @JsonDeserialize(using = RankNameDeserializer.class)
        R subject,
        Instant observedOn,
        @Nullable String notes,
        @Nullable String location,
        @Nullable Identification identification
) implements Entity<ID> {

    @Override public Consumer<? extends Constraints> invariants() { /* as today */ }
    public OrganismObservation<ID, R> withNotes(@Nullable String notes) { … }
    public OrganismObservation<ID, R> withSubject(R subject) { … }
}
```

**Two type parameters** so both the id AND the subject are domain-typed — the
compiler, a human, and an agent all read `OrganismObservation<InsectObservationId,
InsectRankName>` and know exactly which domain and which sealed rank family they
are working with. Insects binds `OrganismObservation<InsectObservationId,
InsectRankName>`, plants `<PlantObservationId, PlantRankName>` — one shared
structure, per-domain typed identity and subject.

Jackson: the field-level `@JsonSerialize`/`@JsonDeserialize` on `subject` win
over the `R` field type (the custom serializer/deserializer are used regardless),
and serialization is unaffected (`id` → bare UUID via `EntityId`'s `@JsonValue`;
`subject` → `RankNameSerializer`). Because `id` and `subject` are type variables,
deserialization needs the concrete parameters: each domain's `TestEntitySource`
reads its catalog with a fully parametric type
(`constructParametricType(OrganismObservation.class, InsectObservationId.class,
InsectRankName.class)`) via the existing `loadFile(relativePath, parser)`
overload — no new `framework-test` hook.

`withSubject` widens its parameter to `RankName`; existing callers passing an
`InsectRankName`/`PlantRankName` still compile.

### 5.3 `Identification` / `Candidate` — moved to the kernel

Already fully organism-agnostic (`confidence`, `evidence`, and
`Candidate.scientificName` is a plain `String` — no typed reference). They move
verbatim into `com.naturalist.observation`. Plants gains a `null`
`identification` slot (its own docs already describe this payload as "deferred").

## 6. The `RankName` codec seam (the primary risk — spike first)

Today `subject` serializes flat via a field-level
`@JsonTypeInfo(EXTERNAL_PROPERTY)` + `@JsonSubTypes` that *names the domain
permits*. A kernel type cannot name domain permits, and the single shared
`TestDataHelper.mapper` (annotation-only, `JavaTimeModule` only) has no domain
knowledge to reconstruct the right permit on read.

**Resolution — self-describing subject + injected reconstructor:**

1. **JSON shape change.** `subject` serializes as a self-contained object
   instead of a flat sibling pair:
   ```json
   { "rank": "GENUS", "value": "salvia" }     // was: "subjectRank":"GENUS","subject":"salvia"
   ```
   Consequence: the insects/plants `OrganismObservation` catalog JSON files are
   migrated to the new shape (small; test data).

2. **Kernel `RankNameSerializer`** (in `taxonomy`, domain-agnostic): writes
   `{rank: subject.rank().name(), value: subject.value()}`. No domain knowledge
   needed on write.

3. **Kernel `RankNameDeserializer`** (`ContextualDeserializer`): reads
   `{rank, value}` and calls a per-domain **`RankNameReconstructor`**
   (`BiFunction<String, LinealRank, RankName>`), obtained from an injected value
   / registered module on the mapper. Domain selection comes from *which
   reconstructor is registered on the mapper that reads that domain's JSON*.

4. **Per-domain reconstructor.** Insects supplies `InsectRankName::of`, plants
   `PlantRankName::of` (both already exist as static factories).

5. **Mapper hook in `framework-test`.** `TestEntitySource` gains an overridable
   `protected ObjectMapper mapper()` defaulting to today's shared
   `TestDataHelper.mapper` (unchanged for every existing source). Only
   `OrganismObservationTestEntitySource` in each domain overrides it to register
   its `RankNameReconstructor`. Bounded blast radius; no rewrite of the shared
   mapper.

Leaf classes used *directly* (`InsectSpecies.name : InsectSpeciesName`) are
untouched — those fields are typed to the concrete permit, not `RankName`, so
they keep serializing as bare strings via `@JsonValue`. The custom (de)serializer
is scoped to the `RankName`-typed `subject` field only.

**Spike (implementation step 1):** prove `OrganismObservation` with an
`InsectSpeciesName` subject round-trips through a mapper configured with
`InsectRankName::of`, asserting decode → `instanceof InsectSpeciesName`. The
existing `InsectRankNameJacksonTest` moves to the kernel and is the oracle. The
exact Jackson binding (injectable value vs `SimpleModule` + `Deserializers`) is
finalized in the spike; the contract above is fixed.

## 7. Migration order

1. **taxonomy:** add `RankName`, `RankNameSerializer`, `RankNameDeserializer`,
   `RankNameReconstructor`; make `InsectRankName`/`PlantRankName` extend
   `RankName`. *(Kernel signature change → clean `mvn install`.)*
2. **observation kernel:** create module with `OrganismObservation`,
   `OrganismObservationId`, `Identification`, `Candidate`.
3. **Spike** the codec round-trip against the moved oracle test.
4. **Insects migration:** delete `insects.FieldObservation`,
   `insects.FieldObservationId`, `insects.Identification`; repoint
   api/core/repository-test/console to the kernel types; override `mapper()` in
   the observation `TestEntitySource` with `InsectRankName::of`; migrate the
   insects observation catalog JSON to the `{rank,value}` shape; rename the
   per-domain plumbing `FieldObservation*` → `OrganismObservation*`.
5. **Plants migration:** same, with `PlantRankName::of`; plants constructs the
   record with `identification = null`.
6. Remove the now-dead per-domain `FieldObservationId` classes from
   `identifiers` and their `TestInsects/PlantsIdentifiers` references; `mvn
   verify` (run locally by the user).

## 8. Ripple inventory (grep the whole repo — do not trust a file list)

Renaming/deleting the two records and widening `subject` touches:
- every `new FieldObservation(...)` construction site (core, console tests,
  repository-test sources);
- `withNotes` / `withSubject` callers;
- `*TestEntitySource` FK method-refs pattern-matching `subject()`;
- JTE templates referencing the observation;
- `TestInsectsIdentifiers` / `TestPlantsIdentifiers` (`FieldObservationId`);
- the per-domain plumbing classes being renamed
  (`FieldObservationQueryImpl/RepositoryMock/TestEntitySource/…`).

Search across `domains/`, `apps/`, and JTE templates; the record-arity change
propagates to every call site, not only the api module.

## 9. Testing

- **Codec oracle:** `InsectRankNameJacksonTest` relocated to the kernel/insects,
  asserting typed-permit reconstruction over the new JSON shape.
- **Kernel:** `OrganismObservationTest` (invariants) and a `RankName` codec
  round-trip test driving `RankNameSerializer`/`RankNameDeserializer` with a
  fake reconstructor. The (de)serializers and reconstructor SPI are production
  persistence code, so they carry unit tests (this is not "test infra for test
  infra").
- **Per-domain repository contract tests:** unchanged in intent; they now store
  the shared type. Domain-specific repo query methods keep their
  validation + null-rejection contract.
- Full `mvn verify` from the repo root after each domain migration.

## 10. Future forks (noted, out of scope)

- **OrganismImage** — unify `Insect/PlantImage` reusing this kernel + codec
  pattern (`parentName : RankName`).
- **Breadcrumb / clade-trail** — shared console-tier module; reconcile the
  insects/plants divergence first.
- **Non-taxonomic observations** — generalizing `subject` beyond `RankName`
  (soil, weather) when such an observation is actually built. Park in the
  parking lot until then.
- **Plumbing prefixing** — whether the per-domain `OrganismObservation*` plumbing
  and the other bare-named `*QueryImpl`/`*RepositoryMock` internals should be
  domain-prefixed for grep-cleanliness.
