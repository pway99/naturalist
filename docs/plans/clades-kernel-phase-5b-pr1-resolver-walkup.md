# Clades Phase 5b — PR 1: Resolver Walk-Up Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a new `InsectLifeStages.stagesOf(species, genus, family, order)` static overload that resolves life-stage kinds by picking the first non-null `placedIn` walking the Linnaean parent chain (species → genus → family → order), then running the existing clade-DAG traversal from there. Unblocks PR 2's species-detail-page migration.

**Architecture:** Pure additive change. The new method lives next to the existing three single-rank `stagesOf` overloads on the same `InsectLifeStages` final class. Existing overloads stay untouched — they retain their narrow "this entity's own placement" semantics. The new overload reuses the existing private `resolve(@Nullable Clade)` helper for the clade-DAG traversal; the walk-up is a one-line `firstNonNull` over the four `placedIn` accessors. No consumer changes in this PR — wired into the species-detail controller in PR 2.

**Tech Stack:** Java 21, JUnit 5, AssertJ, JSpecify nullness, Maven. No new dependencies. No new files — both modified files already exist.

**Source spec:** [`clades-kernel-phase-5b-life-stage-inline-removal.md`](clades-kernel-phase-5b-life-stage-inline-removal.md) (PR 1 section).

---

## File map

| Action  | Path                                                                                              | Responsibility                                                                                   |
| ------- | ------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------ |
| Modify  | `domains/insects/insects-api/src/main/java/com/naturalist/insects/lifestage/InsectLifeStages.java` | Add the four-argument `stagesOf` overload + a private `firstNonNull(Clade...)` helper. ~25 LOC. |
| Modify  | `domains/insects/insects-api/src/test/java/com/naturalist/insects/lifestage/InsectLifeStagesTest.java` | Add `orderWithPlacedIn(Clade)` helper, `HEMIMETABOLOUS_STAGES` constant, and seven new tests covering precedence + walk-up + nulls + hemimetabolous + empty cases. ~120 LOC. |

No JSON, no new files, no module-pom changes.

---

## Task 1: Test fixtures — order helper + hemimetabolous constant

**Files:**
- Modify: `domains/insects/insects-api/src/test/java/com/naturalist/insects/lifestage/InsectLifeStagesTest.java`

Groundwork only; no new tests in this task. After this task the file still compiles and all existing tests still pass — we are extending the fixture vocabulary.

- [ ] **Step 1: Add `HEMIMETABOLOUS_STAGES` constant**

Add immediately after the existing `HOLOMETABOLOUS_STAGES` constant (~line 31):

```java
private static final List<LifeStageKind> HEMIMETABOLOUS_STAGES = List.of(
        LifeStageKind.EGG,
        LifeStageKind.NYMPH,
        LifeStageKind.ADULT);
```

- [ ] **Step 2: Add `orderWithPlacedIn` helper**

Add at the bottom of the test class, immediately before the existing `private static InsectFamily familyWithPlacedIn(...)` helper. New imports required: `com.naturalist.insects.InsectOrder`, `com.naturalist.taxonomy.TaxonomicOrder`.

```java
private static InsectOrder orderWithPlacedIn(@Nullable Clade placedIn) {
    return new InsectOrder(
            InsectOrderName.of("lepidoptera"),
            TaxonomicOrder.of("Lepidoptera"),
            description(),
            Set.of(),
            placedIn,
            null, null, null, null);
}
```

Add `@Nullable` to the parameter of `familyWithPlacedIn`, `genusWithPlacedIn`, and `speciesWithPlacedIn` for consistency with the new helper (existing tests already pass `null` to them; the annotation makes the contract explicit). Add `import org.jspecify.annotations.Nullable;` if not already present.

- [ ] **Step 3: Verify the test class still compiles**

Run from `domains/insects/insects-api/`: `mvn -pl insects-api compile test-compile -am` is **not** run here — the user runs Maven (per project convention). Confirm by reading: no `@Test` methods reference the new helper yet, so no test-runtime failure is possible. The change is constants + a helper method + an annotation refinement.

- [ ] **Step 4: Commit**

