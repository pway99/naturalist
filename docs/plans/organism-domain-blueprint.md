# Organism Domain Blueprint — Rank, Identification, Clade

**Reference, not an effort plan.** Nothing here is scheduled. This is the decision
index for any domain cataloguing organisms: what was decided while building insects,
the constraint that forced each decision, where the canonical implementation lives,
and what breaks if you skip it.

**Deliberately a pointer document.** It names decisions and points at source rather
than restating code, on the same principle as `domains/CLAUDE.md`: "reading the source
is faster than a spec." Prose drifts from code; file references do not. If you are
briefing a chat session with no repo access, this file alone is not enough — pair it
with `docs/briefings/insects-domain.md`.

**Audience.** `insects` is complete and is the reference. `plants` is mid-migration
(see [the consistency plan](2026-08-15-plants-domain-consistency-plan.md)). `arachnids`,
`worms` and `microbes` hold a single `*Species` record each with no rank layer.
`fungi`, `molluscs` and `vertebrates` are empty. Six domains will each re-derive this
otherwise.

---

## The problem

A naturalist photographs a beetle. They can say *Carabidae* with confidence; they
cannot say which species, and no amount of squinting will change that. Three bad
answers are available: force a species guess, discard the observation, or park it in a
"pending identification" limbo that never resolves.

The design rejects all three. **The organism is catalogued at the most specific rank
the evidence supports, and that record is a permanent home — not a placeholder.** A
family-rank record is a first-class catalog citizen. When identification later firms
to genus, a genus record is *added alongside*; the family record is never replaced or
migrated.

Everything below follows from that.

---

## A. Rank identity

### A1 — One entity per rank, not one entity with a nullable rank field

Each rank a domain catalogues gets its own `NamedEntity` with its own typed name.
Insects: `InsectOrder`, `InsectFamily`, `InsectGenus`, `InsectSpecies`.

*Why:* a family-rank record and a species-rank record carry genuinely different
components. Collapsing them into one entity with nullable `genus`/`species` fields
makes every consumer null-check its way to the rank, and makes "identified to family"
indistinguishable from "species not filled in yet."

*Source:* `insects-api/.../InsectFamily.java`, `InsectGenus.java`, `InsectSpecies.java`

*Failure mode:* plants currently has this bug in the opposite direction — `PlantSpecies`
carries a `TaxonomicClassification` whose `genus` and `species` are individually
nullable, so a `PlantSpecies` can be catalogued with neither, and five organisms ended up
recorded twice (once as a species-less `PlantSpecies`, once as a `PlantGenus`) under
colliding slugs.

### A2 — Typed upward FK, one link per rank

Each rank entity carries the typed name of its parent: `InsectSpecies.genusName()`,
`InsectGenus.familyName()`, `InsectFamily.orderName()`. Not a string, not a
`TaxonomicClassification`.

*Why:* it makes the chain traversable and enforceable. The `TestEntitySource`
`ForeignKeyConstraint` can only check a typed FK.

*Source:* `insects-api/.../InsectSpecies.java` (component `InsectGenusName genusName`);
`kernels/taxonomy/.../LinnaeanGenus.java` for the rank-level contract these implement

*Failure mode:* without it there is no `forGenusName` query, no ancestry walk, and no
referential integrity — exactly plants' current state, where `PlantSpeciesTestEntitySource`
declares no foreign keys at all.

### A3 — A sealed `<Domain>RankName` over the rank names

A sealed interface permitting every **Linnaean rank name** the domain catalogues,
exposing the slug and the `LinealRank` position. Rank names only — anything that
classifies organisms without being a rung is an orthogonal axis (D1), and admitting one
here is the mistake that section exists to prevent.

*Why:* anything that attaches to an organism — a photo, an observation, a role, a
citation — attaches at *whichever rank was resolved*. One polymorphic field beats four
nullable ones, and a later refinement becomes a single-field update rather than a
migration between columns.

*Source:* `domains/identifiers/.../insects/InsectRankName.java`

*Constraint:* every permit must be **in the same package** as the sealed interface. This
project has no `module-info.java`, so it is all the unnamed module, where the JLS
requires it. See A4.

*Watch:* Jackson dispatch is declared **at the consuming field**, never on the sealed
interface — `@JsonTypeInfo(EXTERNAL_PROPERTY)` + `@JsonSubTypes` on the component. See
`InsectFunctionalRole.parentName`. Declaring it on the interface would wrap every
leaf-class serialization in an envelope. Getting this wrong is silent until a flush.

