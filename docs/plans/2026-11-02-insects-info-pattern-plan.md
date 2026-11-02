# Insects Info Pattern — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give the insects biology its natural names. The stored rank records become `InsectSpeciesInfo` /
`InsectGenusInfo` / `InsectFamilyInfo` / `InsectOrderInfo`. The composed taxon (a record plus its photographs) takes the
bare name `InsectSpecies` / … under a sealed `InsectTaxon`. `InsectCitationView` becomes `InsectCitations`.

**Architecture:** This is a rename-and-reshape with no change in behaviour. It is the soil `SoilProfileInfo` / `SoilProfile`
pattern applied to insects: `*Info` is the persisted `NamedEntity`, and the bare name is the `ReadModel` a factory composes
from it. The four permits of today's `InsectTaxonView` (`SpeciesView`, …) move out of the interface to become top-level
records. Each one renames its record component (`species` → `info`). Every Java write goes through Kaname atoms
(`java_type_name_change`, `java_type_resolution_change`, `java_record_component_name_change`, `java_type_add`). JTE
templates are not indexed by the engine, so they are edited by hand and checked with grep.

**Tech Stack:** Java 25 (records, sealed interfaces), JTE, Maven reactor, Kaname Engine, OpenRewrite gate.

**Spec:** No separate design doc. This plan carries the decision the user made on 2026-11-02 ("Yes, plan it" to the Info
pattern for the insects taxon types, after the slice-3 follow-up on biology types named `*View`). The precedent lives in
`domains/soil/soil-api/.../SoilProfileInfo.java` and `SoilProfile.java`. The history is the 2026-08-25
`docs/plans/2026-08-25-taxonview-nesting-plan.md`: it nested the former top-level `InsectSpeciesView` files and rejected
`.Species` because that name collided with the entity. This plan removes the collision itself, so the composed type can
take the bare name.

## Decisions

Made by the user:
- Use the Info suffix for the stored root of a composed biology type, and the bare name for the composed read model.
- Insects only in this slice. `PlantTaxonView`, `OrganismFeatureView` and `CladeView` wait for the plants ports work.

Controller calls (each one can be changed independently):
- **C1 — the supporting stack keeps its names.** `InsectSpeciesQueryImpl`, `InsectSpeciesCommandImpl`,
  `InsectSpeciesRepositoryMock`, `InsectSpeciesRepositoryTest`, `InsectSpeciesTestEntitySource`,
  `InsectSpeciesEntityRepositoryRdbms`, `InsectSpecies*Dbo`, `InsectSpeciesSeeder`, the nested ports
  (`SpeciesQuery`, `SpeciesRepository`, `SpeciesCollection`) and their genus / family / order siblings are unchanged.
  The reason: under domains/CLAUDE.md's `<DomainNoun><EntitySubject>` rule the subject is the *rank* ("Species"), and
  that does not change. DBO names follow the table names, which also stay. Soil renamed its stack only because
  `SoilProfile` and `SoilProfileInfo` each have one. *Cost if wrong:* about 40 more mechanical renames in a follow-up.
- **C2 — the component is named `info`,** as in `SoilProfile.info()`. So `insect.species().info().commonNames()`.
  *Cost if wrong:* one record-component rename per permit.
- **C3 — `InsectTaxonViewTest` becomes `InsectTaxonTest`, one file.** It is not split per permit. The unit tests of the
  old records become `InsectSpeciesInfoTest` and so on.
- **C4 — docs to update:** `domains/CLAUDE.md`, `domains/insects/CLAUDE.md`, `kernels/CLAUDE.md`, ADR-020's examples,
  `domains/insects/README.md` and the `.claude/skills/*` files that name these Java types. Plans, archives,
  briefings and pressure-test notes are historical and stay untouched.

## Global Constraints

- No change in behaviour. No new fields, invariants, queries or JSON. Table names and seed-file paths
  (`insects/insect-species.json`, …) are unchanged. If an existing test needs a change in *logic* rather than in the
  type names it uses, stop: the refactor has altered behaviour.
- All Java writes go through engine atoms with `apply_change`. JTE and Markdown are the only hand edits.
- Plants, the taxonomy kernel and library are not touched.
- Each task leaves the reactor compiling, and it ends with `mvn -q -pl domains/insects/insects-console,apps/management-console,apps/test-db-seeder -am verify` green.
- Never weaken a test or a gate.
- The worktree has its own envelope, rebuilt with `scripts/build-corpus-envelope.sh` before Task 1 and after each task's
  commit.

