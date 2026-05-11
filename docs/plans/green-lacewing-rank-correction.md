# Green-Lacewing Rank Correction Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reorganize the under-identified `green-lacewing` catalog record into its correct rank: merge its content into the existing `chrysoperla` `InsectGenus` record, rename its four life-stage records to genus-rank composite slugs (`chrysoperla-egg`, `-larva`, `-pupa`, `-adult`), and delete the `green-lacewing` species-rank record. This is the pilot for the per-organism data reorganization that follows slice 1 of identification roadmap Phase 0; subsequent organisms (the remaining 9 under-identified insects + 5 plants) get their own slices following the same pattern.

**Architecture:** JSON-only data move. No Java code changes — slice 1's API extension (`InsectGenus` gains nullable life-stage components, `LifeStageName.of(InsectGenusName, LifeStageKind)` factory) is the enabling work. Two JSON files mutate:
1. `insect-species.json` — green-lacewing record removed (including its inline life-stage children).
2. `insect-genera.json` — chrysoperla record gains the four life-stage components.
3. `life-stages.json` — four `green-lacewing-*` records renamed to `chrysoperla-*` (the standalone copy used by the `LifeStageRepository`).

Verification: existing test-source tests load the migrated data successfully; new assertion on `chrysoperla` confirms it now carries its life stages; manual console QA confirms /insects no longer shows green-lacewing as a species and (when family/genus views land in a future slice) /insects/genera/chrysoperla shows it with its life stages.

**Tech Stack:** JSON catalog files loaded by Jackson via `TestEntitySource` subclasses. JUnit Jupiter. No Spring annotations on api modules.

**Editorial judgment calls.** Per Pat's reframing (pressure tests guide, don't drive), each per-organism move involves naturalist judgment about what content rightfully lives at the new rank. This plan documents the decisions for green-lacewing:

- **Description:** the existing `chrysoperla` genus description was adapted from `green-lacewing` and is already authoritative at genus rank. Keep chrysoperla's description; drop green-lacewing's (no content loss — they overlap heavily).
- **Common names:** union the sets. Currently chrysoperla has `["Green Lacewing"]`; green-lacewing species has `Set<CommonName>` (likely the same). Verify and union.
- **Life stages:** copy all four inline. Rename composite slugs via the new genus-parent factory.
- **Species-rank fields with no genus-rank slot** (`guilds`, `beneficial`, `sightingNotes`, `identificationFeatures`, `chemicalDefense`, `voltinism`, `habitatProfile`, `habitatRequirements`, `gardenConnections`, `beneficialProfile`, `ecologicalSignificance`): drop. The information is either already in the description prose or is structured species-level claims that have no factual home at genus rank. When *Chrysoperla rufilabris* identification firms (via the identification workflow in Phase 2), a new species record will be created carrying these fields then.
- **`sightingNotes`** in particular is observation-specific ("Single adult observed on white wall surface at night, Spring 2026..."). Pat decides whether to narrate that into a new genus-level `sightingNotes` field (would require slice 1.5 API extension), drop it, or carry it forward to a future species record. Default in this plan: **narrate into the genus description's `university` level** as a single sentence, since that's the most detailed rendering and naturalist observation context is appropriate there.

**Scope boundary.** This slice is the green-lacewing move only. Other organisms get separate slices, each making its own per-organism editorial decisions. Cross-organism rules (e.g., "always drop sightingNotes") are NOT established here — Pat decides anew per record.

---

## File Structure

**Modify:**
- `domains/insects/insects-repository-test/src/main/resources/insects/insect-species.json` — remove the green-lacewing record (and its 4 inline life-stage children).
- `domains/insects/insects-repository-test/src/main/resources/insects/insect-genera.json` — extend the chrysoperla record with `egg` / `larva` / `pupa` / `adult` fields populated from the green-lacewing inline life stages, with composite slugs rewritten to `chrysoperla-*`. Optionally extend the description's `university` field with the sightingNotes narrative.
- `domains/insects/insects-repository-test/src/main/resources/insects/life-stages.json` — rename the four `green-lacewing-*` records to `chrysoperla-*`. Content (phenology, habitat, description, all other fields) unchanged.

**Verify-only:**
- `domains/insects/insects-repository-test/src/test/java/com/naturalist/insects/InsectGenusTestEntitySourceTest.java` — should still pass; chrysoperla now loads with life stages.
- `domains/insects/insects-repository-test/src/test/java/com/naturalist/insects/lifestage/LifeStageTestEntitySourceTest.java` — should still pass with the renamed records.

