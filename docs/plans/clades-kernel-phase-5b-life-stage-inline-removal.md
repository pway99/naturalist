# Phase 5b — Life-Stage Inline Removal & Resolver Walk-Up

A follow-up slice of [`clades-kernel.md`](clades-kernel.md). Closes the residual
work Phase 5 deliberately deferred: removes the inline `egg / larva / pupa /
adult` value-object copies from `InsectSpecies` / `InsectGenus` / `InsectFamily`,
extends the clade resolver to inherit `placedIn` up the Linnaean parent chain,
and adds write-time clade validation to the `LifeStage` repository.

> **Lineage.** Phase 5 of `clades-kernel.md` landed the
> `InsectLifeStages.stagesOf(...)` resolver as an *additive* change — it
> answered "which stages exist" via clade traversal, but kept the inline
> `@Nullable EggStage / LarvaStage / PupaStage / AdultStage` fields on the
> three rank records as deliberate duplicates of the entries already in
> `life-stages.json`. PL-1's resolution note in
> [`parking-lot-resolved.md`](../notes/parking-lot-resolved.md) explicitly
> said those inline fields *"will be removed per-organism via PL-2."* PL-2
> turned out to be a different concern (per-organism rank corrections) and
> closed 2026-05-24 without touching the inline fields. This plan picks up
> the orphaned removal and bundles it with two related cleanups.

---

## Why this exists

Three concrete problems remain after Phase 5:

1. **The dual home is still live.** Every life-stage record exists twice —
   once inline on the rank record (`species.egg()`, `species.larva()`, …),
   once in the `LifeStage` repository (`hippodamia-convergens-egg` etc., 60
   entries in `life-stages.json`). Edits drift between them silently.
2. **The resolver is silent for ~all species.** Only one species
   (`Battus philenor`) has its own `placedIn`. Every other species's call
   to `InsectLifeStages.stagesOf(species)` returns an empty list — even
   though the species's *family* may be cleanly placed in Holometabola. The
   inline fields are masking this gap on the detail page today.
3. **The console detail page still reads the inline fields directly.** The
   standalone `/life-stages` route already routes through the
   `LifeStageQuery`, but `detail.jte:109–187` reads `species.egg()` etc.
   No other non-test Java caller does.

The clades-kernel plan's stated principle — *"inheritance by traversal, not
by duplication"* — is undermined while these duplicates exist.

---

## Scope

Three PRs, landed in order:

| PR | Name                                                | Scope                                                            |
| -- | --------------------------------------------------- | ---------------------------------------------------------------- |
| 1  | Resolver walk-up                                    | Kernel-only. New `InsectLifeStages.stagesOf(species, genus, family, order)` overload that picks first non-null `placedIn` walking species → genus → family → order, then traverses the clade DAG from there. No consumer changes. |
| 2  | Detail-page migration + inline drop                 | `detail.jte` rewritten with nested `<details class="ancestor-intro">` collapsibles matching the rank-context pattern. Controller adds a `stages` list to the model (rank chain already loaded). Inline `EggStage / LarvaStage / PupaStage / AdultStage` fields removed from **all four** rank records (`InsectSpecies`, `InsectGenus`, `InsectFamily`, `InsectOrder`). Inline JSON nodes removed from `insect-species.json` (the only catalog file that actually populated them — 6 species × 4 stages = 24 nodes). |
| 3  | ~~Write-time clade validation~~                     | **CANCELLED 2026-05-31.** Original plan was to add cross-record clade validation to `LifeStageEntityRepository.insert(...)` — wrong layer, violates `domains/CLAUDE.md`'s rule that *"a repository has exactly four responsibilities ... no logic."* The invariant moves to the new `Insect` aggregate's `invariants()`; see [`insect-aggregate.md`](insect-aggregate.md) Phase 3. Phase 5b's effective scope is now PRs 1 + 2 only. |

---

## PR 1 — Resolver walk-up

**Goal.** Make the resolver answer honestly across the catalog by inheriting
`placedIn` up the Linnaean parent chain.

**Kernel change.** Single new static method on `InsectLifeStages`:

```java
public static List<LifeStageKind> stagesOf(
        InsectSpecies species,
        @Nullable InsectGenus genus,
        @Nullable InsectFamily family,
        @Nullable InsectOrder order);
```