## Review Focus

1. **A JTE template that binds to the wrong type.** Between Tasks 1 and 2, `InsectSpecies` stops existing and then comes
   back meaning something else. A template still spelling `InsectSpecies` where it means the stored record would bind to
   the composite. Expected result: Task 1 ends with zero bare `InsectSpecies\b` / `InsectGenus\b` / `InsectFamily\b` /
   `InsectOrder\b` in any `.jte` or `.java` under `domains/insects` and `apps/`, checked by grep before Task 2 starts.
2. **`.species().species()` chains.** These chains are in `InsectsController` (14 sites), `InsectViewQueryImplTest` (1)
   and 4 `.jte` sites, and must all become `.species().info()`. Expected result: zero
   `\.(species|genus|family|order)\(\)\.(species|genus|family|order)\(\)` left in insects after Task 2.
3. **Exhaustiveness of the sealed type.** The permits move out of the interface's file, so the `permits` clause must be
   written out. Every `switch` over `InsectTaxon` must stay exhaustive with no `default` added.
4. **Seed and JSON round-trip.** Jackson binds `insect-*.json` to the renamed records by component name. The
   `*TestEntitySourceTest` suites and the RDBMS ITs must pass unchanged.
5. **Name references in text.** Javadoc `{@link}` follows the atom. Plain prose that says "InsectSpecies" for the stored
   record must be reworded where it is now wrong (the `java_type_name_change` candidates tier lists these).

---

### Task 1: Stored records take the Info suffix

**Files:**
- Rename: `domains/insects/insects-api/src/main/java/com/naturalist/insects/{InsectSpecies,InsectGenus,InsectFamily,InsectOrder}.java` → `…Info.java`
- Rename: `domains/insects/insects-api/src/test/java/com/naturalist/insects/{InsectSpecies,InsectGenus,InsectFamily,InsectOrder}Test.java` → `…InfoTest.java`
- Modify: every Java file that references them (the atom finds them: 42 / 36 / 36 / 30 files)
- Modify (hand): `domains/insects/insects-console/src/main/jte/insects/{life-stages,genus,family,order}.jte` and any other `.jte` under `domains/insects` that imports or takes one of the four as `@param`

