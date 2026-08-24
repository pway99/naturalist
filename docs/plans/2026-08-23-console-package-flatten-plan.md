# Delete the `.console` Package Segment — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:subagent-driven-development, one domain per task. Steps use checkbox (`- [ ]`) syntax.

**Goal:** Remove the `.console` segment from every `<domain>-console` module's packages so console classes live in the domain namespace (`com.naturalist.<domain>[.<sub>]`), a split-package sub-context like `<domain>-test-context` / `<domain>-repository-rdms`. This lets the controller reach package-private core types — reverting the two `public` transaction widenings from the de-fork — and aligns package naming with the project's split-package convention.

**Architecture:** Pure package move (`git mv` + `package`-declaration edit) plus reference updates (in-module Java imports, JTE `@import` lines, test packages). No behavior changes. `mvn verify` per domain is the gate — Java compile catches missed Java refs, JTE compile in `management-console` catches missed template imports.

**Tech Stack:** Java 21, Maven multi-module, JTE templates, Spring component scan (finds beans by type — package-agnostic).

## Global Constraints

- Trunk-based on `main`; **the user pushes — never push**. Subagents STAGE ONLY (no commit); the controller session stops for user sign-off; commit only on explicit "commit".
- Never `git add -A`; stage only each task's files. Leave the untracked `docs/plans/2026-08-21-n-plus-one-*` files alone.
- Commit footer: `Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>`.
- One domain per commit (ADR-019).
- **Locked decisions:** (1) Drop only the `.console` segment, preserving sub-structure — `com.naturalist.<domain>.console` → `com.naturalist.<domain>`; `.console.catalog` → `.catalog`; `.console.render` → `.render`; library `.console.citation` → `.citation`, `.console.clade` → `.clade`, `.console.concept` → `.concept`, `.console.glossary` → `.glossary`. (2) Visibility: revert only what the move frees — the 2 insects transactions to package-private, and any console class that is NOT referenced by a JTE template and NOT a component-scanned/injected bean; **leave template-facing helpers and `@Controller`/`@Component`/`EntityRefLinker` beans `public`** (a JTE template's generated class lives in `management-console`, a different module, so it can only see `public` types).

## Verified facts

- **No simple-name collisions** between console root-package classes and existing `com.naturalist.<domain>` classes (api/core/test-context/rdms) in any of the 6 domains — the flat move is safe.
- **No cross-module by-name references** to `*.console.*` from `apps/management-console` Java (beans resolve by type via component scan). JTE `@import`s are the only external coupling.
- JTE templates per module: insects-console 23, plants-console 18, library-console 8, chemistry-console 7, soil-console 4, garden-console 2, management-console 12 (shared layout + search/admin).
- Console sub-packages present: `.catalog` (all 6), `.render` (insects, plants), library `.citation/.clade/.concept/.glossary`.
- No ArchUnit rule keys on the `.console` package (Resilience/DataFork rules use `com.naturalist` / `..rdms..`).

## Per-domain transform recipe (applies to every task)

For domain `<d>` in module `<d>-console`:

1. **Move Java files** (main + test). For each `.java` file whose package is `com.naturalist.<d>.console[.<sub>]`:
   - `git mv` the file from `.../com/naturalist/<d>/console[/<sub>]/X.java` to `.../com/naturalist/<d>[/<sub>]/X.java` (drop the `console` path segment; keep `<sub>` for catalog/render/citation/etc.).
   - Edit its `package` declaration: `com.naturalist.<d>.console[.<sub>]` → `com.naturalist.<d>[.<sub>]`.
2. **Update Java imports** anywhere in the module that referenced the old packages: `import com.naturalist.<d>.console` → `import com.naturalist.<d>` (and the `.console.<sub>` → `.<sub>` variants). Classes now in the same package as a referenced type no longer need the import at all, but a redundant same-package import still compiles — the gate is compilation, so leave or drop as convenient; prefer dropping now-same-package imports for cleanliness.
3. **Update JTE `@import`** lines in `<d>-console/src/main/jte/**` (and `apps/management-console/src/main/jte/**` if any import this domain's console classes): `@import com.naturalist.<d>.console...` → `@import com.naturalist.<d>...`.
4. **Empty dirs:** after `git mv`, remove the now-empty `console/` directories (git ignores empty dirs; nothing to commit — just don't leave stray files).
5. **Verify:** `mvn -q -pl <d>-console-path,apps/management-console -am test` (compiles the module AND the management-console JTE templates that composite this domain). Then a full `mvn verify` at the end (Task 7).

Directory examples (insects): `.../insects/console/InsectsController.java` → `.../insects/InsectsController.java`; `.../insects/console/render/InsectsParagraphCues.java` → `.../insects/render/InsectsParagraphCues.java`; `.../insects/console/catalog/InsectsLinker.java` → `.../insects/catalog/InsectsLinker.java`.

---

## Task 1: insects (carries the transaction-visibility revert)

**Files:** all of `domains/insects/insects-console/src/{main,test}/java/com/naturalist/insects/console/**` (root + `render` + `catalog`) → drop `.console`; all `domains/insects/insects-console/src/main/jte/**` `@import`s; plus revert 2 core files.

- [ ] **Step 1: Apply the transform recipe** to insects-console (main + test Java, and the 23 JTE templates). Root-console classes (incl. `InsectsController`) land in `com.naturalist.insects`; `render`→`com.naturalist.insects.render`; `catalog`→`com.naturalist.insects.catalog`.
- [ ] **Step 2: Revert the transaction publics.** In `domains/insects/insects-core/src/main/java/com/naturalist/insects/InsectCatalogIdentificationTransaction.java` and `InsectAddPhotoTransaction.java`, change the class + constructor back from `public` to package-private (remove the `public` modifier only — leave everything else). `InsectsController` is now in `com.naturalist.insects` so it still `new`s them fine.
- [ ] **Step 3: Optional internal downgrade.** Check whether `InsectIdentificationCommand` / `InsectAddPhotoCommand` (insects-core, currently `public`) are referenced anywhere outside `com.naturalist.insects` (grep the repo). If only the controller uses them (now same-package) and no JTE template imports them, downgrade to package-private; otherwise leave public. Do NOT downgrade anything a template imports or that is a bean.
- [ ] **Step 4: Verify.** `mvn -q -pl domains/insects/insects-console,apps/management-console -am test` → BUILD SUCCESS (proves insects templates + management-console composite compile). Confirm `git grep -n "com.naturalist.insects.console"` returns nothing (no stale refs, Java or JTE).
- [ ] **Step 5: Stage only insects files; stop for review; commit on user's word:**
```
refactor(insects): drop the .console package segment; revert transaction visibility to package-private

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>
```

## Task 2: chemistry
- [ ] Apply the transform recipe to `chemistry-console` (root + `catalog`; 7 templates). No core changes.
- [ ] Verify: `mvn -q -pl domains/chemistry/chemistry-console,apps/management-console -am test`; `git grep -n "com.naturalist.chemistry.console"` empty. Stage, review, commit: `refactor(chemistry): drop the .console package segment`.

## Task 3: plants
- [ ] Transform recipe on `plants-console` (root + `render` + `catalog`; 18 templates). Verify (`plants-console,apps/management-console`); `git grep` empty. Commit: `refactor(plants): drop the .console package segment`.

## Task 4: soil
- [ ] Transform recipe on `soil-console` (root + `catalog`; 4 templates; note `catalog.NutrientChemistryLinks` is template-imported → stays public, moves to `com.naturalist.soil.catalog`). Verify; `git grep` empty. Commit: `refactor(soil): drop the .console package segment`.

## Task 5: garden
- [ ] Transform recipe on `garden-console` (root + `catalog`; 2 templates). Verify; `git grep` empty. Commit: `refactor(garden): drop the .console package segment`.

## Task 6: library (4 sub-package controllers)
- [ ] Transform recipe on `library-console` (root + `catalog` + `citation` + `clade` + `concept` + `glossary`; 8 templates). The 4 controllers land in `com.naturalist.library.citation` / `.clade` / `.concept` / `.glossary` (they need no core access). `GlossaryLinker` (template-imported) → `com.naturalist.library`, stays public. Verify; `git grep -n "com.naturalist.library.console"` empty. Commit: `refactor(library): drop the .console package segment`.

## Task 7: final sweep + full verify
- [ ] `git grep -n "\.console"` across the repo for any remaining `com.naturalist.<domain>.console` reference (Java or JTE), including `apps/management-console/src/main/jte/**`. Fix any stragglers (e.g. a shared/search template importing a domain console class) in the owning domain's already-made commit if not yet committed, else a small follow-up commit.
- [ ] Full `mvn verify` from repo root → BUILD SUCCESS.
- [ ] Confirm `DataForkComplianceTest` + `ResilienceComplianceTest` still green (no package-based rule was affected).

## Self-Review
- **Coverage:** every `<domain>.console[.sub]` package (survey list) has a task; the transaction-visibility revert (the payoff) is Task 1 Steps 2-3. ✓
- **Decision fidelity:** drops only `.console`, preserves sub-structure (D1); reverts only move-freed visibility, leaves template-facing/bean publics (D2). ✓
- **Risk:** collisions verified absent; JTE `@import` is the sharp edge — `git grep "\.console"` + `mvn verify` (JTE compiles in management-console) are the nets. ✓
- **Type consistency:** `InsectsController` stays `public` (bean); only the 2 transactions (+ optionally the 2 command wrappers) go package-private, and only after confirming no cross-package/template use. ✓
