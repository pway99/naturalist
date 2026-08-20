# RankAncestry Kernel Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extract insects' cross-rank ancestry resolution (`InsectAncestryResolver`) into a generic, rank-agnostic `RankAncestry` helper in `kernels/taxonomy`, and refactor insects to use it — a behavior-preserving change that makes the traversal reusable by every organism domain.

**Architecture:** `RankAncestry` (taxonomy) owns the generic walk (`ancestry`) and the provenance flat-map (`inherited`), both generic over `R extends RankName` with a domain-supplied `parentOf` seam. The kernel names no taxonomic rung — each domain's wrapper switches over its own sealed permits (insects 5, plants 4). Insects' `InsectAncestryResolver` becomes the thin wrapper (owns the FK-chain `parentOf`, delegates the walk to the kernel, and exposes an `inherited` convenience); its citation query adopts `inherited`; its feature query keeps the walk (intrinsic provenance).

**Tech Stack:** Java 21 (records, sealed interfaces, generics, pattern-matching switch), Maven multi-module, JUnit 5 + AssertJ.

## Global Constraints

- **Behavior-preserving refactor.** The insects feature/citation query outputs must be unchanged; the existing insects tests are the proof and must stay green.
- **Kernel is rank-agnostic.** `RankAncestry` references no rung (order/family/genus/species/subspecies) and no `LinealRank` arithmetic — the chain is data-driven through `parentOf`. Each domain's `parentOf` switches over exactly its own permits.
- **Kernel signature changes need a clean install.** After editing `kernels/taxonomy`, run `mvn clean install` (not incremental) so downstream resolves fresh classes.
- **Claude may run Maven** (scoped `-pl` during a task; full `mvn clean install` at the insects-refactor gate).
- **`InsectAncestryResolver` stays** as the insects wrapper (the single place the insect FK chain is encoded) — do not delete it.

---

### Task 1: `RankAncestry` in the taxonomy kernel

**Files:**
- Create: `kernels/taxonomy/src/main/java/com/naturalist/taxonomy/RankAncestry.java`
- Test: `kernels/taxonomy/src/test/java/com/naturalist/taxonomy/RankAncestryTest.java`

**Interfaces:**
- Produces:
  - `static <R extends RankName> List<R> RankAncestry.ancestry(R subject, Function<R, Optional<R>> parentOf)` — subject first, ancestors ascending; stops when `parentOf` returns empty; cycle-guarded.
  - `static <R extends RankName, A> List<AtRank<R, A>> RankAncestry.inherited(R subject, Function<R, Optional<R>> parentOf, Function<R, List<A>> attributesAt)` — attributes gathered across the ancestry (ancestry order), each tagged with its source rank.
  - `record AtRank<R extends RankName, A>(A value, R sourceRank)`.

- [ ] **Step 1: Write the failing test**

```java
// kernels/taxonomy/src/test/java/com/naturalist/taxonomy/RankAncestryTest.java
package com.naturalist.taxonomy;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class RankAncestryTest {

    /** A fake rank name with a slug; equality by value (record) so the cycle guard works. */
    record Rank(String value) implements RankName {
        @Override public LinealRank rank() { return LinealRank.GENUS; } // unused here
    }

    // species -> genus -> family; family has no parent (top of this fake chain)
    private static final Map<Rank, Rank> PARENT = Map.of(
            new Rank("empoasca"), new Rank("cicadellidae"),
            new Rank("cicadellidae"), new Rank("hemiptera"));

    private static Optional<Rank> parentOf(Rank r) { return Optional.ofNullable(PARENT.get(r)); }

    @Test
    void ancestryIsSubjectFirstThenAncestorsAscending() {
        List<Rank> chain = RankAncestry.ancestry(new Rank("empoasca"), RankAncestryTest::parentOf);
        assertThat(chain).containsExactly(
                new Rank("empoasca"), new Rank("cicadellidae"), new Rank("hemiptera"));
    }

    @Test
    void ancestryStopsAtAMissingLink() {
        // "unknown" has no parent mapping -> chain is just itself
        assertThat(RankAncestry.ancestry(new Rank("unknown"), RankAncestryTest::parentOf))
                .containsExactly(new Rank("unknown"));
    }

    @Test
    void ancestryIsCycleGuarded() {
        // a -> b -> a would loop forever without the guard
        Map<Rank, Rank> cyclic = Map.of(new Rank("a"), new Rank("b"), new Rank("b"), new Rank("a"));
        List<Rank> chain = RankAncestry.ancestry(new Rank("a"), r -> Optional.ofNullable(cyclic.get(r)));
        assertThat(chain).containsExactly(new Rank("a"), new Rank("b")); // stops before repeating "a"
    }

    @Test
    void inheritedTagsEachAttributeWithItsSourceRankInAncestryOrder() {
        // attributes: genus "empoasca" has [G1]; family "cicadellidae" has [F1, F2]
        Map<Rank, List<String>> attrs = Map.of(
                new Rank("empoasca"), List.of("G1"),
                new Rank("cicadellidae"), List.of("F1", "F2"));
        List<RankAncestry.AtRank<Rank, String>> tagged = RankAncestry.inherited(
                new Rank("empoasca"), RankAncestryTest::parentOf,
                r -> attrs.getOrDefault(r, List.of()));
        assertThat(tagged).containsExactly(
                new RankAncestry.AtRank<>("G1", new Rank("empoasca")),
                new RankAncestry.AtRank<>("F1", new Rank("cicadellidae")),
                new RankAncestry.AtRank<>("F2", new Rank("cicadellidae")));
        // hemiptera contributed nothing and simply doesn't appear
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `mvn -q -pl kernels/taxonomy test`
Expected: FAIL — `RankAncestry` does not exist.

- [ ] **Step 3: Implement `RankAncestry`**

```java
// kernels/taxonomy/src/main/java/com/naturalist/taxonomy/RankAncestry.java
package com.naturalist.taxonomy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * Generic, rank-agnostic cross-rank ancestry resolution (organism-domain blueprint C2).
 * The walk is data-driven through a domain-supplied {@code parentOf}; this class names
 * no taxonomic rung and does no {@link LinealRank} arithmetic, so each domain's ladder
 * (four rungs, five, or otherwise — with or without subspecies) is expressed entirely
 * by its own {@code parentOf} over its own sealed rank names.
 */