### A4 — Rank names live flat in `domains/identifiers`, entities in `<domain>-api`

*Why:* other domains reference an organism by name without depending on its api, which
is what keeps the DAG acyclic.

*Why flat:* A3's sealed type requires its permits to share a package. Insects keeps all
thirteen identifiers directly in `com.naturalist.insects` — including `LifeStageName`,
whose entity lives in the `lifestage` sub-package — so the rank names are automatically
co-located. A domain that mirrors its api sub-contexts inside `identifiers` must still
keep every rank name at the domain root, or the sealed type will not compile.

*Source:* `domains/identifiers/src/main/java/com/naturalist/insects/` — flat, thirteen
files, no sub-packages

---

## B. Confidence-bounded placement

This is the part worth copying most carefully, and the part most likely to be
reinvented badly.

### B1 — The confidence bound lives in the elicitation contract

The identifier is *asked* for the rank it can support, and its answer is taken. The
vision tool schema declares `identifiedRank` as an enum over the domain's ranks, and
the system prompt instructs: identify to the most specific rank your confidence
supports, and **do not guess a species — identify at family or order level instead.**

*Why:* the alternative is asking for a species and then second-guessing the answer
downstream against a confidence threshold. That produces a species record you then
have to walk back. Bounding at elicitation means the record is right when written.

*Source:* `insects-core/.../InsectIdentificationCommand.java` — `buildToolSchema()` and
`buildSystemPrompt()`

*Reusable:* the structure is. The entomological framing and the guild enum are not.

### B2 — `IdentifiedRankEntity` — a sealed sum over *fully-formed rank entities*

Not an enum of ranks plus a bag of fields. One permit per rank, each wrapping a
constructed entity, each exposing `rankName()` polymorphically.

*Why:* it converts "which rank did we land at" from a runtime string into compile-time
exhaustive dispatch, and gives the write-side aggregate a single typed handle for FK
validation.

*Source:* `insects-api/.../IdentifiedRankEntity.java`

### B3 — Ancestors are materialised, not merely asserted

Identifying at species also *brings its genus, family and order into existence* as real
records, each with a real four-level `Description` sourced from external authority plus
text generation. Existence is checked per rank; only missing ranks incur external calls.

*Why:* the chain is complete by construction. A genus record that exists only as a
string inside a species' taxonomy is not navigable, not linkable, and not a home for a
future genus-level observation.

*Source:* `InsectIdentificationCommand.enrichParentRanks()` / `enrichIfNew()`

### B4 — External authority enriches; it never gates

Authority lookup failures degrade the result. They never block the identification or
change the rank. Every `tryCollectRef` and `enrichIfNew` swallows its exception and
continues with a fallback description.

*Why:* an offline EOL must not stop a naturalist cataloguing what they just saw.

*Source:* `InsectIdentificationCommand` — the class javadoc states it outright, and
`writeCitations` documents precisely why swallowing is defensible *there* and not
elsewhere.

### B5 — All external calls complete before the transaction opens

Vision, authority lookup, content fetch, text generation — all done. The transaction
performs pure database writes.

*Why:* holding a transaction open across a network call to a third party is how you get
lock contention and partial writes tied to someone else's latency.

*Source:* `InsectIdentificationCommand.identify()` — the numbered comments mark the
boundary explicitly.

### B6 — One aggregate, one transaction, cross-entity invariants

The write is a single `Aggregate` whose invariants assert FK coherence *before*
anything is persisted: the image's parent must equal the identified rank, the
observation's subject must equal the identified rank, the image's observation id must
equal the observation's id.

*Why:* these are exactly the constraints no single entity can check for itself.

*Source:* `insects-api/.../CatalogIdentification.java`. Each invariant is a named
private method returning true vacuously when null — nullity is validated separately by
`namedEntity`/`valueObject`, so a null component reports one violation, not two.

### B7 — Confidence and evidence attach to the observation, not the taxon

`Identification(confidence, evidence, alternatives)` hangs off `FieldObservation`. A
manual sighting carries `null` there.

*Why:* confidence is a fact about *this sighting event*, not about the organism. Two
naturalists can identify the same species with different confidence, and the taxon
record should carry neither.

*Source:* `insects-api/.../Identification.java`

*Subtle and important:* `Identification.Candidate.scientificName` is a plain `String`,
**not** a typed rank name. A runner-up may name a taxon absent from the catalog, so it
must carry no cross-entity reference. Typing it would make every rejected alternative a
dangling FK.