```bash
git add domains/insects/insects-api/src/test/java/com/naturalist/insects/lifestage/InsectLifeStagesTest.java
git commit -m "$(cat <<'EOF'
Add HEMIMETABOLOUS_STAGES + orderWithPlacedIn helper to InsectLifeStagesTest

Groundwork for the Phase 5b PR 1 resolver walk-up tests. No new test
methods yet — fixture vocabulary only.

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>
EOF
)"
```

---

## Task 2: TDD pilot — first failing test for the walk-up overload

**Files:**
- Modify: `domains/insects/insects-api/src/test/java/com/naturalist/insects/lifestage/InsectLifeStagesTest.java`
- Modify: `domains/insects/insects-api/src/main/java/com/naturalist/insects/lifestage/InsectLifeStages.java`

Single test → red → minimal implementation → green. The first test asserts that when only `species.placedIn` is set (genus / family / order all null), the new overload uses the species's placement. This is the simplest case; subsequent tests in Task 3 extend coverage.

- [ ] **Step 1: Add the first failing test**

Append after the existing `stagesOfUnplacedFamilyReturnsEmptyList` test, before the private helpers:

```java
@Test
void stagesOfWalkUpUsesSpeciesPlacementWhenSet() {
    InsectSpecies species = speciesWithPlacedIn(new Papilionidae());

    assertThat(InsectLifeStages.stagesOf(species, null, null, null))
            .containsExactlyElementsOf(HOLOMETABOLOUS_STAGES);
}
```

- [ ] **Step 2: Confirm the test fails to compile**

The user runs the build (`mvn verify` from repo root, or `mvn -pl :insects-api test -am`). The expected failure is compile-time:

> `cannot find symbol: method stagesOf(InsectSpecies,<null>,<null>,<null>) in class InsectLifeStages`

No need to actually invoke Maven from this session — the failure is mechanically inevitable because the method does not exist yet. Ask the user to confirm if uncertain. The TDD discipline here is that we have written the test before the implementation; we are not relying on a run to know it will fail.

- [ ] **Step 3: Implement the new overload**

Append to `InsectLifeStages.java` after the existing `stagesOf(InsectFamily family)` method (around line 58), before the private `resolve(@Nullable Clade)`:

```java
/**
 * Resolves life-stage kinds for a species by inheriting {@code placedIn}
 * up the Linnaean parent chain. Picks the first non-null placement
 * walking species → genus → family → order, then runs the clade-DAG
 * traversal from there.
 *
 * <p>The {@code species} parameter is non-null — it is the subject of the
 * query. The three parent ranks are nullable to accommodate partial
 * inputs (controller short-circuits, test fixtures where the chain is not
 * fully assembled). When the entire chain has no placement, the resolver
 * returns an empty list — the same "no exception path" contract the
 * single-rank overloads honour.
 *
 * <p>Wired into {@code InsectsController.detail(...)} in PR 2 of the
 * Phase 5b slice. Replaces silent empties on species-detail pages whose
 * placement is declared at a higher rank than the species itself.
 *
 * @see #stagesOf(InsectSpecies) for the narrow "this entity's own placement" semantics
 */
public static List<LifeStageKind> stagesOf(
        InsectSpecies species,
        @Nullable InsectGenus genus,
        @Nullable InsectFamily family,
        @Nullable InsectOrder order) {

    Clade placement = firstNonNull(
            species.placedIn(),
            genus  != null ? genus.placedIn()  : null,
            family != null ? family.placedIn() : null,
            order  != null ? order.placedIn()  : null);
    return resolve(placement);
}

private static @Nullable Clade firstNonNull(@Nullable Clade... candidates) {
    for (Clade candidate : candidates) {
        if (candidate != null) {
            return candidate;
        }
    }
    return null;
}
```

- [ ] **Step 4: Confirm the test passes**

User runs `mvn verify` from repo root (per project convention). Expected: `InsectLifeStagesTest.stagesOfWalkUpUsesSpeciesPlacementWhenSet` is green; the existing six tests in the same class still pass; no other test in the project regressed.

