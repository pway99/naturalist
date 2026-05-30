# Clades Phase 5b — PR 2: Detail-Page Migration + Inline Drop Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Migrate the species-detail page from reading inline `egg / larva / pupa / adult` value-object fields on `InsectSpecies` to rendering the same content from the `LifeStage` repository via nested collapsible sections (matching the rank-context UX). Drop the inline fields from all four rank records (`InsectSpecies`, `InsectGenus`, `InsectFamily`, `InsectOrder`) and the inline JSON nodes from `insect-species.json`. The standalone `/life-stages` route remains as a print/plate view.

**Architecture:** Four logical commits inside one PR. (1) A throwaway pre-flight drift check verifies `life-stages.json` covers every inline species block — gating safety so deletions can't lose data. (2) Pure refactor: extract the `<dl class="stage-facts">` block from `life-stages.jte` into a shared `stageFacts.jte` partial; both pages render identically. (3) Wire `stages` into the detail controller, rewrite the detail.jte life-stages section as nested `<details class="ancestor-intro">` collapsibles, drop the "View Life Stages →" link, update WebMvc test assertions. (4) Drop the inline fields from all four rank records + the inline JSON in `insect-species.json` + propagate the constructor-shape changes through every fixture / test that constructs these records.

**Tech Stack:** Java 21, JTE templates, Spring Web MVC, JUnit 5, Jackson 2.19.x, Maven. No new dependencies.

**Source spec:** [`clades-kernel-phase-5b-life-stage-inline-removal.md`](clades-kernel-phase-5b-life-stage-inline-removal.md) (PR 2 section).

**Prerequisite:** PR 1 (resolver walk-up) is merged. PR 2 does NOT wire the new four-argument resolver overload into the detail controller — the detail page reads actual catalog entries via `insectLifeStageQuery.lifeStages().forParentName(...)`, which is a different question ("what stage detail exists?") from what the resolver answers ("what stages should exist per the organism's metaboly?"). The resolver is consulted by PR 3's write-time validation and remains available for any future feature that wants to compare cataloged-vs-expected stages.

---

## File map

| Action  | Path                                                                                                              | Responsibility                                                                                                       |
| ------- | ----------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------- |
| Create  | `domains/insects/insects-console/src/main/jte/insects/stageFacts.jte`                                              | New shared partial: renders `<dl class="stage-facts">…</dl>` for one `LifeStage`. ~120 LOC.                          |
| Modify  | `domains/insects/insects-console/src/main/jte/insects/life-stages.jte`                                             | Replace inline `<dl>` block (lines 58–179) with `@template.insects.stageFacts(stage = stage)` call. ~120 LOC down.   |
| Modify  | `domains/insects/insects-console/src/main/jte/insects/detail.jte`                                                  | Drop "View Life Stages →" link (lines 46–50). Replace life-stages `<section>` (lines 109–187) with nested collapsibles using new partial. Add `stages` + `LifeStage` imports. ~80 LOC down + ~30 LOC up. |
| Modify  | `domains/insects/insects-console/src/main/java/com/naturalist/insects/console/InsectsController.java`              | `detail(...)` method gains `stages` list + model attribute. ~5 LOC up.                                               |
| Modify  | `domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectSpecies.java`                              | Remove 4 inline-stage components, invariants, imports, javadoc. ~40 LOC down.                                        |
| Modify  | `domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectGenus.java`                                | Remove 4 inline components + 4 `with*` mutators + invariants + imports. ~40 LOC down.                                |
| Modify  | `domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectFamily.java`                               | Same shape as InsectGenus. ~40 LOC down.                                                                             |
| Modify  | `domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectOrder.java`                                | Same shape. ~40 LOC down.                                                                                            |
| Modify  | `domains/insects/insects-repository-test/src/main/resources/insects/insect-species.json`                           | Drop `"egg" / "larva" / "pupa" / "adult"` nodes from 6 species entries. ~300 LOC down.                               |
| Modify  | Various `*Test.java` files in `insects-api/src/test/` and `insects-repository-test/src/main/java/`                 | Drop the 4 trailing `null` (or stage-record) args from `new InsectSpecies(...)` / `new InsectGenus(...)` / `new InsectFamily(...)` / `new InsectOrder(...)` constructor calls. Drop any test methods asserting inline-stage component values. ~200 LOC net down. |
| Modify  | `domains/insects/insects-console/src/test/java/com/naturalist/insects/console/InsectsControllerWebMvcTest.java`    | Update detail-page assertions: new collapsible markup present, "View Life Stages →" link absent. ~20 LOC change.     |