**Interfaces:**
- Produces: `com.naturalist.insects.InsectSpeciesInfo implements NamedEntity<InsectSpeciesName>` (same components and
  methods as today's `InsectSpecies`, including `displayName(@Nullable InsectGenusInfo)` and `binomial(...)`), and
  likewise `InsectGenusInfo`, `InsectFamilyInfo`, `InsectOrderInfo`.

- [ ] **Step 1:** Preview and apply `java_type_name_change` for each of the four records (`fromFullyQualifiedName`
  `com.naturalist.insects.InsectSpecies`, `toSimpleName` `InsectSpeciesInfo`, and the same for the others). Use
  `rebuild: false` on the first three and the default on the last.
- [ ] **Step 2:** Apply `java_type_name_change` to the four unit-test classes (`InsectSpeciesTest` → `InsectSpeciesInfoTest`, …).
  The atom leaves convention-coupled siblings alone, so they need renames of their own.
- [ ] **Step 3:** Hand-edit the `.jte` templates: `@import com.naturalist.insects.InsectSpecies` → `…InsectSpeciesInfo`,
  `@param InsectSpecies` → `@param InsectSpeciesInfo`, and the same for the other three.
- [ ] **Step 4:** Reword the prose candidates the atom reported where they now describe the stored record by the wrong name.
- [ ] **Step 5: Verify that no bare old name remains.**
  Run: `grep -rnE '\b(InsectSpecies|InsectGenus|InsectFamily|InsectOrder)\b' --include='*.java' --include='*.jte' domains/insects apps`
  Expected: no output.
- [ ] **Step 6:** Run `mvn -q -pl domains/insects/insects-console,apps/management-console,apps/test-db-seeder -am verify`. Expected: BUILD SUCCESS with no test changed beyond its type names.
- [ ] **Step 7:** Commit `refactor(insects): stored rank records take the Info suffix`.

### Task 2: Taxon permits become top-level `InsectSpecies` / `InsectGenus` / `InsectFamily` / `InsectOrder`

**Files:**
- Rename: `InsectTaxonView.java` → `InsectTaxon.java` (insects-api main); `InsectTaxonViewTest.java` → `InsectTaxonTest.java`
- Create: `domains/insects/insects-api/src/main/java/com/naturalist/insects/{InsectSpecies,InsectGenus,InsectFamily,InsectOrder}.java`
- Modify: `Insect`, `InsectFactory`, `InsectViewFactory`, `InsectsController`, `InsectTest`, `InsectFactoryTest`,
  `InsectViewQueryImplTest`, the `Insects*TemplateTest`s, and every other file that references the permits
- Modify (hand): the `.jte` sites with `.species().species()` (and genus / family / order) chains

**Interfaces:**
- Consumes: the `*Info` records from Task 1.
- Produces:
  ```java
  public sealed interface InsectTaxon extends ReadModel
          permits InsectSpecies, InsectGenus, InsectFamily, InsectOrder {
      InsectRankName name();
      ImageCollection images();
  }
  public record InsectSpecies(InsectSpeciesInfo info, ImageCollection images) implements InsectTaxon
  public record InsectGenus(InsectGenusInfo info, ImageCollection images) implements InsectTaxon
  public record InsectFamily(InsectFamilyInfo info, ImageCollection images) implements InsectTaxon
  public record InsectOrder(InsectOrderInfo info, ImageCollection images) implements InsectTaxon
  ```
  Each record keeps today's permit members exactly, with only the types re-spelled: both `of(...)` factories, `name()`,
  `genusName()` / `familyName()` / `orderName()`, `belongsToGenus(@Nullable InsectGenus)` /
  `belongsToFamily(@Nullable InsectFamily)` / `belongsToOrder(@Nullable InsectOrder)`, and `invariants()`. The invariant
  label changes from `"species"` to `"info"` along with the component.
  `Insect` keeps its accessor names (`species()`, `genus()`, `family()`, `order()`, `children()`), and their types become
  the new records and `List<InsectTaxon>`.

- [ ] **Step 1:** Apply `java_type_name_change` `com.naturalist.insects.InsectTaxonView` → `InsectTaxon`, then
  `InsectTaxonViewTest` → `InsectTaxonTest`.
- [ ] **Step 2:** Apply `java_record_component_name_change` on each nested permit: `InsectTaxon.SpeciesView.species` →
  `info`, `GenusView.genus` → `info`, `FamilyView.family` → `info`, `OrderView.order` → `info`. Accessor call sites
  (`sv.species()` → `sv.info()`) follow the atom.
- [ ] **Step 3:** Add each top-level record with `java_type_add`, with its members copied from the nested permit.
- [ ] **Step 4:** Re-point every reference with `java_type_resolution_change`: `InsectTaxon.SpeciesView` → `InsectSpecies`,
  `.GenusView` → `InsectGenus`, `.FamilyView` → `InsectFamily`, `.OrderView` → `InsectOrder`.
- [ ] **Step 4b:** Reduce `InsectTaxon` to the interface shown above: delete the four nested permits, add
  `permits InsectSpecies, InsectGenus, InsectFamily, InsectOrder`, and replace its Javadoc's permit list with the four
  top-level links. **Known atom gap:** the engine has no nested-type removal and no `permits`-clause atom (see
  [[project-kaname-engine-setup]]). This one file is a hand rewrite. Its text is fixed by the Interfaces block, so there
  is exactly one way to write it. If the write hook refuses, the controller records the gap and makes the edit.
- [ ] **Step 5:** Hand-edit `.jte`: `.species().species()` → `.species().info()`, and the same for genus, family and order. Re-spell
  any `@import`/`@param` of `InsectTaxonView…` as the new names.
- [ ] **Step 6: Add the exhaustiveness test** in `InsectTaxonTest`:
  ```java
  @Test
  void switchOverEveryPermitCompilesWithoutDefault() {
      InsectTaxon taxon = InsectOrder.of(orderInfo);
      String rank = switch (taxon) {
          case InsectSpecies s -> "species";
          case InsectGenus g -> "genus";
          case InsectFamily f -> "family";
          case InsectOrder o -> "order";
      };
      assertThat(rank).isEqualTo("order");
  }
  ```
  `orderInfo` is the `InsectOrderInfo` fixture `InsectTaxonTest` already builds for its order-permit tests. Do not add a
  new fixture class.
- [ ] **Step 7: Verify that no chains or old names remain.**
  Run: `grep -rnE '\.(species|genus|family|order)\(\)\.(species|genus|family|order)\(\)|InsectTaxonView|SpeciesView|GenusView|FamilyView|OrderView' --include='*.java' --include='*.jte' domains/insects apps`
  Expected: no output.
- [ ] **Step 8:** Run the same `mvn -q -pl … -am verify` as Task 1. Expected: BUILD SUCCESS.
- [ ] **Step 9:** Commit `refactor(insects): taxon permits become top-level InsectSpecies/Genus/Family/Order over *Info`.

### Task 3: `InsectCitationView` → `InsectCitations`

**Files:**
- Rename: `InsectCitationView.java` → `InsectCitations.java`; `InsectCitationViewTest.java` → `InsectCitationsTest.java`
- Modify: `Insect`, `InsectQuery`, `InsectCitationQueryImpl`, `InsectCitationQueryImplTest`, `InsectTest` (the atom finds them all)
- Modify (hand): `domains/insects/insects-console/src/main/jte/insects/citations.jte`

**Interfaces:**
- Produces: `public record InsectCitations(InsectRankName subject, List<RankedCitation> citations) implements ReadModel`,
  with the nested `RankedCitation` unchanged. `Insect.citations()` returns `InsectCitations`.

- [ ] **Step 1:** Apply `java_type_name_change` for the record and then for its test.
- [ ] **Step 2:** Re-spell `InsectCitationView` in `citations.jte` (`@import`, `@param`, `InsectCitationView.RankedCitation`).
- [ ] **Step 3:** Run `grep -rn 'InsectCitationView' --include='*.java' --include='*.jte' domains apps`. Expected: no output.
- [ ] **Step 4:** Run the `mvn -q -pl … -am verify` from Task 1. Expected: BUILD SUCCESS.
- [ ] **Step 5:** Commit `refactor(insects): InsectCitationView becomes InsectCitations`.

### Task 4: Write the convention down

**Files:**
- Modify: `domains/CLAUDE.md`. Rewrite the Views-namespace paragraph's last sentences and add an **Info pattern**
  paragraph beside it. Fix the reference-implementation line ("`InsectSpecies` (nested value-object graph)") to name
  `InsectSpeciesInfo`.
- Modify: `domains/insects/CLAUDE.md`. The `InsectSpecies` vocabulary entry becomes `InsectSpeciesInfo` (stored) plus
  `InsectSpecies` (composed), and the `InsectTaxonView` entry becomes `InsectTaxon` with top-level permits and an explicit
  `permits` clause.
- Modify: `docs/adr/ADR-020-namespace-interface-pattern.md`, its examples only (no change to the decision), `kernels/CLAUDE.md`
  mentions, `domains/insects/README.md`, and `.claude/skills/{entity-query,entity-repository,identify-insect,test-entity-source}`
  wherever they name these Java types (the skills' JSON field names do not change).

The Info pattern paragraph (exact copy):

> **Info pattern.** When a composed biology read model and its persisted root would want the same name, the persisted
> `NamedEntity` takes the `Info` suffix and the composed `ReadModel` takes the bare name, so the bare name is the fuller
> thing a biologist means: `SoilProfileInfo` / `SoilProfile`, `InsectSpeciesInfo` / `InsectSpecies` (the species with its
> photographs). The `<DomainNoun><EntitySubject>` stack (query/command impls, mocks, test-entity-sources, DBOs) keeps
> the rank subject (`InsectSpeciesQueryImpl` persists `InsectSpeciesInfo`). It renames only where, as in soil, both
> types own a stack of their own. A sealed family whose permits are top-level records spells `permits` explicitly.
> `CompoundInfo` in chemistry predates this and means a field group inside `Compound`, so do not copy it.
> Plants adopts this when ported.

- [ ] **Step 1:** Make the edits above.
- [ ] **Step 2:** Run `grep -rnE 'InsectTaxonView|InsectCitationView|SpeciesView' domains/CLAUDE.md domains/insects kernels/CLAUDE.md docs/adr/ADR-020-namespace-interface-pattern.md .claude/skills`. Expected: no output.
- [ ] **Step 3:** Rebuild the envelope, then run the full gate from the repo root:
  `mvn verify` followed by `mvn install -DskipTests && mvn rewrite:dryRun -Drewrite.failOnDryRunResults=true`. Expected: both BUILD SUCCESS.
- [ ] **Step 4:** Commit `docs(insects): record the Info pattern; vocabulary for InsectTaxon and *Info`.
