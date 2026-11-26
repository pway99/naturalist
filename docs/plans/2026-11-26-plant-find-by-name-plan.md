# Plant findByName Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `PlantQuery.findByName` answers the plant asked for or the nearest one the catalog holds (binomial → genus), and the species page uses it to send a visitor to the genus page with a "not added yet" note.

**Architecture:** a new non-deterministic `findByName` beside the exact `getByName` on `PlantQuery`, returning a `PlantMatch` value; `PlantQueryImpl` tries the exact name, then the genus named by a species slug's first segment. `PlantsController.detail` redirects a genus match to `/plants/genera/{genus}?missing={species}`; `genusDetail` renders the note.

**Tech Stack:** Java 21 records, Spring MVC (`@WebMvcTest`), JTE, the kernel Observer.

**Spec:** [`2026-11-26-plant-find-by-name-design.md`](2026-11-26-plant-find-by-name-design.md)

## Global Constraints

- `getByName` stays deterministic and unchanged; the rank pages' `has<Rank>` guards stay as they are.
- The visitor always sees that what they asked for is missing; the genus is never passed off as the species.
- A fallback and a full miss are INFO (a record not added yet); nothing new is WARN.
- No `var`, no star imports, IntelliJ format (`mvn rewrite:run`); an Observer literal names its enclosing method.
- Argument checks go through the Observer, never `requireNonNull`.
- Worktree Maven installs pass `-Dmaven.repo.local=$PWD/temp/m2`.
- Don't push, merge or open a PR without Pat's word.

## Review Focus

- A species slug with no hyphen (`aristolochia`, typed as a species): the first segment is the whole slug; it must answer the genus if the catalog holds one of that name, never throw. Pinned in Task 1.
- A `missing` value that is not a slug (`?missing=Not_A_Slug`) or names a species the catalog *does* hold: the first is dropped (no note), the second still shows the note (the redirect only ever writes misses, a hand-typed URL is harmless). Pinned in Task 2 (not-a-slug case).
- `missing` on a genus page that 404s (`/plants/genera/unobtainium?missing=x`): still the plain not-found page. Covered by the existing unknown-name edge test; no new test.
- A species miss whose genus slug is itself not a valid `PlantGenusName` (e.g. a leading digit segment if slugs allow it): `findByName` answers empty rather than throwing from `buildByName`'s `identifier` check. Pinned in Task 1.
- The redirect URL carries the species slug only after it passed `entityName` validation, so no unescaped input reaches the `Location` header. Holds by ordering in Task 2; asserted by the redirect test's exact URL.

---

### Task 1: `PlantMatch` and `PlantQuery.findByName`

**Files:**
- Create: `domains/plants/plants-api/src/main/java/com/naturalist/plants/PlantMatch.java`
- Modify: `domains/plants/plants-api/src/main/java/com/naturalist/plants/PlantQuery.java` (beside `getByName`)
- Modify: `domains/plants/plants-core/src/main/java/com/naturalist/plants/PlantQueryImpl.java:108`
- Test: `domains/plants/plants-core/src/test/java/com/naturalist/plants/PlantQueryFindByNameTest.java`

**Interfaces:**
- Produces:
  - `public record PlantMatch(PlantRankName asked, PlantRankName found, Plant plant) implements ValueObject`, with `boolean exact()` (`asked.equals(found)`) and `invariants()`: `identifier(asked, "asked")`, `identifier(found, "found")`, `readModel(plant, "plant")`, plus `plantIsFound` and `foundIsAskedOrItsGenus`.
  - `Optional<PlantMatch> PlantQuery.findByName(PlantRankName name)`.
  - Javadoc on both query methods: `getByName` is exact; `findByName` may answer a different (ancestor) rank and says so through `PlantMatch.found`.

- [ ] **Step 1: Write the failing tests** (wire `PlantQuery` with `@Wired` and `NaturalistTestExtension`, as `PlantFactoryTest` wires `PlantFactory`)