If anything in the broader build fails (console / Spring Boot / JTE modules per the user's build-workflow note), inspect those modules — but no consumer references the new overload yet, so a regression would indicate a transitive compile failure (e.g. unused import warning escalated to error). Fix and re-run.

- [ ] **Step 5: Do NOT commit yet**

Hold off until Task 3 adds the remaining coverage. The walk-up implementation lands as one logical change with full test coverage.

---

## Task 3: Add the remaining six test cases

**Files:**
- Modify: `domains/insects/insects-api/src/test/java/com/naturalist/insects/lifestage/InsectLifeStagesTest.java`

Covers precedence (closer parent wins), walk-up from each rung, all-null short-circuit, hemimetabolous resolution, and the species-only-placed case.

- [ ] **Step 1: Add precedence test — genus placement wins over family**

Append after the Task 2 test:

```java
@Test
void stagesOfWalkUpPrefersCloserParentWhenSpeciesUnplaced() {
    InsectSpecies species = speciesWithPlacedIn(null);
    InsectGenus   genus   = genusWithPlacedIn(new Papilionidae());
    InsectFamily  family  = familyWithPlacedIn(null);
    InsectOrder   order   = orderWithPlacedIn(null);

    assertThat(InsectLifeStages.stagesOf(species, genus, family, order))
            .containsExactlyElementsOf(HOLOMETABOLOUS_STAGES);
}
```

- [ ] **Step 2: Add family-rung walk-up test**

```java
@Test
void stagesOfWalkUpUsesFamilyPlacementWhenSpeciesAndGenusUnplaced() {
    InsectSpecies species = speciesWithPlacedIn(null);
    InsectGenus   genus   = genusWithPlacedIn(null);
    InsectFamily  family  = familyWithPlacedIn(new Papilionidae());
    InsectOrder   order   = orderWithPlacedIn(null);

    assertThat(InsectLifeStages.stagesOf(species, genus, family, order))
            .containsExactlyElementsOf(HOLOMETABOLOUS_STAGES);
}
```

- [ ] **Step 3: Add order-rung walk-up test**

```java
@Test
void stagesOfWalkUpUsesOrderPlacementWhenSpeciesGenusFamilyUnplaced() {
    InsectSpecies species = speciesWithPlacedIn(null);
    InsectGenus   genus   = genusWithPlacedIn(null);
    InsectFamily  family  = familyWithPlacedIn(null);
    InsectOrder   order   = orderWithPlacedIn(new Papilionidae());

    assertThat(InsectLifeStages.stagesOf(species, genus, family, order))
            .containsExactlyElementsOf(HOLOMETABOLOUS_STAGES);
}
```

- [ ] **Step 4: Add all-unplaced test (empty contract)**

```java
@Test
void stagesOfWalkUpReturnsEmptyWhenNoRankIsPlaced() {
    InsectSpecies species = speciesWithPlacedIn(null);
    InsectGenus   genus   = genusWithPlacedIn(null);
    InsectFamily  family  = familyWithPlacedIn(null);
    InsectOrder   order   = orderWithPlacedIn(null);

    assertThat(InsectLifeStages.stagesOf(species, genus, family, order)).isEmpty();
}
```

- [ ] **Step 5: Add hemimetabolous walk-up test**

Asserts the resolver returns the right metaboly's stages (not just "any non-empty list"). Imports needed: `com.naturalist.clades.Hemiptera`.

```java
@Test
void stagesOfWalkUpResolvesHemimetabolousFromFamilyPlacement() {
    InsectSpecies species = speciesWithPlacedIn(null);
    InsectGenus   genus   = genusWithPlacedIn(null);
    InsectFamily  family  = familyWithPlacedIn(new Hemiptera());
    InsectOrder   order   = orderWithPlacedIn(null);

    assertThat(InsectLifeStages.stagesOf(species, genus, family, order))
            .containsExactlyElementsOf(HEMIMETABOLOUS_STAGES);
}
```

- [ ] **Step 6: Add nullable-parents test (species placed, parents null)**

This is structurally the same as Task 2's pilot but explicitly asserts the nullable-parameter contract. Kept as a separate test because the docstring distinction matters for future readers.

```java
@Test
void stagesOfWalkUpAcceptsNullParentsWhenSpeciesPlacedIn() {
    InsectSpecies species = speciesWithPlacedIn(new Papilionidae());

    // Identical call shape to Task 2's pilot — kept as a separate test
    // to document the nullable-parents contract independently from the
    // precedence assertion the pilot is responsible for.
    assertThat(InsectLifeStages.stagesOf(species, null, null, null))
            .containsExactlyElementsOf(HOLOMETABOLOUS_STAGES);
}
```

(If you prefer to drop this as duplicative of the pilot, it's a judgement call — the test is cheap and the documentation value is real. Keep unless the reviewer objects.)

- [ ] **Step 7: Add nullable-parents-with-unplaced-species test**

```java
@Test
void stagesOfWalkUpReturnsEmptyWhenSpeciesUnplacedAndParentsNull() {
    InsectSpecies species = speciesWithPlacedIn(null);

    assertThat(InsectLifeStages.stagesOf(species, null, null, null)).isEmpty();
}
```

- [ ] **Step 8: Confirm all tests pass**

User runs `mvn verify` from repo root. Expected: 6 pre-existing tests + 1 Task 2 pilot + 7 Task 3 additions = **14 tests** in `InsectLifeStagesTest` (or 13 if Task 3 step 6's documentation-value duplicate is dropped). Broader build remains green (console / Spring Boot / JTE modules per the build-workflow note).

- [ ] **Step 9: Commit**

```bash
git add domains/insects/insects-api/src/main/java/com/naturalist/insects/lifestage/InsectLifeStages.java \
        domains/insects/insects-api/src/test/java/com/naturalist/insects/lifestage/InsectLifeStagesTest.java
git commit -m "$(cat <<'EOF'
Add InsectLifeStages.stagesOf walk-up overload (Phase 5b PR 1)

Adds stagesOf(species, genus, family, order) that picks the first
non-null placedIn walking the Linnaean parent chain, then traverses
the clade DAG from there. Existing single-rank overloads unchanged.
No consumer wired yet; PR 2 wires the species-detail controller.

Closes the resolver-walk-up scope of:
docs/plans/clades-kernel-phase-5b-life-stage-inline-removal.md

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>
EOF
)"
```

---

## Self-review checklist (run before declaring PR 1 done)

- [ ] **All seven test cases from the spec land as separate `@Test` methods.** Spec lists: species-own-placement wins, walk-up to genus, walk-up to family, walk-up to order, all-unplaced empty, hemimetabolous resolution, null-parents-with-placed-species (+ null-parents-with-unplaced as added safety). Counted: 7 in Task 3 + 1 in Task 2 pilot = 8 if both nullable-parents kept; 7 if duplicate dropped. Acceptable either way.
- [ ] **`InsectLifeStages.stagesOf(species)` (existing single-rank overload) is untouched.** Re-read the file diff before committing Task 3; the existing method must keep its body identical to verify no semantic shift for current callers.
- [ ] **No consumer file is modified.** PR 1 is kernel-only by design; if anything outside the two files in the file-map changed, back it out.
- [ ] **No `@Nullable` import added if it's already present.** Idempotency check on the test file's import block.
- [ ] **The Task 2 commit was held until Task 3.** Per the task ordering, only two commits should appear: the fixture-vocabulary commit (Task 1 step 4) and the walk-up commit (Task 3 step 9).

---

## Out of scope for this PR (in the parent slice)

- Wiring the new overload into any consumer (PR 2's controller change does this).
- Removing the inline `egg / larva / pupa / adult` fields from any rank record (PR 2).
- Write-time clade validation on `LifeStage` insert (PR 3).
- Spec edit to clarify that `InsectOrder` also carries inline `egg / larva / pupa / adult` fields (the parent spec lists only `InsectSpecies` / `InsectGenus` / `InsectFamily`; `InsectOrder` belongs in PR 2's removal list too — out of scope for PR 1 but flagged here so PR 2's plan captures it).