### B8 — Refinement is additive; the ladder is directional

Rank records are never replaced or migrated. `LinealRank` is ordinal-ordered from
`KINGDOM` to `SUBSPECIES` specifically so a rank transition can be constrained to move
*down* the ladder.

*Source:* `kernels/taxonomy/.../LinealRank.java` — the javadoc states the intent.

*Status:* the enum and its ordering exist; no domain currently enforces the
down-only rule. Enforce it when a re-identification path is built.

---

## C. Attachment and inheritance

### C1 — Cross-rank entities key on `<Domain>RankName`, not on the species

Anything attaching to an organism takes the sealed rank name. Insects does this for
`InsectImage.parentName`, `FieldObservation.subject`,
`InsectFunctionalRole.parentName`, and `InsectFeatureAssignment`.

*Why:* an organism identified only to family still has photographs, still has
ecological roles, still belongs in someone's collection.

*Source:* `insects-api/.../InsectFunctionalRole.java` — its javadoc records the
threshold plainly: "one consumer of the pattern was coincidence; two consumers are a
pattern."

*Precedent worth knowing:* insects originally carried functional-ecology fields on
`InsectSpecies` and migrated them off (PL-11). If you are about to add
`roles`/`guilds` to a rank entity, you are about to repeat the mistake. Plants took the
cross-rank route from the start on that basis.

### C2 — Inherited attributes resolve by walking ancestry, tagged with provenance

Features and citations are not copied down the chain. They are resolved at read time by
walking the subject rank plus its ancestors, so a species page shows its own field
marks *and* those inherited from its family, each labelled with where it came from.

*Source:* `insects-core/.../InsectAncestryResolver.java` — shared by
`InsectCitationQueryImpl` and `InsectFeatureQueryImpl`. The traversal is the reuse; the
result types stay distinct.

---

## D. Orthogonal axes — dimensions that are not rank

### D1 — Tell a rung from an axis before you model it

A domain will meet dimensions that classify the same organisms without being Linnaean
ranks: which clade it sits in, which cultivated variety it is, which morphotype or
strain. The mistake is filing them as extra rungs on the ladder. They are **second
axes** — independent classifications over the same records.

The distinction is not stylistic. A rung extends the chain and participates in ancestry
walks, `LinealRank` ordering, and rank-transition rules. An axis does none of that; it
crosses the chain. Two organisms can share a rank and differ on the axis, and share an
axis value while differing in rank.

**Three signals that you are holding an axis, not a rung:**

1. **It has no `LinealRank` value.** The shared ladder is closed by biology. If your
   concept has no position on it, that is the ladder telling you it is not on it.
2. **Adding it to `<Domain>RankName` forces you to delete a method.** If `rank()` has to
   become nullable or disappear so the new permit fits, the permit does not belong. An
   abstraction that has to shed behaviour to admit a member is being widened past its
   meaning.
3. **Its relationship to the chain is membership, not extension.** A cultivar belongs to
   a species; a clade contains ranks. Neither continues the chain downward or upward.

*Failure mode, observed:* plants nearly put `CultivarName` into `PlantRankName`. Both
signals fired — `rank()` had to go, and because the permits of a sealed type in the
unnamed module must share a package (JLS), it would additionally have forced a four-package
identifier move. That is an expensive amount of work to make a concept fit a type it does
not belong in. Model it as an axis and every cost disappears.

### D2 — Clade is a curated sealed vocabulary, not an entity

`kernels/clades` ships one stateless record permit per recognised clade. No repository,
no JSON seed. Adding a clade is a deliberate kernel PR.

*Why:* the catalog is small and curated, and needs value-equal references to the same
logical node across every domain so trait inheritance by traversal works.

*Source:* `kernels/clades/`, and the rationale in `kernels/CLAUDE.md`

### D3 — Rank entities carry `@Nullable Clade placedIn` — worked example 1

Clade placement is a *second, independent* axis over the same records. A rank entity is
placed in a clade; the two hierarchies do not have to agree, and neither derives from
the other. The record carries both: `name` for rank identity, `placedIn` for clade.

*Source:* `insects-api/.../InsectSpecies.java` and its siblings — component
`@Nullable Clade placedIn`, plus a `withPlacedIn` method per rank entity

### D4 — Traits are domain-owned, declared as a pure function