```java
@Test void findByName_knownSpecies_isExact() {
    PlantMatch match = plantQuery.findByName(PlantSpeciesName.of("aristolochia-californica")).orElseThrow();
    assertThat(match.exact()).isTrue();
    assertThat(match.found()).isEqualTo(PlantSpeciesName.of("aristolochia-californica"));
    assertThat(match.plant().hasSpecies()).isTrue();
}
@Test void findByName_missingSpecies_answersItsGenus() {
    PlantMatch match = plantQuery.findByName(PlantSpeciesName.of("aristolochia-tomentosa")).orElseThrow();
    assertThat(match.exact()).isFalse();
    assertThat(match.asked()).isEqualTo(PlantSpeciesName.of("aristolochia-tomentosa"));
    assertThat(match.found()).isEqualTo(PlantGenusName.of("aristolochia"));
    assertThat(match.plant().hasGenus()).isTrue();
    assertThat(match.plant().hasSpecies()).isFalse();
}
@Test void findByName_missingSpeciesAndGenus_isEmpty() {
    assertThat(plantQuery.findByName(PlantSpeciesName.of("unobtainium-vine"))).isEmpty();
}
@Test void findByName_speciesSlugWithoutEpithet_triesTheWholeSlugAsAGenus() {
    assertThat(plantQuery.findByName(PlantSpeciesName.of("aristolochia")).orElseThrow().found())
            .isEqualTo(PlantGenusName.of("aristolochia"));
}
@Test void findByName_missingGenus_isEmpty_noClimb() {
    assertThat(plantQuery.findByName(PlantGenusName.of("unobtainium"))).isEmpty();
}
@Test void findByName_fallback_isCountedAtInfo() { /* SimpleMeterRegistry as in PlantsNamedValueEdgeWebMvcTest:
    one violated "PlantQueryImpl.findByName.exact" counter, level tag "info" */ }
@Test void findByName_null_throws() {
    assertThat(catchThrowable(() -> plantQuery.findByName(null)))
            .isInstanceOf(InvariantViolationException.class);
}
@Test void getByName_missingSpecies_staysEmpty() {
    assertThat(plantQuery.getByName(PlantSpeciesName.of("aristolochia-tomentosa"))).isEmpty();
}
```

- [ ] **Step 2: Run and see them fail**

Run: `mvn -q -pl domains/plants/plants-core -am test -Dtest=PlantQueryFindByNameTest -Dsurefire.failIfNoSpecifiedTests=false -Dmaven.repo.local=$PWD/temp/m2`
Expected: compilation failure, `findByName` / `PlantMatch` not found.

- [ ] **Step 3: Implement**

`PlantQueryImpl.findByName`:
1. `observer.arguments("findByName", i -> i.identifier(name, "name")).throwWhenInvalid()`.
2. `getByName(name)`; present → exact match.
3. `observer.state("findByName", c -> c.isPresent(exact, "exact")).observe(Level.INFO)`, the miss counted.
4. Only a `PlantSpeciesName` climbs: `PlantGenusName.of(slug before the first '-')` (the whole slug when there is none). Skip if that genus name is not valid. `getByName(genus)` → match with `found = genus`.
5. Anything else → empty.

Keep the split private to the impl.

- [ ] **Step 4: Run and see them pass** — same command; expected `BUILD SUCCESS`, 8 tests.

- [ ] **Step 5: Commit**

```bash
git add domains/plants/plants-api/src/main/java/com/naturalist/plants/PlantMatch.java domains/plants/plants-api/src/main/java/com/naturalist/plants/PlantQuery.java domains/plants/plants-core/src/main/java/com/naturalist/plants/PlantQueryImpl.java domains/plants/plants-core/src/test/java/com/naturalist/plants/PlantQueryFindByNameTest.java
git commit -m "feat(plants): findByName answers the nearest plant the catalog holds"
```

### Task 2: the species page sends a miss to its genus

**Files:**
- Modify: `domains/plants/plants-console/src/main/java/com/naturalist/plants/PlantsController.java` (`detail` ~line 115, `genusDetail`/`genusPage` ~line 438)
- Modify: `domains/plants/plants-console/src/main/jte/plants/genera/detail.jte` (new `@param PlantSpeciesName missing = null`, note under the `<h1>`)
- Test: `domains/plants/plants-console/src/test/java/com/naturalist/plants/PlantsNamedValueEdgeWebMvcTest.java`

**Interfaces:**
- Consumes: `PlantQuery.findByName`, `PlantMatch` (Task 1).
- Produces: `genusDetail(@PathVariable("name") PlantGenusName genusName, @RequestParam(name = "missing", required = false) @Nullable PlantSpeciesName missing, HttpServletResponse, Model)`, and the model attribute `missing` (`PlantSpeciesName` or absent).