Picks the first non-null `placedIn` walking species → genus → family →
order, then runs the existing `CladeTraversal.findTrait` against it.
Existing single-rank overloads (`stagesOf(species)`, `stagesOf(genus)`,
`stagesOf(family)`) stay — now narrowed to mean *"this entity's own
placement."*

Genus / family / order are nullable to preserve the existing
"no exception path — empty list is a legitimate state" contract for
partially-assembled inputs (controller short-circuits, test fixtures).

`InsectOrder` is in the chain because it is the highest rank carrying
`placedIn` today (6/8 orders placed). `InsectSubspecies` is not — no
entity exists yet (`InsectAggregateFactory:75` returns
`Optional.empty()`); it gets a fifth argument when that entity lands.

**Consumer impact.** None in this PR. Wired in PR 2.

**Tests** (alongside existing `InsectLifeStages` tests in `insects-api`):

- Species with own `placedIn` → species placement wins (Battus philenor →
  `[Egg, Larva, Pupa, Adult]`).
- Species unplaced, genus unplaced, family placed in Holometabola →
  `[Egg, Larva, Pupa, Adult]`.
- Species unplaced, genus placed → genus placement wins (closer parent
  precedence).
- Family placed in a hemimetabolous order → `[Egg, Nymph, Adult]`. Asserts
  walk-up + clade traversal compose.
- All four unplaced → empty list.
- Null parents, species placed → resolves from species.
- Null parents, species unplaced → empty list.

**LOC estimate.** ~30 production + ~80 test.

---

## PR 2 — Detail-page migration + inline drop

The visible UX change plus the dead-code removal.

### Detail page shape

`detail.jte` gains a new section after `ancestorIntros`, before the
four-level Description, in the same `<details class="ancestor-intro">`
family used today for Class / Order / Family / Genus rank context (with
localStorage open/close persistence). One outer collapsible for "Life
Stages", with N nested collapsibles inside — one per stage the organism
has, in the order the metaboly declares.

```
<details class="ancestor-intro" data-storage-key="life-stages">
  <summary><h2>Life Stages</h2></summary>

  <details class="ancestor-intro stage-detail" data-storage-key="life-stage-egg">
    <summary><h3>Egg</h3></summary>
    <!-- Lead description, phenology, habitat, kind-specific fields -->
  </details>
  <details class="ancestor-intro stage-detail" data-storage-key="life-stage-larva">…</details>
  …
</details>
```

The per-stage `<dl class="stage-facts">…</dl>` block (the
phenology / habitat / chemistry-role / `instanceof EggStage egg`
pattern-matching currently at `life-stages.jte:58–179`) is extracted
into a shared partial template `stageFacts.jte`, parameterised by
`LifeStage stage`. Both pages invoke it. Per-page **wrapper markup
stays per page** — `life-stages.jte` keeps `<article
class="stage-plate">` + `<header class="stage-header">` + lead
description; `detail.jte` uses `<details class="ancestor-intro
stage-detail">` + `<summary>` + lead description. Only the facts block
is shared, because the wrapper concerns differ (plate view vs.
inline-section view). JTE convention in this codebase is no-underscore
filename — partials live as plain `*.jte` files alongside the pages
that use them.

The "View Life Stages →" link at `detail.jte:47–50` is removed; the
standalone `/life-stages` route is kept (reframed as a print/plate view)
but no longer linked from the detail page.

### Resolver vs repository split