**Optional new test:**
- `domains/insects/insects-repository-test/src/test/java/com/naturalist/insects/InsectGenusTestEntitySourceTest.java` — add an assertion that `chrysoperla` carries its larva stage with the composite slug `chrysoperla-larva`. (Determined after the data move; only add if it's a clean fit with the existing test class.)

---

### Task 1: Read and inventory the green-lacewing record

**Files:**
- Inspect only — no edits.

- [ ] **Step 1: Identify the exact line range of the green-lacewing record in insect-species.json**

Run: `grep -n '"name": "green-lacewing"' /Users/pat/dev/naturalist/domains/insects/insects-repository-test/src/main/resources/insects/insect-species.json`

Expected: one line number around 2381. The record begins at the line *before* that (an opening `{`).

- [ ] **Step 2: Identify the closing `}` of the record**

Read the file from the green-lacewing line forward until the matching closing `}` — the record's outermost brace. The next record's opening `{` follows; capture the line range exactly.

- [ ] **Step 3: Inventory the record's content**

Note which fields are present:
- Always: `name`, `taxonomy`, `description`, `guilds`, `beneficial`, `sightingNotes`.
- Likely: `egg`, `larva`, `pupa`, `adult` (verify each is present).
- Possibly: `commonNames`, `identificationFeatures`, `chemicalDefense`, `voltinism`, `habitatProfile`, `habitatRequirements`, `gardenConnections`, `beneficialProfile`, `ecologicalSignificance`.

Record which fields are present and which are null. Use this inventory in Tasks 3 and 4.

- [ ] **Step 4: Inventory the four life-stage records' content**

For each of `green-lacewing-egg`, `green-lacewing-larva`, `green-lacewing-pupa`, `green-lacewing-adult`:
- Note the line range within the inline species record.
- Note the line range within `life-stages.json` (the standalone copy).
- These should match in content; verify briefly by spot-checking one field (e.g., the `description.preschool` text).

If the two copies have drifted (inline vs. standalone), flag the discrepancy and stop — that's a data-consistency bug worth fixing before the move proceeds.

---

### Task 2: Read the chrysoperla destination record

**Files:**
- Inspect only.

- [ ] **Step 1: Read the chrysoperla record in insect-genera.json**

Run: `grep -n '"name": "chrysoperla"' /Users/pat/dev/naturalist/domains/insects/insects-repository-test/src/main/resources/insects/insect-genera.json`

Expected: one line number around 49 (per the earlier survey).

- [ ] **Step 2: Read the full record**

Note its current content:
- `name`, `familyName`, `order`, `family`, `genus`, `description` (4-level), `commonNames`.
- No life-stage fields yet (slice 1 enabled them; this slice populates them).

- [ ] **Step 3: Decide the merge**

For each field, decide:
- **`description`:** keep chrysoperla's existing prose. Optionally append green-lacewing's `sightingNotes` text to the `university` level as a single sentence. Pat decides at edit time.
- **`commonNames`:** union with green-lacewing's set. If both are `["Green Lacewing"]`, the union is unchanged; no action needed.
- **`egg` / `larva` / `pupa` / `adult`:** copy the four inline life-stage records from green-lacewing into chrysoperla. The composite-slug `name` field of each rewrites from `green-lacewing-*` to `chrysoperla-*`. Every other field in each life-stage record (phenology, habitat, description, chemistryRole, ...) is copied verbatim.

---

### Task 3: Extend chrysoperla in insect-genera.json

**Files:**
- Modify: `domains/insects/insects-repository-test/src/main/resources/insects/insect-genera.json`

- [ ] **Step 1: Add the four life-stage fields to chrysoperla**

In the chrysoperla record, after the `commonNames` array, append four new fields: `egg`, `larva`, `pupa`, `adult`. The value of each is the corresponding inline life-stage record from green-lacewing (from Task 1 Step 4 inventory), with the `name` field rewritten:

- `egg.name`: `green-lacewing-egg` → `chrysoperla-egg`
- `larva.name`: `green-lacewing-larva` → `chrysoperla-larva`
- `pupa.name`: `green-lacewing-pupa` → `chrysoperla-pupa`
- `adult.name`: `green-lacewing-adult` → `chrysoperla-adult`

All other fields within each life-stage record copy verbatim. JSON formatting: match the existing indentation of the surrounding chrysoperla record (4 spaces per level inside the array).

- [ ] **Step 2: Optional — narrate `sightingNotes` into the description**

If Pat decides the green-lacewing sightingNotes content ("Single adult observed on white wall surface at night, Spring 2026 — attracted to exterior lighting. Natural arrival; not purchased or released. Observation #17 in Oak Vista beneficial insect census...") is worth preserving as genus-rank context, append it as a sentence to chrysoperla's `description.university` field. Otherwise skip — the description as it stands is already rich.

- [ ] **Step 3: Verify JSON validity**

Run: `jq 'length' /Users/pat/dev/naturalist/domains/insects/insects-repository-test/src/main/resources/insects/insect-genera.json`

Expected: `4` (same as before — record count unchanged; chrysoperla just gained fields).

Run: `jq '.[] | select(.name == "chrysoperla") | {name, larva: .larva.name, adult: .adult.name}' /Users/pat/dev/naturalist/domains/insects/insects-repository-test/src/main/resources/insects/insect-genera.json`

Expected output:
```json
{
  "name": "chrysoperla",
  "larva": "chrysoperla-larva",
  "adult": "chrysoperla-adult"
}
```

---

### Task 4: Rename the four standalone life-stage records in life-stages.json

**Files:**
- Modify: `domains/insects/insects-repository-test/src/main/resources/insects/life-stages.json`

- [ ] **Step 1: For each of the four `green-lacewing-*` records, rewrite the `name` field**

The four records are at approximately lines 2064, 2107, 2152, 2196 (per the earlier grep). For each:

- `"name": "green-lacewing-egg"` → `"name": "chrysoperla-egg"`
- `"name": "green-lacewing-larva"` → `"name": "chrysoperla-larva"`
- `"name": "green-lacewing-pupa"` → `"name": "chrysoperla-pupa"`
- `"name": "green-lacewing-adult"` → `"name": "chrysoperla-adult"`

Every other field in each record is preserved verbatim. No content changes — only the composite-slug prefix changes.

- [ ] **Step 2: Verify JSON validity**

Run: `jq 'length' /Users/pat/dev/naturalist/domains/insects/insects-repository-test/src/main/resources/insects/life-stages.json`

Expected: `60` (unchanged).

Run: `jq '[.[] | select(.name | startswith("chrysoperla-")) | .name]' /Users/pat/dev/naturalist/domains/insects/insects-repository-test/src/main/resources/insects/life-stages.json`

Expected output:
```json
[
  "chrysoperla-egg",
  "chrysoperla-larva",
  "chrysoperla-pupa",
  "chrysoperla-adult"
]
```

Run: `jq '[.[] | select(.name | startswith("green-lacewing-")) | .name]' /Users/pat/dev/naturalist/domains/insects/insects-repository-test/src/main/resources/insects/life-stages.json`

Expected output: `[]` (no records left).

---

### Task 5: Remove the green-lacewing record from insect-species.json

**Files:**
- Modify: `domains/insects/insects-repository-test/src/main/resources/insects/insect-species.json`

- [ ] **Step 1: Remove the entire green-lacewing record**

Using the line range identified in Task 1 Step 2, remove the entire record from the opening `{` of the record through the closing `}`, plus the trailing `,` separator (if any) — taking care to preserve valid JSON across the array.

If the green-lacewing record is followed by another record, remove the comma after green-lacewing's closing `}` along with the record itself. If it was the last record before the array's closing `]`, the preceding record's trailing comma (before green-lacewing) is what needs removal.

- [ ] **Step 2: Verify JSON validity**

Run: `jq 'length' /Users/pat/dev/naturalist/domains/insects/insects-repository-test/src/main/resources/insects/insect-species.json`

Expected: `15` (was 16 — green-lacewing removed).

Run: `jq '[.[] | .name] | contains(["green-lacewing"])' /Users/pat/dev/naturalist/domains/insects/insects-repository-test/src/main/resources/insects/insect-species.json`

Expected: `false`.

---

### Task 6: Optional — add a positive assertion to `InsectGenusTestEntitySourceTest`

**Files:**
- Read first: `domains/insects/insects-repository-test/src/test/java/com/naturalist/insects/InsectGenusTestEntitySourceTest.java`
- Possibly modify: same file.

- [ ] **Step 1: Read the existing test**

Open the file. Look at how it asserts on the loaded data — likely a count assertion + spot-checks of specific records' fields.

- [ ] **Step 2: Decide whether to add a chrysoperla life-stages assertion**

Two judgments:
- (a) If the test class is concise and adding one focused assertion fits cleanly, add a test asserting that chrysoperla now carries a larva stage with composite slug `chrysoperla-larva`.
- (b) If the test class is purely about source loading and adding behavior assertions doesn't fit, skip — the implicit test is that `mvn verify` is green across all modules (load succeeds, repository contract tests pass).

Pat decides based on the existing test class shape.

- [ ] **Step 3: If adding, append the test**

```java
    @Test
    void chrysoperlaCarriesLarvaStageWithGenusRankCompositeSlug() {
        InsectGenus chrysoperla = source.findByName(
                InsectGenusName.of("chrysoperla")).orElseThrow();

        assertThat(chrysoperla.larva()).isNotNull();
        assertThat(chrysoperla.larva().name().value())
                .isEqualTo("chrysoperla-larva");
    }
```

> Adjust to match the existing test class's source-access pattern (the `source` reference may have a different name). Read the file to find the canonical pattern.

---

### Task 7: Full-build verification

**Files:**
- None modified — verification step only.

- [ ] **Step 1: Run `mvn verify` from repo root**

Run: `mvn verify`

Expected: BUILD SUCCESS. Watch in particular for:
- `InsectGenusTestEntitySourceTest` — should load chrysoperla with the new life stages.
- `LifeStageTestEntitySourceTest` — should load the four renamed records (`chrysoperla-egg` etc.) without complaint.
- `InsectsCatalogContributionTest` — should still emit correct counts; green-lacewing is no longer a species, so species count drops by 1.
- Any catalog search tests that hit `green-lacewing` as a token — should fail; update them to search `chrysoperla` or remove the obsolete cases.

- [ ] **Step 2: If a test fails on a `green-lacewing` token reference**

Update the test to reference `chrysoperla` instead, or remove the case if it's specifically testing the under-identified-species path (no longer applicable). Re-run `mvn verify`.

- [ ] **Step 3: Manual console QA**

Start the console. Verify:
- `/insects` no longer lists green-lacewing as a species.
- `/insects` lists the remaining 15 species (was 16).
- (If family/genus views exist after a future slice) `/insects/genera/chrysoperla` shows the four life stages.

---

### Task 8: Commit

**Files:**
- All three modified JSON files plus any test updates from Tasks 6–7.

- [ ] **Step 1: Stage and commit**

```bash
git add domains/insects/insects-repository-test/src/main/resources/insects/insect-species.json \
        domains/insects/insects-repository-test/src/main/resources/insects/insect-genera.json \
        domains/insects/insects-repository-test/src/main/resources/insects/life-stages.json
# Add any test file modifications from Tasks 6–7.

git commit -m "$(cat <<'EOF'
Reorganize green-lacewing as chrysoperla genus-rank record

First per-organism data move under identification roadmap Phase 0 slice 2.
The under-identified "green-lacewing" InsectSpecies record carried partial
taxonomy (genus Chrysoperla, no species) — it sat at species rank but was
only family/genus-level confident. Per Pat's reframing, organisms live at
their actual identification rank; the existing chrysoperla InsectGenus
record (from PR-2c) is the right home.

Changes:
- insect-species.json: green-lacewing record removed (16 → 15 records).
- insect-genera.json: chrysoperla record gains egg/larva/pupa/adult life
  stages, composite slugs rewritten chrysoperla-*. Description unchanged
  (already adapted from green-lacewing).
- life-stages.json: four green-lacewing-* records renamed chrysoperla-*.

No Java code changes — slice 1's API extension enabled this move. Pattern
established for the remaining 9 under-identified insect records, each
landing in its own subsequent slice.

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>
EOF
)"
```

---

## Done When

- `mvn verify` is green from the repo root.
- `insect-species.json` has 15 records (was 16); green-lacewing is gone.
- `insect-genera.json` chrysoperla record carries four life-stage components with composite slugs `chrysoperla-{egg,larva,pupa,adult}`.
- `life-stages.json` has the four records renamed `chrysoperla-*`; no `green-lacewing-*` records remain.
- Console manual QA confirms /insects no longer shows green-lacewing.

## Out of Scope (next slices)

- **The other 9 under-identified insect records** — each gets its own slice following this pattern:
  - native-sweat-bee → halictus (genus)
  - grey-mining-bee → andrena (genus)
  - potato-leafhopper → empoasca (genus)
  - tachinid-fly → tachinidae (family)
  - braconid-wasp → braconidae (family)
  - hoverfly → syrphidae (family)
  - ground-beetle → carabidae (family)
  - crane-fly → tipulidae (family)
  - skipper-butterfly → hesperiidae (family)
- **The 5 under-identified plant records** (creeping-thyme, ornamental-passiflora, dianthus, sage, citrus) — analogous, but plants don't have life stages so the moves are simpler.
- **Console family/genus list/detail views** (Phase 0 slice 4+) — rendering the reorganized data.
- **`sightingNotes` on `InsectGenus` / `InsectFamily`** — if Pat decides naturalist observation context is genus/family-rank worthy across many organisms, that's an API extension worth its own slice (after a few per-organism moves establish the pattern).