public final class RankAncestry {

    private RankAncestry() {}

    /**
     * The ancestry chain from {@code subject} upward: subject first, then ancestors in
     * ascending order. Stops when {@code parentOf} yields empty (top rank, or a missing
     * link — the chain ends at the gap). Cycle-guarded: a rank that reappears terminates
     * the walk rather than looping.
     */
    public static <R extends RankName> List<R> ancestry(R subject, Function<R, Optional<R>> parentOf) {
        List<R> chain = new ArrayList<>();
        Set<R> seen = new HashSet<>();
        R current = subject;
        while (current != null && seen.add(current)) {
            chain.add(current);
            current = parentOf.apply(current).orElse(null);
        }
        return List.copyOf(chain);
    }

    /**
     * Attributes attached across the ancestry, each tagged with the rank it was attached
     * at — the lineage composite. Returned in ancestry order (subject first); a consumer
     * that wants ancestor-first reverses the result. Ranks contributing no attributes
     * simply do not appear.
     */
    public static <R extends RankName, A> List<AtRank<R, A>> inherited(
            R subject, Function<R, Optional<R>> parentOf, Function<R, List<A>> attributesAt) {
        List<AtRank<R, A>> result = new ArrayList<>();
        for (R rank : ancestry(subject, parentOf)) {
            for (A value : attributesAt.apply(rank)) {
                result.add(new AtRank<>(value, rank));
            }
        }
        return List.copyOf(result);
    }

    /** An attribute tagged with the ancestry rank it was attached at. */
    public record AtRank<R extends RankName, A>(A value, R sourceRank) {}
}
```

- [ ] **Step 4: Run to verify green**

Run: `mvn -q -pl kernels/taxonomy test`
Expected: PASS (all four tests).

- [ ] **Step 5: Commit**

```bash
git add kernels/taxonomy/src/main/java/com/naturalist/taxonomy/RankAncestry.java kernels/taxonomy/src/test/java/com/naturalist/taxonomy/RankAncestryTest.java
git commit -m "feat(taxonomy): RankAncestry — generic cross-rank ancestry + provenance resolution"
```

- [ ] **Step 6: Clean-install the kernel** so the insects refactor resolves the new class.

Run: `mvn clean install -q -pl kernels/taxonomy -am`
Expected: BUILD SUCCESS.

---

### Task 2: Refactor insects onto `RankAncestry`

Behavior-preserving. `InsectAncestryResolver` stays but delegates to the kernel and gains an `inherited` convenience; the citation query adopts `inherited`; the feature query is unchanged (it calls `resolveAncestry`, which now delegates to the kernel).

**Files:**
- Modify: `domains/insects/insects-core/src/main/java/com/naturalist/insects/InsectAncestryResolver.java`
- Modify: `domains/insects/insects-core/src/main/java/com/naturalist/insects/InsectCitationQueryImpl.java`

**Interfaces:**
- Consumes: `RankAncestry.ancestry`, `RankAncestry.inherited`, `RankAncestry.AtRank` (Task 1).
- Produces: `InsectAncestryResolver.resolveAncestry(InsectRankName) -> List<InsectRankName>` (unchanged signature) and new `<A> List<RankAncestry.AtRank<InsectRankName, A>> InsectAncestryResolver.inherited(InsectRankName subject, Function<InsectRankName, List<A>> attributesAt)`.

- [ ] **Step 1: Refactor `InsectAncestryResolver` to delegate to the kernel + add `inherited`**

Replace the class body so `parentOf` is a private switch and both public methods delegate:

```java
package com.naturalist.insects;