The kernel holds no trait declarations. Each domain writes a pure
`Function<Clade, Set<Trait>>` — a pattern-matching switch with `default -> Set.of()` —
and passes it to `CladeTraversal.findTrait`.

*Why:* plants and insects declare different traits over the *same* clade values without
a shared registry or startup wiring, because the clade values are value-equal.

*Source:* `insects-api/.../InsectClades.java` — three cases and a default; that is the
whole file.

### D5 — Cultivar is an axis, not a rung — worked example 2

A cultivated variety is a horticultural selection *within* a species, not a rank below
it. Plants models it as `Cultivar`, its own `NamedEntity` with its own `CultivarName`,
carrying a `plantName` FK to the species it belongs to — exactly the membership
relationship D1 describes.

Consumers that need to reference "what was planted, at whatever specificity is known"
carry **both axes as separate components**, the same shape as `name` + `placedIn`:

```java
Planting(@Nullable PlantRankName subject, @Nullable CultivarName cultivarName, ...)
```

`subject` is rank-flexible, so a planting can be recorded at family, genus or species —
this is what makes "I planted a salvia" expressible. `cultivarName` is the orthogonal
selection within it. Both are nullable with an at-least-one invariant: the axes are
independent, so a record may carry either, and a disjunction *across two axes* is a
different thing from the A1 anti-pattern of parallel nullable fields on one axis.

*Why not one field:* collapsing them into a single sealed union produces a type meaning
"Linnaean rank **or** horticultural selection", which is two concepts wearing one name,
and it drags in every cost D1 lists. Keeping them separate also lets a consumer state
both at once — species *and* cultivar — which a union cannot express at all.

*Status:* shipped 2026-08-15. `garden.Planting` took a `PlantSpeciesName` until then, which
restricted every planting to species-level identification — the Linnaean axis could not
express a genus-rank record at all.

*Source:* `plants-api/.../cultivar/Cultivar.java`, `garden-api/.../Planting.java`

---

## E. Read side and console

### E1 — A sealed `*TaxonView` read model, one permit per rank

Composes a rank entity with what hangs off it. A `ReadModel`, not an `Aggregate` — it
owns nothing and is never mutated as a unit. Assembled by a package-private factory in
`<domain>-core`, never inline in the query.

*Source:* `insects-api/.../InsectTaxonView.java` and the `*View` permits; factory at
`insects-core/.../InsectTaxonViewFactory.java`. Rationale in `domains/insects/CLAUDE.md`.

### E2 — Every rank gets a console page carrying the same evidence surfaces

Because most identifications land at ORDER or FAMILY rather than SPECIES, the rank
pages are not a lesser version of the species page. They carry the same field marks,
photo gallery, evidence disclosure and citations.

*Source:* `insects-console/src/main/jte/insects/family.jte`, `genus.jte`, `order.jte`

### E3 — Every rank name the catalog indexes must resolve to a URL

A domain's `EntityRefLinker` needs a case for *every* name type its
`CatalogContribution` emits. `SearchController` silently drops any hit whose linker
returns null.

*Source:* `insects-console/.../catalog/InsectsLinker.java` and — more importantly —
`InsectsLinkerTest.java`

*Failure mode, observed:* plants indexed families and genera for search while its linker
handled neither, so every family and genus hit vanished from results with no error
anywhere. A linker test is what catches this; nothing else will.

### E4 — Two console traps, both learned expensively

**Never add `spring-security-web` to a domain-console pom.** `layout/page.jte` is
compiled by every domain-console module's template tests against classpaths that
deliberately lack it. Read the CSRF param and token as plain request attributes
(`naturalistCsrfParam` / `naturalistCsrfToken`) instead of typing a `CsrfToken` param.
An earlier attempt added the jar and was reverted in review.

**Rank pages pass `forParentName`, not `forRankHierarchy`.** The child-rank cards on
the same page already show descendant photos; the hierarchy query renders every
descendant image twice.

*Source:* both documented at length in `domains/insects/CLAUDE.md`.

---

## The ladder is per-domain — and the ladder is Linnaean

**Document the mechanism; do not copy insects' ranks.**

`LinealRank` declares `KINGDOM → SUBSPECIES` and is shared. Which of those rungs a domain
*catalogues as entities* is its own decision. Anything that is not a rung on that ladder
is an orthogonal axis (section D), never an extra permit:

| Domain  | Ladder (rungs with entities)                  | Orthogonal axes  |
|---------|-----------------------------------------------|------------------|
| insects | ORDER → FAMILY → GENUS → SPECIES → SUBSPECIES | clade            |
| plants  | FAMILY → GENUS → SPECIES                      | clade, cultivar  |
| fungi   | undecided                                     | undecided        |