**Net diff: ~−800 LOC.** Meaningful review surface ≈ 300 lines (new partial + detail.jte rewrite + four record shrinkages + controller addition). The bulk is mechanical compile-fix work.

---

## Task 1: Pre-flight drift check

**Files:**
- Create (throwaway, deleted at end of task): `temp/lifestage-drift-check.py` — one-shot Python script comparing inline species blocks against `life-stages.json` twins.

**Why this is task 1, not a checklist item:** the verification *must* run before any deletion. If a divergence exists (e.g. an inline `larva.feedingStrategy` differs from its `life-stages.json` twin), the deletion would silently lose data. Per project convention scratch files live in `temp/`, not `/tmp/`.

- [ ] **Step 1: Write the drift-check script**

Create `/Users/pat/dev/naturalist/temp/lifestage-drift-check.py`:

```python
#!/usr/bin/env python3
"""Pre-deletion check: every inline egg/larva/pupa/adult block on
insect-species.json must have a structurally-equal twin in
life-stages.json. Run BEFORE PR 2 drops the inline blocks."""

import json
import sys
from pathlib import Path

ROOT = Path('/Users/pat/dev/naturalist/domains/insects/insects-repository-test/src/main/resources/insects')
SPECIES = ROOT / 'insect-species.json'
STAGES = ROOT / 'life-stages.json'

INLINE_KEYS = ('egg', 'larva', 'pupa', 'adult')

def canonical_stage_name(parent_slug: str, kind_key: str) -> str:
    return f'{parent_slug}-{kind_key}'

def main() -> int:
    species = json.loads(SPECIES.read_text())
    stages = {s['name']: s for s in json.loads(STAGES.read_text())}
    missing = []
    diverged = []
    for sp in species:
        sp_name = sp['name']
        for key in INLINE_KEYS:
            if key not in sp:
                continue
            twin_name = canonical_stage_name(sp_name, key)
            twin = stages.get(twin_name)
            if twin is None:
                missing.append(twin_name)
                continue
            # Structural equality on the fields both copies share.
            inline = sp[key]
            for field in inline:
                if field == 'name':
                    continue
                if twin.get(field) != inline.get(field):
                    diverged.append((twin_name, field, inline.get(field), twin.get(field)))
    if missing:
        print(f'MISSING TWINS ({len(missing)}):')
        for n in missing:
            print(f'  {n}')
    if diverged:
        print(f'DIVERGED FIELDS ({len(diverged)}):')
        for n, f, inline_v, twin_v in diverged:
            print(f'  {n}.{f}: inline={inline_v!r} vs twin={twin_v!r}')
    if missing or diverged:
        return 1
    print(f'OK: all inline blocks have matching twins ({sum(1 for sp in species for k in INLINE_KEYS if k in sp)} checked)')
    return 0

if __name__ == '__main__':
    sys.exit(main())
```

- [ ] **Step 2: Run the script**

```bash
python3 /Users/pat/dev/naturalist/temp/lifestage-drift-check.py
```

Expected: `OK: all inline blocks have matching twins (24 checked)` — verified pre-implementation. If output shows MISSING or DIVERGED entries, **STOP**. Surface the output to the controller / user; fix the catalog drift (either repair `life-stages.json` or correct the inline block) BEFORE proceeding to Task 2. The drift fix is a separate commit on its own, not folded into PR 2.

- [ ] **Step 3: Delete the script after passing**

```bash
rm /Users/pat/dev/naturalist/temp/lifestage-drift-check.py
```

The script has served its purpose. The PR description captures the script + its output so reviewers can re-run if needed; no need to keep it in the repo.

- [ ] **Step 4: Do NOT commit**

Nothing in the repository changed. Task 1's value is the verification, not a code change. Task 2 produces the first commit.

---

## Task 2: Extract `stageFacts.jte` partial

**Files:**
- Create: `domains/insects/insects-console/src/main/jte/insects/stageFacts.jte`
- Modify: `domains/insects/insects-console/src/main/jte/insects/life-stages.jte`

Pure refactor — the rendered output of `/life-stages` is byte-identical before and after this task. The partial is created, life-stages.jte invokes it where the inline `<dl>` block used to be. Verified by manual smoke at the end of the task.

- [ ] **Step 1: Create `stageFacts.jte`**