import com.naturalist.taxonomy.RankAncestry;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * The insects wrapper over {@link RankAncestry}: the single place the insect FK chain
 * (species→genus→family→order) is encoded, as {@link #parentOf}. Shared by
 * {@link InsectCitationQueryImpl} and {@link InsectFeatureQueryImpl}.
 */
class InsectAncestryResolver {

    private final InsectQuery.SpeciesQuery speciesQuery;
    private final InsectQuery.GenusQuery genusQuery;
    private final InsectQuery.FamilyQuery familyQuery;

    InsectAncestryResolver(InsectQuery.SpeciesQuery speciesQuery,
                           InsectQuery.GenusQuery genusQuery,
                           InsectQuery.FamilyQuery familyQuery) {
        this.speciesQuery = speciesQuery;
        this.genusQuery = genusQuery;
        this.familyQuery = familyQuery;
    }

    /** subject first, ancestors ascending up to the order. */
    List<InsectRankName> resolveAncestry(InsectRankName rankName) {
        return RankAncestry.ancestry(rankName, this::parentOf);
    }

    /** Attributes across the ancestry tagged with source rank (blueprint C2). */
    <A> List<RankAncestry.AtRank<InsectRankName, A>> inherited(
            InsectRankName subject, Function<InsectRankName, List<A>> attributesAt) {
        return RankAncestry.inherited(subject, this::parentOf, attributesAt);
    }

    /** The insect FK chain: the parent rank of a given rank, empty at the order (or a gap). */
    private Optional<InsectRankName> parentOf(InsectRankName rankName) {
        return switch (rankName) {
            case InsectSpeciesName s -> speciesQuery.getByName(s).map(InsectSpecies::genusName);
            case InsectGenusName g -> genusQuery.getByName(g).map(InsectGenus::familyName);
            case InsectFamilyName f -> familyQuery.getByName(f).map(InsectFamily::orderName);
            case InsectOrderName o -> Optional.empty();
            case InsectSubspeciesName ss -> Optional.empty();
        };
    }
}
```

(Verify the FK accessor names against the entities: `InsectSpecies.genusName()`, `InsectGenus.familyName()`, `InsectFamily.orderName()` — they are what the old resolver called. Adjust only if an accessor differs.)

- [ ] **Step 2: Adopt `inherited` in `InsectCitationQueryImpl`**

In `findByRankName`, replace the manual ancestry loop that builds `pending` with a single `inherited` call. Old block:

```java
        List<InsectRankName> ancestry = ancestryResolver.resolveAncestry(rankName);
        List<PendingCitation> pending = new ArrayList<>();

        for (InsectRankName rank : ancestry) {
            EntityRef ref = new EntityRef(INSECTS, (EntityName) rank);
            for (CitationAssociation a : citationAssociationQuery.findBySubject(ref).stream().toList()) {
                pending.add(new PendingCitation(a.citationName(), rank, a.note()));
            }
        }
```

New block:

```java
        List<PendingCitation> pending = ancestryResolver.inherited(rankName, rank ->
                        citationAssociationQuery.findBySubject(new EntityRef(INSECTS, (EntityName) rank))
                                .stream().toList())
                .stream()
                .map(at -> new PendingCitation(at.value().citationName(), at.sourceRank(), at.value().note()))
                .toList();
```

Keep everything after `pending` (the empty check, `names` set, batch `citationQuery.findByNameSet`, `RankedCitation` assembly, observability) exactly as-is. Add `import com.naturalist.taxonomy.RankAncestry;` only if you reference `AtRank` by name — with the lambda above you do not, so no new import is needed; remove the now-unused `import ...ArrayList;` if it is no longer referenced. Update the class javadoc/imports for anything left unused.

- [ ] **Step 3: Verify the insects refactor green (scoped)**

Run: `mvn -q -pl domains/insects/insects-core -am test`
Expected: BUILD SUCCESS — `InsectCitationQueryImplTest` and `InsectFeatureQueryImplTest` (and the resolver's own test, if any) pass unchanged, proving behavior is preserved. If a stale-jar error appears, use `mvn -q -pl domains/insects/insects-core -am clean install`.

- [ ] **Step 4: Verify the whole app green**

Run: `mvn clean install` from the repo root.
Expected: BUILD SUCCESS (the console pages that render feature/citation views exercise the refactored path).

- [ ] **Step 5: Commit**

```bash
git add domains/insects/insects-core
git commit -m "refactor(insects): resolve ancestry via the RankAncestry kernel"
```

---

## Follow-up (not in this plan)

- **Plants & garden adoption.** Give `PlantEcologicalRoleQueryImpl.forPlantName` (and `garden.PlantingQuery.forSubject`) a hierarchy-walking variant via a `PlantAncestryResolver` / garden wrapper over `RankAncestry` — a **behavior change** (exact-rank → inherited) and a separate feature decision.
- **TaxonViews.** The `*TaxonView` rank-page read model is the main consumer of inherited resolution and the planned next extraction; it will build on `RankAncestry`.
