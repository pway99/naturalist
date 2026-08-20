# RankAncestry — extracting cross-rank ancestry resolution to the taxonomy kernel

Date: 2026-08-19
Status: **SHIPPED 2026-08-19** (commits `40dcd18b` kernel, `9e7b02dd` insects
refactor; full `mvn clean install` green; whole-branch review SOUND,
behavior-preserving confirmed). Follow-ups remain as noted below (plants/garden
adoption; TaxonViews).

**NEXT EFFORT: brainstorm TaxonViews** (organism-domain blueprint E1 — the sealed
`*TaxonView` rank-page read model, one permit per rank). It is the main consumer of
this ancestry kernel (inherited features/citations/roles rendered on a rank page) and
the natural next "extract insects-proven pattern to a kernel" step; it also pulls
plants' console + plants' ancestry/role adoption along. Start it in a fresh session.

First of the "extract insects-proven organism patterns to kernels before the
other domains re-derive them" efforts (after `OrganismObservation`/`OrganismImage`,
which unified existing duplication; this one extracts a single proven
implementation ahead of the copy).

## 1. Problem

Blueprint C2 ("inherited attributes resolve by walking ancestry, tagged with
provenance") is implemented once, in insects: `InsectAncestryResolver`
(insects-core), shared by `InsectFeatureQueryImpl` and `InsectCitationQueryImpl`
— "the traversal is the reuse; the result types stay distinct."

Two facts make this worth lifting to a kernel now:
- **Insects proved it** but it is domain-local; every organism domain that grows
  a read/view side (features, citations, roles at a rank) will re-derive the same
  walk.
- **Plants already has a latent gap.** `PlantEcologicalRoleQueryImpl.forPlantName`
  does an **exact-rank** `getByPlantName(...)` — a plant species page shows only
  its own ecological roles, never its family's. Same "no hierarchy-walking query"
  gap the blueprint lists (also `garden.PlantingQuery.forSubject`). The kernel
  makes fixing that cheap.

The traversal splits into a generic core and a domain-specific seam:
- **Generic:** repeatedly apply "parent-of" from a subject rank, accumulating the
  chain; optionally gather attributes attached at each rank, tagged with source.
- **Domain-specific:** *which* query fetches a rank entity and *which* FK accessor
  gives its parent (`InsectSpecies::genusName`, `InsectFamily::orderName`, …).

## 2. The kernel — `RankAncestry` (in `kernels/taxonomy`)

Home is `taxonomy`: it is pure `RankName` ancestry, generic over `R extends
RankName`, with no other dependency. A domain supplies the `parentOf` seam.

```java
package com.naturalist.taxonomy;

public final class RankAncestry {
    private RankAncestry() {}

    /**
     * The Linnaean ancestry chain from {@code subject} upward: subject first,
     * then ancestors in ascending order. Stops when {@code parentOf} returns
     * empty (top rank, or a missing link — the same "chain ends at the gap"
     * behavior domain resolvers already have). Cycle-guarded: a rank that
     * reappears (malformed data) terminates the walk rather than looping.
     */
    public static <R extends RankName> List<R> ancestry(
            R subject, Function<R, Optional<R>> parentOf) { … }

    /**
     * Attributes attached across the ancestry, each tagged with the rank it came
     * from — the blueprint-C2 lineage composite. Equivalent to
     * {@code ancestry(subject, parentOf).flatMap(rank ->
     *   attributesAt(rank).map(a -> new AtRank<>(a, rank)))}, returned in
     * ancestry order (subject first; a consumer reverses for top-down).
     */
    public static <R extends RankName, A> List<AtRank<R, A>> inherited(
            R subject, Function<R, Optional<R>> parentOf,
            Function<R, List<A>> attributesAt) { … }

    /** An attribute tagged with the rank in the ancestry it was attached at. */
    public record AtRank<R extends RankName, A>(A value, R sourceRank) {}
}
```

Notes:
- `parentOf` and `attributesAt` are the two seams; both are `Function`s the domain
  builds closing over its queries/repositories. The kernel imports nothing
  domain-specific.
- `inherited` deliberately does **not** own the per-consumer concerns: within-rank
  `ordinal` sorting, batch-resolving the referenced entities
  (`InsectFeature`/`Citation`), and view assembly stay in the domain. Those are
  not shared; only the walk-and-tag is.
- No `LinealRank` arithmetic is needed — the chain is data-driven through
  `parentOf`; `LinealRank` still orders the rungs elsewhere.
- **The kernel is rank-agnostic; the wrapper owns the ladder.** `RankAncestry`
  never names a rung (order/family/genus/species/subspecies). Each domain's
  `parentOf` switches over exactly its own sealed permits — insects has 5 cases
  (incl. `InsectSubspeciesName`), plants 4 (no subspecies). This is precisely why
  `parentOf` + a per-domain wrapper is the seam rather than baking a fixed chain
  into the kernel: a domain is never forced to model a taxon rank it does not
  have, and a future domain with a different set of rungs just writes its own
  wrapper. Honors the blueprint's "the ladder is per-domain."

## 3. Insects refactor (proves reuse, removes internal duplication)

- `InsectAncestryResolver.resolveAncestry(InsectRankName)` becomes a `parentOf`
  switch delegating to `RankAncestry.ancestry`:
  ```java
  Optional<InsectRankName> parentOf(InsectRankName r) {
      return switch (r) {
          case InsectSpeciesName s    -> speciesQuery.getByName(s).map(InsectSpecies::genusName);
          case InsectGenusName g      -> genusQuery.getByName(g).map(InsectGenus::familyName);
          case InsectFamilyName f     -> familyQuery.getByName(f).map(InsectFamily::orderName);
          case InsectOrderName o      -> Optional.empty();
          case InsectSubspeciesName ss -> Optional.empty();
      };
  }
  // resolveAncestry(subject) = RankAncestry.ancestry(subject, this::parentOf)
  ```
  Behavior is identical (a missing entity ends the chain, exactly as today).
- `InsectFeatureQueryImpl` and `InsectCitationQueryImpl` adopt
  `RankAncestry.inherited(...)` for their "loop the ancestry, gather-at-rank, tag
  with source" section (feature assignments keyed by rank → `RankedFeature`;
  citation associations by rank → `RankedCitation`). Keep the existing within-rank
  `ordinal` sort and the batch entity resolution — feed the tagged results into
  the same view assembly. Their existing tests must stay green (behavior-preserving).