Path: `/Users/pat/dev/naturalist/domains/insects/insects-console/src/main/jte/insects/stageFacts.jte`

Contents (literally lifted from `life-stages.jte:58–179`, wrapped as a partial):

```jte
@import com.naturalist.insects.lifestage.AdultStage
@import com.naturalist.insects.lifestage.EggStage
@import com.naturalist.insects.lifestage.LarvaStage
@import com.naturalist.insects.lifestage.LifeStage
@import com.naturalist.insects.lifestage.PupaStage

@param LifeStage stage

<dl class="stage-facts">
    <dt>Phenology</dt>
    <dd>
        @for(var window : stage.phenology().windows())
            <div>
                @if(window.cohortLabel() != null)
                    <em>${window.cohortLabel()}:</em>
                @endif
                onset ${window.onset().toString()}
                @if(window.peak() != null)
                    &middot; peak ${window.peak().toString()}
                @endif
                &middot; tail ${window.tail().toString()}
            </div>
        @endfor
        @if(stage.phenology().notes() != null)
            <p class="aside">${stage.phenology().notes()}</p>
        @endif
    </dd>

    <dt>Habitat</dt>
    <dd>
        @if(stage.habitat().substrate() != null)
            <div><em>Substrate:</em> ${stage.habitat().substrate()}</div>
        @endif
        @if(stage.habitat().microclimate() != null)
            <div><em>Microclimate:</em> ${stage.habitat().microclimate()}</div>
        @endif
        @if(stage.habitat().spatialNotes() != null)
            <div><em>Spatial notes:</em> ${stage.habitat().spatialNotes()}</div>
        @endif
    </dd>

    @if(stage.chemistryRole() != null)
        <dt>Chemistry role</dt>
        <dd>${stage.chemistryRole().toString()}</dd>
    @endif

    @if(stage instanceof EggStage egg)
        @if(egg.layingPattern() != null)
            <dt>Laying pattern</dt>
            <dd>${egg.layingPattern()}</dd>
        @endif
        @if(egg.colorProgression() != null)
            <dt>Colour progression</dt>
            <dd>${egg.colorProgression()}</dd>
        @endif
        @if(egg.adaptiveSignificance() != null)
            <dt>Adaptive significance</dt>
            <dd>${egg.adaptiveSignificance()}</dd>
        @endif
    @endif

    @if(stage instanceof LarvaStage larva)
        @if(larva.feedingStrategy() != null)
            <dt>Feeding strategy</dt>
            <dd>${larva.feedingStrategy().name()}</dd>
        @endif
        @if(!larva.hostPlants().isEmpty())
            <dt>Host plants</dt>
            <dd>
                @for(var host : larva.hostPlants())
                    <span class="token">${host.value()}</span>
                @endfor
            </dd>
        @endif
        @if(!larva.parasitoidHosts().isEmpty())
            <dt>Parasitoid hosts</dt>
            <dd>
                @for(var host : larva.parasitoidHosts())
                    <span class="token">${host.value()}</span>
                @endfor
            </dd>
        @endif
        @if(larva.instarProgression() != null)
            <dt>Instar progression</dt>
            <dd>${larva.instarProgression()}</dd>
        @endif
        @if(larva.remarkableBehavior() != null)
            <dt>Remarkable behaviour</dt>
            <dd>${larva.remarkableBehavior()}</dd>
        @endif
    @endif

    @if(stage instanceof PupaStage pupa)
        @if(pupa.appearance() != null)
            <dt>Appearance</dt>
            <dd>${pupa.appearance()}</dd>
        @endif
        @if(pupa.diapauseRegulation() != null)
            <dt>Diapause regulation</dt>
            <dd>${pupa.diapauseRegulation().getClass().getSimpleName()}</dd>
        @endif
        @if(pupa.adaptiveSignificance() != null)
            <dt>Adaptive significance</dt>
            <dd>${pupa.adaptiveSignificance()}</dd>
        @endif
    @endif

    @if(stage instanceof AdultStage adult)
        @if(adult.feedingHabit() != null)
            <dt>Feeding habit</dt>
            <dd>${adult.feedingHabit().name()}</dd>
        @endif
        @if(adult.ecologicalRole() != null)
            <dt>Ecological role</dt>
            <dd>${adult.ecologicalRole()}</dd>
        @endif
        @if(adult.lifespan() != null)
            <dt>Lifespan</dt>
            <dd>${adult.lifespan()}</dd>
        @endif
        @if(!adult.nectarSources().isEmpty())
            <dt>Nectar sources</dt>
            <dd>
                @for(var source : adult.nectarSources())
                    <span class="token">${source.value()}</span>
                @endfor
            </dd>
        @endif
    @endif
</dl>
```

