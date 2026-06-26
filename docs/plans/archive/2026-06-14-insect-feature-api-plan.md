# InsectFeature API — Plan

**Status.** Design conclusion from a chat brainstorm (2026-06-14). Feeds a
fresh Claude Code session. This is **Step 0 of the identification module**
— a prerequisite that must land before the Order-level ground stage is
real. Per-domain, lands in `domains/insects/`,
`com.naturalist.insects` (confirm package against the tree).

**Relationship to `identification-module-design.md`.** That doc listed
`IdentificationFeatures` under "reused, unchanged" (§3) and had the ground
stage display it at Order (§8). That was wrong in one specific way:
`IdentificationFeatures` exists, but as a **nested VO on `InsectSpecies`**
— species-only. The identification walk concludes at **Order** in Phase 2,
where there is no `InsectSpecies` in hand to read it off. This plan fixes
that rank mismatch by making features available at *any* rank. After this
lands, the main doc's §3 should move features from "reused as-is" to
"reused via this plan," and §8's "diagnostic checklist at the concluded
rank" becomes real for Order.

**Pair with.** `insects-domain.md` (for `InsectSpecies`,
`InsectCitationView`, `RankedCitation`, the rank types),
`framework-kernel.md` (for `NamedValue`, `ValueObject`, `ReadModel`,
`BehavioralCollection` conventions).

---

## 1. Abstract

`IdentificationFeatures` is the curated, ordered (conspicuous→diagnostic)
list of field marks for a taxon — what a member *looks like*, shown after
a conclusion so the naturalist can check it against the organism in hand.
Today it is a species-only nested VO carrying `List<String>`. This plan
does three things:

