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

*Failure mode:* plants currently has this bug in the opposite direction — `Plant`
carries a `TaxonomicClassification` whose `genus` and `species` are individually
nullable, so a `Plant` can be catalogued with neither, and five organisms ended up
recorded twice (once as a species-less `Plant`, once as a `PlantGenus`) under
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
referential integrity — exactly plants' current state, where `PlantTestEntitySource`
declares no foreign keys at all.

### A3 — A sealed `<Domain>RankName` over the rank names

A sealed interface permitting every rank name the domain catalogues, exposing the slug
and the `LinealRank` position.

*Why:* anything that attaches to an organism — a photo, an observation, a role, a
citation — attaches at *whichever rank was resolved*. One polymorphic field beats four
nullable ones, and a later refinement becomes a single-field update rather than a
migration between columns.

*Source:* `domains/identifiers/.../insects/InsectRankName.java`

*Watch:* Jackson dispatch is declared **at the consuming field**, never on the sealed
interface — `@JsonTypeInfo(EXTERNAL_PROPERTY)` + `@JsonSubTypes` on the component. See
`InsectFunctionalRole.parentName`. Declaring it on the interface would wrap every
leaf-class serialization in an envelope. Getting this wrong is silent until a flush.

### A4 — Rank names live in `domains/identifiers`, entities in `<domain>-api`

*Why:* other domains reference an organism by name without depending on its api, which
is what keeps the DAG acyclic.

*Source:* `domains/identifiers/src/main/java/com/naturalist/insects/`

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

## D. Clade placement — orthogonal to rank

### D1 — Clade is a curated sealed vocabulary, not an entity

`kernels/clades` ships one stateless record permit per recognised clade. No repository,
no JSON seed. Adding a clade is a deliberate kernel PR.

*Why:* the catalog is small and curated, and needs value-equal references to the same
logical node across every domain so trait inheritance by traversal works.

*Source:* `kernels/clades/`, and the rationale in `kernels/CLAUDE.md`

### D2 — Rank entities carry `@Nullable Clade placedIn`

Clade placement is a *second, independent* axis over the same records. A rank entity is
placed in a clade; the two hierarchies do not have to agree, and neither derives from
the other.

*Source:* `insects-api/.../InsectSpecies.java` and its siblings — component
`@Nullable Clade placedIn`, plus a `withPlacedIn` method per rank entity

### D3 — Traits are domain-owned, declared as a pure function

The kernel holds no trait declarations. Each domain writes a pure
`Function<Clade, Set<Trait>>` — a pattern-matching switch with `default -> Set.of()` —
and passes it to `CladeTraversal.findTrait`.

*Why:* plants and insects declare different traits over the *same* clade values without
a shared registry or startup wiring, because the clade values are value-equal.

*Source:* `insects-api/.../InsectClades.java` — three cases and a default; that is the
whole file.

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

## The ladder is per-domain

**Document the mechanism; do not copy insects' ranks.**

`LinealRank` declares `KINGDOM → SUBSPECIES` and is shared. Which rungs a domain
*catalogues as entities* is a domain decision:

| Domain    | Ladder                                             |
|-----------|----------------------------------------------------|
| insects   | ORDER → FAMILY → GENUS → SPECIES → SUBSPECIES      |
| plants    | ORDER → FAMILY → GENUS → SPECIES → **CULTIVAR**    |
| fungi     | undecided                                          |

`Cultivar` is **not a Linnaean rank**. It sits below species, is already modelled in
plants as its own `NamedEntity` with its own name type, and has no `LinealRank` value.
Everything in sections A–E applies to it unchanged *except* anything that assumes
`LinealRank` covers the ladder:

- `<Domain>RankName` permits whatever names the domain catalogues — three for plants
  today, not five. Only permit a rank that has an entity.
- `RankName.rank()` returning `LinealRank` does not generalise to infraspecific ranks.
  A domain with non-Linnaean rungs needs either a nullable return or its own rank type.
  **Unresolved — decide when plants reaches it.**
- Ancestry walks and ordering must not assume `LinealRank` ordinal comparison covers
  every rung.

If you are adding a domain and find yourself copying `InsectRankName`'s five permits,
stop. Copy its *shape*.

---

## Build order for a new organism domain

Each step is a PR. Order is dependency-driven; nothing later works without the earlier.

1. **Decide the ladder.** Which rungs get entities, and are any non-Linnaean? Record
   the answer in the domain's `CLAUDE.md` before writing code.
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
- **The `LinealRank`-covers-every-rung assumption** breaks at plants' cultivar. Called
  out above; still open.

---

## Pointers

- Reference implementation: `domains/insects/` — start at
  [`domains/insects/CLAUDE.md`](../../domains/insects/CLAUDE.md)
- Shared conventions: [`domains/CLAUDE.md`](../../domains/CLAUDE.md)
- Kernels this depends on: [`kernels/CLAUDE.md`](../../kernels/CLAUDE.md) —
  `taxonomy`, `clades`, `field-notes`, `catalog`
- Live migration applying this: [plants consistency plan](2026-08-15-plants-domain-consistency-plan.md)
- Identification roadmap and its FU-1 framing: [identification.md](identification.md)