Two consequences:

- **`<Domain>RankName` permits only rungs that have an entity.** Three for plants today,
  five for insects. Do not permit a rank with no record behind it, and do not permit a
  non-rank — `CultivarName` belongs to the cultivar axis, not this type.
- **`rank()` returning `LinealRank` therefore always works.** Every permit is a Linnaean
  rung by construction, so the method needs no nullable or `Optional` return. If you find
  yourself wanting one, re-read D1 — you are about to admit an axis as a rung.

A further constraint that decides where rank names live: **a sealed type's permits must
all sit in the same package.** This project has no `module-info.java`, so everything is
the unnamed module, where the JLS requires exactly that. Insects satisfies it by keeping
all thirteen identifiers flat in `com.naturalist.insects`, including `LifeStageName`,
whose entity lives in the `lifestage` sub-package. Plants mirrors its api sub-contexts
instead (`cultivar/`, `heritage/`, …), which is fine *because* its three rank names are
flat — but it is why admitting `CultivarName` would have forced a package move.

If you are adding a domain and find yourself copying `InsectRankName`'s five permits,
stop. Copy its *shape*.

---

## Build order for a new organism domain

Each step is a PR. Order is dependency-driven; nothing later works without the earlier.

1. **Decide the ladder and the axes.** Which Linnaean rungs get entities, and what other
   dimensions classify these organisms without being rungs (clade, cultivar, strain,
   morphotype)? Run each candidate past D1 before assuming it belongs on the ladder.
   Record both lists in the domain's `CLAUDE.md` before writing code.
2. **Rank names** in `domains/identifiers/.../<domain>/`, plus the sealed
   `<Domain>RankName`. Includes the Jackson-dispatch decision (A3).
3. **Rank entities** in `<domain>-api`, each with its typed upward FK, implementing the
   matching `kernels/taxonomy` `Linnaean*` contract. Invariant tests per record.
4. **Test sources + JSON catalogs**, with a `ForeignKeyConstraint` per FK. Add a
   catalog-data test asserting every referenced parent exists — see
   `PlantGenusCatalogDataTest` or `ElementCatalogDataTest`.
5. **Repositories + queries** for each rank, including the `forParentName` rollups.
   Mock argument validation and three contract cases per method.
6. **Cross-rank attachments** as needed (images, observations, roles), all keyed on
   `<Domain>RankName` (C1).
7. **Console**: a page per rank, an `EntityRefLinker` case per name type, and a linker
   test (E3).
8. **Clade placement**: `placedIn` on the rank entities plus a domain trait function
   (D2, D3).
9. **Identification**: the confidence-bounded flow (section B). Last, because it needs
   everything above.

Steps 1–5 are the minimum for a domain to hold real records. Steps 6–9 are additive and
independently useful.

---

## What insects has not solved

Do not treat these as settled just because insects shipped:

- **Down-only rank transitions are unenforced.** `LinealRank` orders the ladder; nothing
  checks the direction on re-identification.
- **Feature dedup by value is unimplemented.** Each identification creates fresh
  `InsectFeature` records, so the catalog holds near-duplicates.
- **No subspecies entity exists**, despite `InsectSubspeciesName` being a permitted
  rank name — `taxonView().getByName()` returns empty for it.
- **The two-axis shape has one consumer, in one domain.** `garden.Planting` carries
  `subject` + `cultivarName` (D5), but insects has never needed a second axis beside
  clade, so the pattern is proven once rather than repeatedly.
- **No hierarchy-walking planting query.** `PlantingQuery.forSubject` matches the rank
  exactly, so querying a genus does not return plantings of its species. Insects solved
  the equivalent with a separate `forRankHierarchy`; garden has no such method yet.

---

## Pointers

- Reference implementation: `domains/insects/` — start at
  [`domains/insects/CLAUDE.md`](../../domains/insects/CLAUDE.md)
- Shared conventions: [`domains/CLAUDE.md`](../../domains/CLAUDE.md)
- Kernels this depends on: [`kernels/CLAUDE.md`](../../kernels/CLAUDE.md) —
  `taxonomy`, `clades`, `field-notes`, `catalog`
- Live migration applying this: [plants consistency plan](2026-08-15-plants-domain-consistency-plan.md)
- Identification roadmap and its FU-1 framing: [identification.md](identification.md)