- The resolver (`InsectLifeStages.stagesOf(species, genus, family, order)`
  — PR 1's new overload) answers *which `LifeStageKind`s the organism
  has*, used for ordering and as a render decision.
- The repository query
  (`insectLifeStageQuery.lifeStages().forParentName(species.name())`)
  returns the actual `LifeStage` records with full per-stage detail.

A stage the metaboly declares but the catalog hasn't filled in is silently
absent from the inline section — matching today's `life-stages.jte`
behaviour for empty stage lists.

### Controller change

`InsectsController.detail(...)` already loads `species` plus the full
rank chain (`genus / family / order` at lines 415–417) and exposes them
to detail.jte. The only addition PR 2 needs is the LifeStage list — the
same shape the `/life-stages` route already constructs at lines 463–465:

```java
List<LifeStage> stages = insectLifeStageQuery.lifeStages().forParentName(speciesName).stream()
        .sorted(Comparator.comparingInt(stage -> stage.kind().ordinal()))
        .toList();
model.addAttribute("stages", stages);
```

`insectLifeStageQuery` is already an injected field (constructor sets it
at line 61). No new dependency wiring. The N+1 risk is bounded — every
detail page already fetches the rank chain; this is one additional
lookup per page (the LifeStage list).

### Inline-field removal

The four `@Nullable EggStage egg / LarvaStage larva / PupaStage pupa /
AdultStage adult` components are removed from **all four** rank records
(PR 1's exploration found `InsectOrder` carries them too, not just the
three originally named):

- **`InsectSpecies`** — components at `:112–115`, `.namedEntityOrNull(...)`
  invariants at `:152–155`, imports at `:10–13`, javadoc passages at
  `:52–75` referencing the four life-stage paragraphs.
- **`InsectGenus`** — components at `:44–47`, the four `withEgg / withLarva
  / withPupa / withAdult` mutators at `:55–73` (deleted entirely),
  invariants at `:84–87`, imports at `:7–10`.
- **`InsectFamily`** — same shape (`:44–47, :57–77, :88–91, :7–10`).
- **`InsectOrder`** — components at `:39–42`, the four `with*` mutators
  at `:50–68` (deleted entirely), invariants at `:77–80`, imports at
  `:7–10`.

### JSON catalog cleanup

**Only `insect-species.json` carries inline life-stage nodes** — the
genera / families / orders JSON files use the inline fields as
always-null (Jackson handles this by leaving them out, since records
default missing components to null). Verified: 24 occurrences of
`"egg" / "larva" / "pupa" / "adult"` keys across 6 species
(`hippodamia-convergens`, `blattella-vaga`, `xylocopa-varipuncta`,
`vanessa-cardui`, `battus-philenor`, `colias-eurytheme`). Removing
these blocks shrinks the file by ~300 lines. The other three JSON
files are already clean — no changes needed there.

**Pre-deletion drift check.** Before deletion, run a one-shot test
script (or Java test) that, for each species entry in
`insect-species.json` carrying inline `egg / larva / pupa / adult`
nodes, asserts a same-name twin exists in `life-stages.json` with
structurally-equal contents (description, phenology, habitat,
kind-specific fields). Verified pre-implementation: all 6 species with
inline nodes already have life-stages.json twins. The test is captured
in the PR description and deleted in the same PR after passing.

### Test impact

- `InsectSpeciesTest`, `InsectGenusTest`, `InsectFamilyTest`,
  `InsectOrderTest` — remove inline-stage assertions and constructor
  calls.
- `SpeciesRepositoryTest`, `GenusRepositoryTest`, `FamilyRepositoryTest`,
  `OrderRepositoryTest` — `newEntity / modifiedEntity / ghostEntity`
  lose the four stage arguments per ADR-002.
- `SpeciesQueryImplTest` and similar — same constructor shape change.
- `InsectLifeStagesTest` — its helpers (`speciesWithPlacedIn`,
  `genusWithPlacedIn`, `familyWithPlacedIn`, `orderWithPlacedIn`) lose
  the four trailing `null` arguments per constructor shrinkage.
- `InsectsControllerWebMvcTest` — assertions for the new collapsible
  section and absence of the "View Life Stages →" button.
- `InsectAggregateFactoryTest` — unchanged; aggregate does not reference
  stages.
- No new fixtures.

### Manual smoke before declaring done

Per the project UI rule: start the dev server, open a species detail
page, expand the new "Life Stages" section, expand a nested per-stage
block, reload to verify localStorage persistence, navigate to a different
species and verify the open state persisted (matching how Order / Family
/ Genus persist today), confirm the standalone `/life-stages` route
still renders.

**LOC estimate.** Records / invariants (four ranks): ~160 down. JSON
(species file only): ~300 down. JTE: ~80 down on detail.jte +
~120 down on life-stages.jte (replaced by partial call) + ~140 up for
the new `stageFacts.jte` partial + the new detail.jte life-stages
section. Tests (fixture compile-fixes across four ranks + their repo /
query tests + InsectLifeStagesTest helpers): ~200 net down.
Controller: ~5 up. Net diff: ~−800 LOC; meaningful review surface
~300 lines (new partial + detail.jte rewrite + four record
constructor shrinkages + controller addition). Borderline against the
400-line target — most volume is mechanical compile-fix work that
flows naturally from the record changes.

---

## PR 3 — ~~Write-time clade validation~~ (CANCELLED 2026-05-31)

> **Status: cancelled.** The original design proposed putting cross-record
> clade-conformance validation on `LifeStageEntityRepository.insert(...)`,
> with the repository constructor taking four rank queries as
> dependencies. That violates the project's repository contract from
> `domains/CLAUDE.md`: *"A repository has exactly four responsibilities:
> entity cache, referential integrity, unique constraints, transactional
> consistency — no logic."* Cross-record domain logic belongs on an
> aggregate, not a repository.
>
> The invariant ("every `LifeStage.kind()` ∈ resolved `Metaboly.stages()`
> for the organism's clade chain") moves to the new `Insect` aggregate's
> `invariants()` — see [`insect-aggregate.md`](insect-aggregate.md)
> Phase 3. Phase 5b's effective scope is now PRs 1 + 2 only; PR 3 is
> not executed.
>
> The original PR 3 design below is retained for reference but should not
> be implemented.

---

### Original (cancelled) design

Closes Phase 5's deferred open question.

**Where the validation lives.** `LifeStageEntityRepository`'s insert
path. The repository constructor takes the four rank queries it needs to
walk the chain:

```java
LifeStageEntityRepositoryMock(
    SpeciesQuery speciesQuery,
    GenusQuery   genusQuery,
    FamilyQuery  familyQuery,
    OrderQuery   orderQuery) { … }
```

On insert: read the parent slug via `LifeStageName.parentSlug()`
(returns `String` today — PL-12 would type this), probe the four rank
queries in order (species → genus → family → order) by constructing
`InsectSpeciesName.of(slug)` / `InsectGenusName.of(slug)` / etc. and
calling `getByName(...)` on each — first non-empty match identifies the
rank. Resolve the remaining chain via the typed parent FKs the matched
entity already carries (`species.genusName()`, `genus.familyName()`,
`family.orderName()`). Then call
`InsectLifeStages.stagesOf(species, genus, family, order)` (PR 1's
overload), reject if `stage.kind()` is not in the returned list.

The four-rank probe is O(4) repository lookups per insert — bounded and
cheap against in-memory adapters. When PL-12 lands the typed
`parentName : InsectRankName` component on `LifeStage`, the probe
collapses to a single sealed-permit switch on the typed parent.

**Why not on `LifeStage.invariants()`.** The constraint is cross-record —
it requires the organism's rank chain to resolve `placedIn` and walk the
clade DAG. Outside the record's own scope. Belongs at the same boundary
that enforces uniqueness and referential integrity.

**Error mode.** Invariant-style violation via `observer().arguments(...).
throwWhenInvalid()` — same family as other domain-specific repository
method validations (per the project's repository-mock-validation
convention). Named constraint `"kind"`, message names both the metaboly
and the invalid kind:
`"LARVA is not declared by Battus philenor's metaboly (Holometabolous → Egg, Larva, Pupa, Adult)"`.

**Unresolvable cases.** Organism with no `placedIn` anywhere up the chain
→ resolver returns empty list → write rejected with a specific message:
`"Battus philenor has no clade placement on any rank; cannot validate life-stage kinds. Place the species, genus, family, or order in a clade carrying a MetabolyTrait."`

The kind-not-in-metaboly and missing-placement messages share the same
error type but distinct content. Organism-doesn't-exist is the
referential-integrity path, already enforced; not touched by this PR.

**Why write-time, not query-time.** The catalog is hand-curated and
small. Silent drift is the failure mode this slice exists to prevent;
deferring the check to readers lets new drift creep back in. Write-time
costs nothing extra because every reader is already going through the
repository.

**Tests** — additions to `LifeStageEntityRepositoryTest` contract:

- Insert with kind matching declared metaboly → succeeds.
- Insert with kind not in declared metaboly → throws, message names the
  metaboly + invalid kind.
- Insert when organism has no placement on any rank → throws with the
  missing-placement message.
- Insert when organism is placed in a clade with no `MetabolyTrait`
  declaration up the chain → throws with the missing-placement message.
- Insert with kind matching a hemimetabolous metaboly → succeeds.
  Asserts validation isn't holometaboly-only.
- Insert on an unplaced species whose family is placed → succeeds via
  PR 1's walk-up. Asserts the validation uses the walk-up overload.

**Pre-merge catalog conformance.** Existing `life-stages.json` (60
entries, seeded by Phase 5) must satisfy the new invariant on load. Run
the validation logic against the catalog before wiring into the
repository constructor; any failures fixed in the same PR (correct the
catalog entry or place the organism in the right clade).

**Wiring impact.** `LifeStageEntityRepositoryMock` constructor grows by
four arguments. Every construction site updates — predominantly
`*TestContext.create(...)` in the console module and contract test
fixtures.

**LOC estimate.** ~30 repository + ~75 wiring + ~120 contract tests ≈
~250 LOC.

---

## Risk register

- **PR 2's catalog deletion is irreversible by mistake.** Inline edits
  made post-Phase-5 that never propagated to `life-stages.json` would be
  silently dropped. *Mitigation:* drift check runs in-PR with output in
  description; catalog diff reviewed line-by-line (mechanically uniform,
  so non-mechanical entries flag themselves).

- **PR 3's write-time validation breaks the catalog boot path.** A bad
  `life-stages.json` entry → app refuses to start. *Mitigation:* dry-run
  the validation against the existing catalog in PR 3 before wiring it
  into the constructor; treat clean load as a precondition for merge.

- **PR 1's walk-up changes resolution semantics for existing callers.**
  Mitigated by keeping the narrow single-rank overloads unchanged. Only
  the new overload walks up; consumers opt in.

- **PL-12 intersects with PR 3.** PR 3 recovers the parent rank from
  `LifeStageName.parentSlug()` (returns `String`). PL-12 wants to promote
  that to a typed `InsectRankName` component. Order of landing doesn't
  block either — PR 3 uses the slug-parse; PL-12 simplifies later. Note
  the coupling in PL-12's resolution path.

- **Standalone `/life-stages` page becomes increasingly redundant.** Kept
  by explicit decision (print/plate framing). If usage signal stays at
  zero post-merge, raise as a future parking-lot entry. Not this slice's
  job.

---

## Out of scope

- Removing `ChemicalDefense.protectedStages` (the `Set<LifeStageKind>` on
  `InsectSpecies`). Phase 5 flagged it as *"candidate for removal once
  every stage carrying a chemistry role is populated"* — separate
  concern, different removal criterion.
- Backfilling `placedIn` on every species / genus. The walk-up obviates
  the need; placements stay where they are biologically declared.
- `InsectSubspecies` placement plumbing. No entity exists yet.
- Plant-side clade work (Phase 6 of `clades-kernel.md`, deferred).

---

## Open questions

None remaining. The brainstorming session resolved:

- Resolver placement (Option A — extend the static utility with a
  hierarchy overload).
- Detail-page contents (nested expanding sections, full per-stage detail
  inline, partial template shared with `/life-stages`).
- Fate of standalone `/life-stages` page (kept, no longer linked from
  detail page).
- Linnaean walk-up (yes, in the resolver kernel — biology lives where
  the biology says it should).
- Slicing (three PRs, in the order above).
- Write-time vs query-time validation (write-time; data quality is
  load-bearing for the curated catalog).

---

## Work-tracker slot

Row to add to [`docs/work-tracker.md`](../work-tracker.md):

> **Clades kernel — Phase 5b (life-stage inline removal)** — Slice plan
> — *active — PR 1 next* —
> [`plans/clades-kernel-phase-5b-life-stage-inline-removal.md`](plans/clades-kernel-phase-5b-life-stage-inline-removal.md)
> — *Removes the inline `egg / larva / pupa / adult` value-object copies
> from the three rank records, extends the clade resolver to walk up the
> Linnaean parent chain, and adds write-time clade validation to the
> `LifeStage` repository insert. Closes the residual scope Phase 5
> deferred.*

The `clades-kernel.md` phase summary table also gains a row between
Phase 5 and Phase 6:

> | 5b    | Inline life-stage removal + resolver walk-up        | active — PR 1 next ([`clades-kernel-phase-5b-life-stage-inline-removal.md`](clades-kernel-phase-5b-life-stage-inline-removal.md)) | Phase 5 ✅ |