- [ ] **Step 2: Replace the inline `<dl>` block in `life-stages.jte`**

Read `/Users/pat/dev/naturalist/domains/insects/insects-console/src/main/jte/insects/life-stages.jte`. Find lines 58–179 (the entire `<dl class="stage-facts">…</dl>` block inside the `@for(var stage : stages)` loop). Replace with a single line:

```jte
                    @template.insects.stageFacts(stage = stage)
```

Maintain the existing 20-space indentation (4 levels × 4 spaces, plus the @ at column 21 per the rest of the file's @-block indentation style).

Remove now-unused imports from life-stages.jte's import block:
- `import com.naturalist.insects.lifestage.LifeStage` — STAYS, still used in `@param`.
- `import com.naturalist.insects.lifestage.EggStage` — REMOVE (only used inside the extracted block).
- `import com.naturalist.insects.lifestage.LarvaStage` — REMOVE.
- `import com.naturalist.insects.lifestage.PupaStage` — REMOVE.
- `import com.naturalist.insects.lifestage.AdultStage` — REMOVE.

The other imports (`InsectFamily`, `InsectOrder`, `InsectSpecies`, `AncestorIntro`, `BreadcrumbSegment`, `LifeStage`, `List`) all remain referenced by the surrounding template.

- [ ] **Step 3: Self-review the diff**

The diff should show:
- `stageFacts.jte`: 100% added.
- `life-stages.jte`: -120 / +1 inside the `@for` block; -4 import lines.

Verify by reading both files end-to-end:
- `stageFacts.jte` has exactly one `<dl>` block, parameterised by `LifeStage stage`.
- `life-stages.jte` still has the outer `<article class="stage-plate">` wrapper around the partial call.

- [ ] **Step 4: Stage the files (do NOT commit)**

```bash
git add domains/insects/insects-console/src/main/jte/insects/stageFacts.jte \
        domains/insects/insects-console/src/main/jte/insects/life-stages.jte
```

- [ ] **Step 5: User verification + commit handoff**

Controller surfaces to user: please run `mvn verify` from repo root (JTE recompilation happens automatically; if anything is wrong the build fails). Then open `/insects/<some-species>/life-stages` in the browser and verify the page renders identically to before the refactor. Then commit with:

```bash
git commit -m "$(cat <<'EOF'
Extract stageFacts.jte partial from life-stages.jte (Phase 5b PR 2)

Pure refactor: per-stage <dl class="stage-facts"> block becomes a
shared partial parameterised by LifeStage. Output is byte-identical
to before. Sets up the detail-page life-stages section in the next
commit to render via the same partial.

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>
EOF
)"
```

---

## Task 3: Wire `stages` into detail controller + rewrite detail.jte life-stages section + update WebMvc test

**Files:**
- Modify: `domains/insects/insects-console/src/main/java/com/naturalist/insects/console/InsectsController.java`
- Modify: `domains/insects/insects-console/src/main/jte/insects/detail.jte`
- Modify: `domains/insects/insects-console/src/test/java/com/naturalist/insects/console/InsectsControllerWebMvcTest.java`

This task switches the species-detail page from reading `species.egg() / larva() / pupa() / adult()` (inline value-object accessors) to rendering from a `List<LifeStage> stages` loaded by the controller. The inline fields are STILL PRESENT on the record after this task — they're just no longer consulted by detail.jte. Task 4 drops them.

- [ ] **Step 1: Add controller imports if missing**

In `InsectsController.java`, verify the following imports are present (most likely already are — the `/life-stages` route uses identical types):
- `java.util.Comparator`
- `java.util.List`
- `com.naturalist.insects.lifestage.LifeStage`

Add any that are missing.

- [ ] **Step 2: Add `stages` to `detail` method**

In `InsectsController.detail(...)` (around lines 408–438), after the existing rank-chain loads (lines 415–417) and before the existing `model.addAttribute("species", s);` call (line 420), insert:

```java
        List<LifeStage> stages = insectLifeStageQuery.lifeStages().forParentName(speciesName).stream()
                .sorted(Comparator.comparingInt(stage -> stage.kind().ordinal()))
                .toList();
```

And after the existing `model.addAttribute("order", order);` call (line 423), insert:

```java
        model.addAttribute("stages", stages);
```

`insectLifeStageQuery` is already an injected field (declared line 51, set line 61). No new constructor wiring.

- [ ] **Step 3: Rewrite `detail.jte` life-stages section**

In `/Users/pat/dev/naturalist/domains/insects/insects-console/src/main/jte/insects/detail.jte`:

**Add imports** (after the existing `@import` block near top of file):
```jte
@import com.naturalist.insects.lifestage.LifeStage
@import com.naturalist.insects.LifeStageKind
```

**Add `@param`** (after the existing `@param List<InsectImage> images` line):
```jte
@param List<LifeStage> stages = List.of()
```

The default of `List.of()` keeps existing tests / direct template invocations that don't pass `stages` from breaking (defensive — controller always passes a list now, but the default protects against forgotten test fixtures).

**Drop the "View Life Stages →" link** at lines 46–50 — delete the entire `<p class="page-actions">…</p>` block.

**Replace the existing life-stages `<section>`** (lines 109–187 — the `@if(species.egg() != null || species.larva() != null || species.pupa() != null || species.adult() != null)` block) with the new collapsible-based shape. Insert at the same position:

```jte
    @if(!stages.isEmpty())
        <details class="ancestor-intro" data-storage-key="life-stages">
            <summary><h2>Life Stages</h2></summary>
            @for(var stage : stages)
                <details class="ancestor-intro stage-detail" data-storage-key="life-stage-${stage.kind().name().toLowerCase()}">
                    <summary><h3>${stage.kind().name().charAt(0) + stage.kind().name().substring(1).toLowerCase()}</h3></summary>
                    <p class="lead">${stage.description().secondary()}</p>
                    @template.insects.stageFacts(stage = stage)
                </details>
            @endfor
        </details>
    @endif
```

**Note on the `<h3>` title expression.** The `stage.kind().name().charAt(0) + stage.kind().name().substring(1).toLowerCase()` idiom mirrors what `life-stages.jte:43` does today — turns `"EGG"` into `"Egg"`. Keep this consistent; no helper utility is necessary.

- [ ] **Step 4: Update `InsectsControllerWebMvcTest`**

The WebMvc test asserts detail-page contents. Two changes:
- Remove any assertion that the page contains the `"View Life Stages →"` link (or `"/life-stages"` link from the detail page).
- Add an assertion that — for a species with cataloged stages (e.g. `colias-eurytheme` or `battus-philenor`) — the rendered HTML contains a `<details class="ancestor-intro" data-storage-key="life-stages">` element and nested `data-storage-key="life-stage-egg"` (or similar). Use a substring match (`.contains(...)`) rather than DOM parsing — matches existing test conventions in the file.

Read the existing test file first to see the assertion style — it likely uses `MockMvc` + `.andExpect(content().string(containsString(...)))`. Match that.

For a species with NO cataloged stages, the new section should be absent — add a negative assertion. If the test data doesn't already include such a species in the rank-chain it tests, add a separate test method or skip this assertion (the @if guard in the template handles the empty-list case, low regression risk).

- [ ] **Step 5: Stage all three files (do NOT commit)**

```bash
git add domains/insects/insects-console/src/main/java/com/naturalist/insects/console/InsectsController.java \
        domains/insects/insects-console/src/main/jte/insects/detail.jte \
        domains/insects/insects-console/src/test/java/com/naturalist/insects/console/InsectsControllerWebMvcTest.java
```

- [ ] **Step 6: User verification + commit handoff**

Controller surfaces to user: please run `mvn verify` from repo root. Open `/insects/battus-philenor` in the browser (Battus is the species most likely to have a full set of cataloged life stages). Verify:
- The "View Life Stages →" link no longer appears at the top of the page.
- A new "Life Stages" section appears with the same `ancestor-intro` collapsible styling as Class / Order / Family / Genus.
- Expanding the outer "Life Stages" shows nested per-stage collapsibles (Egg / Larva / Pupa / Adult, in metaboly order).
- Expanding a nested per-stage block shows the lead description + the stage facts (`<dl>` block).
- Closing the outer "Life Stages" collapsible and reloading the page preserves the closed state (localStorage).
- Navigating to a different species preserves the open/closed state for Life Stages (matching how Order / Family / Genus persist today).
- The `/insects/battus-philenor/life-stages` standalone route still renders the plate view unchanged.

Commit with:

```bash
git commit -m "$(cat <<'EOF'
Migrate species detail page to collapsible life-stages section (Phase 5b PR 2)

Adds a `stages: List<LifeStage>` model attribute loaded by the
controller from InsectLifeStageQuery and replaces the inline-field
reads in detail.jte with nested <details class="ancestor-intro">
collapsibles matching the existing rank-context UX. The
stageFacts.jte partial (from the prior commit) renders the per-stage
<dl> block. The "View Life Stages →" link to the standalone plate
route is removed; the standalone /life-stages route stays accessible
for direct navigation.

Inline egg/larva/pupa/adult fields on InsectSpecies are still present
but no longer consulted by the detail page; they are dropped in the
next commit.

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>
EOF
)"
```

---

## Task 4: Drop inline fields from all four rank records + fix test fixtures + drop inline JSON

**Files:**
- Modify: `domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectSpecies.java`
- Modify: `domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectGenus.java`
- Modify: `domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectFamily.java`
- Modify: `domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectOrder.java`
- Modify: `domains/insects/insects-repository-test/src/main/resources/insects/insect-species.json`
- Modify: every `*Test.java` file that constructs one of the four rank records via `new InsectSpecies(...)` / `new InsectGenus(...)` / `new InsectFamily(...)` / `new InsectOrder(...)` — drops the four trailing arguments.

This is the destructive change. Records lose 4 components; every constructor call site must shrink by 4 args. Java's compiler is the safety net — every missed site is a compile error.

The order below matters: change the records first, then iterate compile errors. **Do not attempt to enumerate fixture sites upfront** — let the compiler tell you which to fix. Iterative fix-and-recompile is faster than upfront grep.

- [ ] **Step 1: Shrink `InsectSpecies.java`**

Remove from the record components list (around lines 112–115):
- `@Nullable EggStage egg,`
- `@Nullable LarvaStage larva,`
- `@Nullable PupaStage pupa,`
- `@Nullable AdultStage adult`

Note the trailing comma on the LAST surviving component — `egg / larva / pupa / adult` are at the end, so the component immediately before them (likely `voltinism` or similar — verify by reading the file) needs no comma change.

Remove from `invariants()` (around lines 152–155):
- `.namedEntityOrNull(egg, "egg")`
- `.namedEntityOrNull(larva, "larva")`
- `.namedEntityOrNull(pupa, "pupa")`
- `.namedEntityOrNull(adult, "adult")`

Remove imports (around lines 10–13):
- `import com.naturalist.insects.lifestage.AdultStage;`
- `import com.naturalist.insects.lifestage.EggStage;`
- `import com.naturalist.insects.lifestage.LarvaStage;`
- `import com.naturalist.insects.lifestage.PupaStage;`

Update the javadoc (around lines 52–75): remove the four paragraphs describing the inline life-stage components. Replace with a single sentence pointing readers to the `LifeStage` repository: *"Per-stage data (descriptions, phenology, habitat, kind-specific fields) lives on the `LifeStage` records in the `lifestage` sub-package, keyed by `(speciesName, stageKind)` and queried via `InsectLifeStageQuery`."*

Verify any `with*` mutators that referenced the dropped components. If `InsectSpecies` has `withEgg / withLarva / withPupa / withAdult` mutators (like `InsectGenus` does), delete them too.

- [ ] **Step 2: Shrink `InsectGenus.java`**

Same shape (components ~`:44–47`, mutators `withEgg / withLarva / withPupa / withAdult` at `:55–73`, invariants at `:84–87`, imports at `:7–10`). Delete all four `with*` mutators in full.

- [ ] **Step 3: Shrink `InsectFamily.java`**

Same shape (components `:44–47`, mutators `:57–77`, invariants `:88–91`, imports `:7–10`). Delete the four `with*` mutators.

- [ ] **Step 4: Shrink `InsectOrder.java`**

Same shape (components `:39–42`, mutators `:50–68`, invariants `:77–80`, imports `:7–10`). Delete the four `with*` mutators.

- [ ] **Step 5: Drop inline nodes from `insect-species.json`**

For each of the 6 species with inline blocks — `hippodamia-convergens`, `blattella-vaga`, `xylocopa-varipuncta`, `vanessa-cardui`, `battus-philenor`, `colias-eurytheme` — remove the `"egg" / "larva" / "pupa" / "adult"` top-level keys and their entire nested objects. Use a JSON-aware approach (not raw `sed`); a small Python script or careful manual edit. Round-trip with `ensure_ascii=False` to preserve em-dashes and other UTF-8 (per project memory `feedback_json_dump_unicode`).

Suggested Python:

```python
import json
from pathlib import Path

P = Path('/Users/pat/dev/naturalist/domains/insects/insects-repository-test/src/main/resources/insects/insect-species.json')
data = json.loads(P.read_text())
INLINE = ('egg', 'larva', 'pupa', 'adult')
for sp in data:
    for k in INLINE:
        sp.pop(k, None)
P.write_text(json.dumps(data, indent=2, ensure_ascii=False) + '\n')
```

Verify the diff is ONLY removed `"egg" / "larva" / "pupa" / "adult"` keys — no reordering, no quote changes. If `json.dumps` produces different formatting (e.g. 4-space indent when the file used 2), match the file's existing indentation by reading the original and using its style.

- [ ] **Step 6: Iterate compile errors**

At this point the test suite will fail to compile in many places. Resist the temptation to grep for `new InsectSpecies(...)` — instead, let the compiler list every broken call site, then fix them one file at a time.

For each compile error of the form *"constructor InsectSpecies cannot be applied to given types; ... required 17, found 21"* (or similar — the exact component counts depend on the records' final shape):

- Open the file at the indicated line.
- Find the `new InsectSpecies(...)` / `new InsectGenus(...)` / `new InsectFamily(...)` / `new InsectOrder(...)` constructor call.
- Delete the last four arguments (the `egg, larva, pupa, adult` slot — usually `null, null, null, null` in test fixtures, but possibly real `new EggStage(...)` / `new LarvaStage(...)` etc. in detail-heavy tests).
- If the deleted arguments include constructed stage records (`new EggStage(...)` etc.), they were testing the inline-field invariants — those test methods may now be redundant. **Do not silently delete them** — instead, mark the test method with a `// TODO PR 2 — inline stage removed; assess whether this test still has value` comment and surface to the controller / user at end of task.

Expected fixture sites (from prior exploration; non-exhaustive, the compiler enumerates the rest):
- `InsectLifeStagesTest.java` — the four `speciesWithPlacedIn / genusWithPlacedIn / familyWithPlacedIn / orderWithPlacedIn` helpers each pass `null, null, null, null` as the trailing args. Remove those four nulls per helper.
- `SpeciesRepositoryTest`, `GenusRepositoryTest`, `FamilyRepositoryTest`, `OrderRepositoryTest` (or their concrete mock implementations) — `newEntity / modifiedEntity / ghostEntity` hooks per the ADR-002 contract. Remove trailing stage args.
- `SpeciesQueryImplTest`, `GenusQueryImplTest`, etc. — same shape.
- `InsectSpeciesTest`, `InsectGenusTest`, `InsectFamilyTest`, `InsectOrderTest` — drop any assertions on inline-stage components. Drop any test methods whose entire purpose was asserting the inline-stage invariant (those tests are now invalid; their concern moves to PR 3's write-time validation).
- `InsectAggregateFactoryTest` — should not require changes (aggregate doesn't reference stages directly) but verify.

For each fix, recompile to find the next batch. Repeat until the project compiles.

- [ ] **Step 7: Strip orphaned imports**

After the fixture wave, search for now-unused imports of `EggStage / LarvaStage / PupaStage / AdultStage` in non-test files. The IDE / compiler may report them as warnings; remove. (Test files in `lifestage/` still reference these types for the partial template — that's expected.)

- [ ] **Step 8: Stage all changes**

```bash
git add -A domains/insects
```

Use `-A` here because the change touches many test files and a JSON file; enumerating each path is brittle. Review with `git diff --cached --stat` to confirm only the expected files are staged. If `git status` shows anything OUTSIDE `domains/insects/`, investigate before committing.

- [ ] **Step 9: User verification + commit handoff**

Controller surfaces to user: please run `mvn verify` from repo root. Expected: clean green build. Then open the same species detail pages used in Task 3's smoke test (especially `battus-philenor`, `colias-eurytheme`, `hippodamia-convergens`) and verify the Life Stages section still renders correctly — the inline drop should be invisible at the UI layer since Task 3 already migrated the consumer.

If any tests were marked `// TODO PR 2` in Step 6, surface the file:line list so the user can decide per-test whether to delete or rework.

Commit with:

```bash
git commit -m "$(cat <<'EOF'
Drop inline EggStage/LarvaStage/PupaStage/AdultStage fields from rank records (Phase 5b PR 2)

Removes the four inline life-stage components (plus their with* mutators,
invariants, and imports) from InsectSpecies, InsectGenus, InsectFamily,
and InsectOrder. Drops the matching JSON nodes from insect-species.json
(the only catalog file that populated them — 24 keys across 6 species).
All per-stage data continues to live on the LifeStage records keyed by
(parentSlug, stageKind), queried via InsectLifeStageQuery.

Detail page already migrated in the prior commit; standalone
/life-stages route untouched. Closes the inline-removal scope of:
docs/plans/clades-kernel-phase-5b-life-stage-inline-removal.md

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>
EOF
)"
```

---

## Task 5: Manual smoke + PR description

**Files:**
- None (manual verification only)

After all four commits land, do an end-to-end pass before pushing:

- [ ] **Step 1: Browser smoke across multiple species**

Open the console (`mvn spring-boot:run` from `apps/management-console/`, or however the user normally launches). Visit:
- `/insects/battus-philenor` — should have full life stages section.
- `/insects/colias-eurytheme` — should have full life stages section.
- `/insects/hippodamia-convergens` — should have full life stages section.
- `/insects/xylocopa-varipuncta` — should have life stages section.
- Any species that had NO inline stages and is not in `life-stages.json` — should NOT show a Life Stages section (the @if guard).
- `/insects/<any-species>/life-stages` — standalone plate view should render identically to before PR 2.

Verify across two browser tabs that localStorage open/closed state for the outer "Life Stages" collapsible persists across navigation, matching the rank-context behavior.

- [ ] **Step 2: PR description**

When ready to open the PR (or update the existing PR), include:
- The drift-check script + its `OK` output from Task 1.
- Per-species before/after screenshots of one species detail page (optional but useful for review).
- A line confirming `mvn verify` passes locally.
- A note linking back to the slice plan and noting which scope remains for PR 3 (write-time validation).

- [ ] **Step 3: Push**

Per project convention the user runs `git push`. Surface the readiness; do not push from this session.

---

## Self-review checklist (run before declaring PR 2 done)

- [ ] **All four rank records are shrunk.** Verify `InsectSpecies / InsectGenus / InsectFamily / InsectOrder` no longer carry `egg / larva / pupa / adult` components, mutators, invariants, or imports.
- [ ] **`insect-species.json` is the only JSON file changed.** `git diff --stat` on resources should show only that one file.
- [ ] **`detail.jte` no longer references `species.egg() / .larva() / .pupa() / .adult()`.** Grep the file for those accessors; expected zero matches.
- [ ] **`detail.jte` no longer renders the "View Life Stages →" link.** Grep for `"View Life Stages"` and `"life-stages"` (excluding the `data-storage-key="life-stages"` attribute) — expected to find only the data-attribute occurrence.
- [ ] **`life-stages.jte` renders identically to before the partial extraction.** The standalone plate view at `/<species>/life-stages` must look unchanged.
- [ ] **`InsectLifeStageQuery` is unchanged.** PR 2 does not modify the query interface or its adapter.
- [ ] **PR 1's resolver overload is unchanged.** Search for any incidental edits to `InsectLifeStages.java` — there should be none.
- [ ] **No new test fixtures added.** All test changes are constructor-shape compile-fixes or removed inline-stage assertions; nothing new.
- [ ] **`InsectsControllerWebMvcTest` has updated assertions for the new collapsible markup and the dropped link.**

---

## Out of scope for this PR (deferred to PR 3 or beyond)

- Write-time clade validation on `LifeStage` inserts — PR 3.
- Wiring `InsectLifeStages.stagesOf(species, genus, family, order)` (PR 1's walk-up overload) into any consumer. The detail page reads stages from the repository, which is a different question ("what stage detail exists?") from the resolver's question ("what stages should exist per metaboly?"). The walk-up resolver becomes useful when a future feature compares cataloged-stages-vs-expected-stages.
- Removing the standalone `/life-stages` route. Kept as the print/plate view by explicit decision.
- Removing `ChemicalDefense.protectedStages` from `InsectSpecies`. Separate concern with its own removal criterion.
- CSS adjustments for the new `.stage-detail` nested collapsibles. Verify during manual smoke; raise a separate follow-up commit if the layout needs tweaking.