- [ ] **Step 1: Write the failing tests** (in the edge test; replace `detail_unknownName_isNotFound_andIsCounted`'s path with one whose genus is also unknown — it already is, `unobtainium-vine`)

```java
@Test void detail_missingSpeciesOfAKnownGenus_redirectsToTheGenus() throws Exception {
    mockMvc.perform(get("/plants/aristolochia-tomentosa"))
            .andExpect(status().isFound())
            .andExpect(redirectedUrl("/plants/genera/aristolochia?missing=aristolochia-tomentosa"));
}
@Test void genusDetail_missing_saysTheSpeciesIsNotAddedYet() throws Exception {
    mockMvc.perform(get("/plants/genera/aristolochia").param("missing", "aristolochia-tomentosa"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("No record for")))
            .andExpect(content().string(containsString("aristolochia-tomentosa")));
}
@Test void genusDetail_missingNotASlug_isDropped() throws Exception {
    mockMvc.perform(get("/plants/genera/aristolochia").param("missing", NOT_A_SLUG))
            .andExpect(status().isOk())
            .andExpect(content().string(not(containsString("No record for"))));
}
@Test void genusDetail_withoutMissing_showsNoNote() throws Exception {
    mockMvc.perform(get("/plants/genera/aristolochia"))
            .andExpect(status().isOk())
            .andExpect(content().string(not(containsString("No record for"))));
}
```

- [ ] **Step 2: Run and see them fail**

Run: `mvn -q -pl domains/plants/plants-console -am install -DskipTests -Dmaven.repo.local=$PWD/temp/m2 && mvn -q -pl domains/plants/plants-console test -Dtest=PlantsNamedValueEdgeWebMvcTest -Dmaven.repo.local=$PWD/temp/m2`
Expected: the redirect test gets 404; the note test finds no "No record for".

- [ ] **Step 3: Implement**

`detail`:
- Keep the `entityName` argument check (INFO) as it is.
- Call `plantQuery.findByName(plantName)` in place of `getByName`.
- Keep `state(c -> c.isPresent(match, "plant")).observe(Level.INFO).whenNotValid(notInCatalog...)`.
- `.otherwise(...)`: an exact match → `speciesPage(plantName, match.plant(), ...)`. A non-exact match → `"redirect:/plants/genera/" + match.found().value() + "?missing=" + plantName.value()`.

`genusDetail`:
- Check `missing` with `arguments(c -> c.whenNotNull(missing, ...entityName(missing, "missing")))`, observed at INFO and folded into the existing `genusName` check.
- Pass `missing` to `genusPage` only when it is present, valid and of this genus (`PlantMatch.genusOf(missing).equals(genusName)`), else `null`.

`genusPage`:
- Gains a `@Nullable PlantSpeciesName missing` parameter and sets the model attribute when it is non-null.

Template, under the `<h1>`: `@if(missing != null)<p class="not-yet-added">No record for <code>${missing}</code> yet; here is its genus.</p>@endif`.

- [ ] **Step 4: Run and see them pass** — same command; expected `BUILD SUCCESS`, the edge test's 10 tests green, and `PlantsGenusDetailTemplateTest` still green.

- [ ] **Step 5: Commit**

```bash
git add domains/plants/plants-console/src/main/java/com/naturalist/plants/PlantsController.java domains/plants/plants-console/src/main/jte/plants/genera/detail.jte domains/plants/plants-console/src/test/java/com/naturalist/plants/PlantsNamedValueEdgeWebMvcTest.java
git commit -m "feat(plants-console): a missing species sends the visitor to its genus"
```

### Task 3: gate, review, tracker

- [ ] **Step 1: Full gate** — `mvn verify`, then `mvn install -DskipTests -Dmaven.repo.local=$PWD/temp/m2 && mvn rewrite:dryRun -Drewrite.failOnDryRunResults=true -Dmaven.repo.local=$PWD/temp/m2`, then the credential grep (`git ls-files -z -co --exclude-standard | xargs -0 grep -nIiE '[a-z0-9]+[-_]api[-_]?key|sk-ant[-]'`). Expected: all exit 0, the grep prints nothing. Format fixes go through `mvn rewrite:run`.
- [ ] **Step 2: Observability review** — after the builds finish, dispatch `observability-reviewer` with `checkout: <worktree>`. Fix its main-section findings; re-dispatch until `No findings` or only uncertain ones. Report its "Existing code" rows unfixed.
- [ ] **Step 3: Tracker** — in `docs/work-tracker.md`, replace the Later → Plants "Degradable `PlantQuery.getByName`" item with: binomial strategy shipped on this branch (`findByName`); look-alike search strategy is the next PR. Commit the tracker, this plan and the design together.