`InsectAncestryResolver` may remain as the thin insects wrapper (it still holds
the three rank queries and owns the `parentOf` switch), now built on the kernel —
or its two consumers call the kernel directly with a shared `parentOf`. Prefer
keeping the wrapper: it is the single place the insect FK chain is encoded.

## 4. Out of scope (noted follow-ups)

- **Plants & garden adoption.** `PlantEcologicalRoleQueryImpl.forPlantName` and
  `garden.PlantingQuery.forSubject` stay **exact-rank** — flipping them to
  ancestry-aware resolution is a behavior change and a separate feature decision.
  The kernel simply makes it a small change when wanted. Plants' console is the
  next effort and will want this.
- **TaxonViews.** The `*TaxonView` rank-page read model (blueprint E1) is the main
  consumer of inherited resolution; it is the planned next extraction and will
  build on this kernel.

## 5. Testing

- Kernel `RankAncestryTest` (in `kernels/taxonomy`): a fake `RankName` chain with
  an injected `parentOf` map — assert `ancestry` order (subject first), the
  missing-link stop, and the cycle guard; assert `inherited` produces the right
  `AtRank` provenance tags in ancestry order over a fake `attributesAt`.
- Insects: `InsectFeatureQueryImpl`/`InsectCitationQueryImpl` existing tests
  unchanged and green (the refactor preserves behavior); the resolver's own tests
  (if any) likewise.
- Full `mvn clean install` after the insects refactor.