1. **Type the feature.** `String` → `InsectFeature`, a
   `NamedValue<String>` with normalization in its `of(...)` factory. This
   makes feature equality reliable (so "Sucking mouthparts" and "sucking
   mouthparts" are the same value) and makes the reverse query below
   *sound* rather than fragile string-matching.
2. **Make features available at any rank.** Lift the
   `IdentificationFeatures` VO off `InsectSpecies`-only so order, family,
   and genus can carry features too, and add a lineage-walk read model —
   `InsectFeatureView` — that composites a rank's own features with those
   inherited from its ancestors. This is the **exact mechanism
   `InsectCitationView` already uses** for citations; the two are read
   models over the *same* traversal.
3. **Enable browse-by-feature.** A reverse query — "which taxa carry this
   feature" — implemented as a scan over `InsectFeature` value-equality. A
   naturalist's browse aid (e.g. "show me insects with chewing
   mouthparts"), explicitly *not* a phylogenetic claim (see §6).

**Two decisions are made on purpose and must not be silently reversed**
(§7): the feature value space stays **open** (no enum / closed registry),
because the same feature string legitimately denotes *non-homologous*
characters in different lineages; and features stay **intrinsic content
owned by the rank** (no shared `Feature` entity, no association, no
`EntityRef`), because they fail the tests that justify the
citation-association machinery.

**One decision is left open** (§5): whether higher ranks carry the VO
directly (if they are entities) or features move to a rank-keyed catalog
(if they are not). This hinges on a fact about the tree this plan cannot
see. The read side is identical either way.

---

## 2. The types

```java
// the typed feature value. OPEN value space (§7.1). Normalize in of().
record InsectFeature(String value) implements NamedValue<String>
// @JsonCreator static of(String) — trim, case-fold, canonical spelling.

// the ordered collection wrapper. Promoted from nested-on-InsectSpecies
// to a top-level VO so any rank can carry it. Ordering contract below.
record IdentificationFeatures(List<InsectFeature> features)
    implements ValueObject
// invariant: features non-null; authored order is conspicuous→diagnostic.

// provenance-tagged feature, exactly mirroring RankedCitation
record RankedFeature(
    InsectRankName sourceRank,   // which lineage rank contributed this feature
    InsectFeature feature
) implements ValueObject

// the read model, exactly mirroring InsectCitationView
record InsectFeatureView(
    InsectRankName subject,
    List<RankedFeature> features   // composited up the lineage
) implements ReadModel
```

- `InsectFeature` and `IdentificationFeatures` live in
  `com.naturalist.insects` alongside the other domain value objects
  (confirm). `IdentificationFeatures` is **no longer nested** inside
  `InsectSpecies` — promoting it to top-level is part of this work (§8).
- No Spring annotations in api. `NamedValue` uses `@JsonCreator` on
  `of(...)` only; no `@JsonCreator` on the records.

---

## 3. The read mechanism — lineage walk (shared with citations)

`InsectFeatureView` is assembled the same way `InsectCitationView` is:
walk the subject rank's lineage (e.g. species → genus → family → order),
collect each rank's `IdentificationFeatures`, tag each with its source
rank as a `RankedFeature`, and composite.

**The shared abstraction is the lineage walk, not the storage.** Features
and citations have *nothing* structurally in common except that both need
"everything at this rank plus everything inherited from ancestors, tagged
with provenance." That traversal is the reuse. Confirm whether the walk
`InsectCitationView` performs is already a shared helper or inlined in the
citation view's impl; if inlined, consider extracting it so both views
share one traversal rather than duplicating it. **Do not** model features
*as* citations to get the walk (see §7.2) — share the walk, not the type.

### Ordering contract

The conspicuous→diagnostic ordering survives and extends for free:

- **Within a rank:** preserve the authored order (conspicuous→diagnostic).
- **Across ranks:** ancestor ranks first (most general), descendant ranks
  last (most specific). The two orderings align — general features read
  conspicuous, specific features read diagnostic — so the composited list
  is globally conspicuous→diagnostic.

`RankedFeature.sourceRank` lets the consumer group the display
("Order-level marks: … / Family-level marks: …") and is what makes the
Order-first ground stage useful: at an Order conclusion the view returns
just the order's features, and the list grows naturally as walks deepen.

### Query surface

Mirror the citation surface exactly. Citations are reached via
`insectQuery.citations().findByRankName(rank)`; features mirror it:

```java
interface InsectFeatureQuery {
    InsectFeatureView findByRankName(InsectRankName subject);  // forward: lineage composite
    <CollectionType> findByFeature(InsectFeature feature);     // reverse: see §6
}
```

Reached via `insectQuery.features()` (confirm the facade method name
against how `.citations()` is exposed). Multi-result returns are
`BehavioralCollection` subclasses per convention, never raw `List`.

---

## 4. Why `NamedValue`, not `String`

`NamedValue<String>` buys a typed identity and a **normalization seam** in
`of(...)` — the same wrapper discipline every other domain value carries
(`TaxonomicOrder`, etc.). It is a strict upgrade over `List<String>` and
should be taken regardless of the §5 fork. What it fixes precisely: the
*form* of a feature is now canonical, so equality is reliable and the
reverse query (§6) matches on a sound value instead of brittle
`String.equals`. What it does **not** do is impose a controlled
vocabulary — that would be a *closed* value space, which §7.1 rules out on
purpose. `NamedValue` is the right container; it is deliberately left open.

---

## 5. OPEN DECISION — where higher-rank features are authored

Features must be available at any rank (the requirement). *Authoring*
them depends on a fact this plan cannot see: **which insect ranks are
first-class entities with records today?** Confirm against the tree, then
pick. The read side (§3) is identical either way, so this choice never
leaks to the ground stage and can be switched later.

### Option A — ranks are entities: lift the VO onto each rank

If `InsectOrder` / `InsectFamily` / `InsectGenus` are entities, give each
a `@Nullable IdentificationFeatures identificationFeatures` component
(`InsectSpecies` already has it). Optionally a marker interface so the
view walks them homogeneously:

```java
interface FeatureBearing {
    @Nullable IdentificationFeatures identificationFeatures();
}
// InsectOrder, InsectFamily, InsectGenus, InsectSpecies implement it
```

Features are authored locally in each rank entity's existing JSON source.

*Pros:* features stay intrinsic content *on* the taxon (conceptually
exact); no new top-level catalog type; species data untouched in place.
*Con:* requires every queryable rank to be an entity — if some rank isn't,
A forces promoting it to one (out of scope for this plan).

### Option B — not all ranks are entities: a rank-keyed catalog

If higher ranks are not all entities, features move to a single store
keyed by `InsectRankName` — **association-shaped but never via
`EntityRef`**, and with no association record (the feature is the payload,
§7.2):

```java
record InsectRankFeatures(
    InsectRankName rank,             // natural key = the rank described
    IdentificationFeatures features
) implements NamedEntity<InsectRankName>
// loaded from insect-rank-features.json via the repository-mock convention
```

For uniformity **all** features move here, including species' — so
`InsectSpecies` loses its `IdentificationFeatures` field (otherwise a
hybrid: species features on the entity, higher-rank features in the
catalog). The view queries this catalog per lineage rank and composites.

*Pros:* works regardless of which ranks are entities; one uniform store
and query path; mirrors the citation decoupling you already know. *Cons:*
features read as a side catalog though they're conceptually intrinsic
(mild mismatch); relocating species features is a migration;
`InsectRankFeatures` shares the `InsectRankName` keyspace with the rank
entities (allowed — same way citations key by rank without colliding).

**Lean:** A if the ranks are entities (smaller, more correct, leaves the
existing VO in place); B only if they are not. The single fact decides it.

---

## 6. The reverse query — browse by feature (scan, with a caveat)

`findByFeature(InsectFeature)` answers "which taxa carry this feature"
(e.g. "all insects with sucking mouthparts"). The `NamedValue` equality
from §4 makes this **sound** — it matches a canonical value, not raw text.

**Implemented as a scan, not an index.** Iterate the feature store (rank
entities in A, the catalog in B), match on `InsectFeature` equality,
return the carrying ranks. No inverted index is built — scan is adequate
until proven costly (§7.3). The query interface is identical in A and B;
only the scan target differs.

**The homology caveat (state it in the API doc, not just here).** This
query matches on feature *value* — i.e. *form*, not *homology*. Because
the value space is open and context-dependent (§7.1), the same
`InsectFeature("reduced-wings")` can denote non-homologous characters in
different lineages. So `findByFeature` is a **naturalist's browse aid**,
not a phylogenetic or homology claim. For a deep, clean character
(mouthparts) the result is meaningful; for a context-dependent one
("aquatic," "reduced wings") it is a loose net lumping unlike characters.
That is acceptable — it is exactly the register this app operates in — but
it must be labeled so no consumer mistakes it for a cladistic assertion.

Return type is a design choice to confirm: a `BehavioralCollection` of
`InsectRankName` (the carrying ranks) is the minimal answer; a richer
return (resolved `InsectTaxonView`s) is heavier. Default to the rank-name
collection and let the consumer resolve.

> **Scope note.** The reverse query is the *data seed* of multi-access
> ("Lucid-style") identification — answer characters in any order,
> intersect candidate sets. The identification module deliberately chose
> single-next couplet keys over multi-access for Phase 2. Shipping
> `findByFeature` here does **not** commit to multi-access; it is a browse
> aid over data you already have. Multi-access keying remains out of scope
> and unbuilt.

---

## 7. Decided and discarded (preserve these reasons)

### 7.1 Open value space — NO closed registry / enum (decided)

`InsectFeature`'s value space stays **open**. A future session must not
"helpfully" close it into an enum, sealed set, or validated registry.

**Reason — context-dependence, not disagreement.** The same feature string
denotes genuinely *different, non-homologous* characters depending on
where in the tree it appears. "Reduced wings" is brachyptery in a beetle
and aptery in an ant — same words, different character. "Aquatic" is gills
in a mayfly nymph and an air bubble in a diving beetle — the feature that
*matters* differs. A closed registry would force one `InsectFeature` value
to mean one thing everywhere, which is biologically false for exactly the
features that look most shareable. Ownership-by-rank (§5) carries the
disambiguating context: the string matches, the meaning lives in the
owning taxon. Open `NamedValue` gives reliable *form* without asserting
universal *meaning* — which is precisely correct.

(Note: this is the insects-domain, one-class feature model. It is *not*
the deferred cross-domain typed-trait vocabulary — see §7.4.)

### 7.2 Features are intrinsic — NO shared entity / association (decided)

Features are **not** modeled as a shared `Feature` entity bound to taxa by
a `FeatureAssociation`/`EntityRef`, the way `Citation` /
`CitationAssociation` are. Run the three tests that justify that
machinery and features fail all three:

- **Independent entity?** No — a feature has no identity or lifecycle apart
  from the taxon it describes. (A citation exists independently and binds
  to many subjects; a feature is authored *about* a rank.)
- **Cross-domain?** No — and this is the sharp one. `EntityRef` exists to
  *erase* the domain (a paper is a paper whether about beetles or basil). A
  feature's meaning *depends* on its domain: "wings held flat at rest" is
  meaningless outside Insecta. Pushing features through `EntityRef` would
  be actively wrong — it forgets the very context the feature needs.
- **Does the binding carry its own data?** No — there is no annotation on
  "this rank has this feature." The feature *is* the data; there is no
  `note`-equivalent to host.

So a citation is *extrinsic, shared, cross-domain* → needs an association
to a domain-erased reference. A feature is *intrinsic, owned, domain-bound*
→ lives on the rank, in the domain's vocabulary. The only thing they share
is the **lineage walk** (§3), and that is shared at the read model, not by
unifying the types.

(The reverse query in §6 does *not* reopen this — it is a scan over
value-equality, not a stored bidirectional binding.)

### 7.3 No inverted index for reverse lookup (deferred, YAGNI)

`findByFeature` is a scan (§6). An inverted feature→ranks index is *not*
built now. Build it only if scan cost or a real multi-access need bites —
same `@Incubating` restraint applied elsewhere. The `NamedValue` keeps the
eventual index sound; nothing here forecloses it.

### 7.4 Relationship to the deferred typed-trait kernel SPI (note)

The open `InsectFeature` `NamedValue` is the **insects-domain, one-class**
feature model. It is distinct from the cross-domain *typed Class-scoped
trait vocabulary* deferred in `identification-module-design.md` §13.9
(the `clades`-`Trait`-style SPI that arrives when a multi-class domain
forces it). They are related (both are "discriminating/descriptive
character vocabularies") but separate: this plan ships now for insects;
the typed-trait SPI is a kernel concern for later. Do not conflate them —
in particular, §7.1's open-value decision is an insects authoring choice,
not a statement about the future kernel SPI.

---

## 8. Migration steps

1. **Add `InsectFeature : NamedValue<String>`** in `com.naturalist.insects`
   with a normalizing `@JsonCreator of(String)` (trim, case-fold, canonical
   spelling). Tests for normalization equality.
2. **Promote `IdentificationFeatures` to a top-level VO** in
   `com.naturalist.insects` (it is currently nested in `InsectSpecies`),
   and change its component `List<String>` → `List<InsectFeature>`. Update
   `InsectSpecies` to reference the now-top-level type.
3. **Make features rank-available** per the §5 decision:
   - *Option A:* add `@Nullable IdentificationFeatures` to the other rank
     entities (+ optional `FeatureBearing` marker); author higher-rank
     features in their JSON sources.
   - *Option B:* add `InsectRankFeatures : NamedEntity<InsectRankName>`,
     `insect-rank-features.json` + repository-mock, and **remove**
     `IdentificationFeatures` from `InsectSpecies` (all features move to the
     catalog).
4. **Add `RankedFeature` and `InsectFeatureView`** (`ReadModel`), and the
   composite assembly reusing the citation lineage walk (§3). Ordering
   contract per §3.
5. **Add `InsectFeatureQuery`** with `findByRankName` (forward composite)
   and `findByFeature` (reverse scan, §6), exposed via the insects query
   facade mirroring `.citations()`. Reverse return = `BehavioralCollection`
   of `InsectRankName` (confirm).
6. **Update existing species feature data** to the typed `InsectFeature`
   form (the `List<String>` → `List<InsectFeature>` content migration in the
   JSON sources).
7. **Behavioral contract tests** mirroring the citation view's tests:
   lineage composition, ordering, provenance tagging, reverse-scan
   correctness, normalization equality.

---

## 9. Open questions

1. **§5 fork — which ranks are entities.** The one decision that gates
   authoring. Confirm `InsectOrder` / `InsectFamily` / `InsectGenus` entity
   status in the tree.
2. **Lineage-walk sharing.** Is the traversal `InsectCitationView` uses
   already a shared helper, or inlined? If inlined, extract so both views
   share it (§3).
3. **Query facade method name.** Confirm `.features()` mirrors however
   `.citations()` is exposed on the insects query facade.
4. **Reverse-query return type.** `BehavioralCollection<InsectRankName>`
   (minimal) vs richer resolved views (§6).
5. **Normalization rules.** Exact canonicalization in `InsectFeature.of()`
   — case-fold + trim is the floor; decide on hyphen/space and
   singular/plural handling. (More aggressive normalization improves
   reverse-query recall but risks merging distinct features — keep it
   conservative; the homology caveat in §6 means over-merging is worse than
   under-merging.)

---

## 10. Implementer conventions checklist

- `InsectFeature` = `NamedValue<String>`, **open** value space, normalize
  in `@JsonCreator of(...)`. Never an enum / closed registry (§7.1).
- `IdentificationFeatures` = top-level `ValueObject` (no longer nested in
  `InsectSpecies`); `List<InsectFeature>`; ordering conspicuous→diagnostic.
- `RankedFeature` = `ValueObject`; `InsectFeatureView` = `ReadModel`.
  Mirror `RankedCitation` / `InsectCitationView` precisely.
- Features are **intrinsic** — never an `EntityRef`, association, or shared
  `Feature` entity (§7.2). Share the lineage *walk* with citations, not the
  type.
- No Spring in api; `@JsonCreator` on `NamedValue.of()` / `NamedEntity`
  factories only, not on records.
- Multi-result query returns = `BehavioralCollection` subclasses.
- `findByFeature` is a **scan**, labeled a browse aid (form, not homology);
  no inverted index (§7.3).
- Option B catalog (if chosen) loads via `*RepositoryMock`, not a Spring
  `@Profile` adapter.
- When unsure whether a type/method/package exists: **say so, do not
  invent** (framework primary rule). Especially the §5 entity-status fact —
  do not assume it.
