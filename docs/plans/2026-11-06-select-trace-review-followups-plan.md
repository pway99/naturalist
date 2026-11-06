# SelectTrace Review Follow-ups — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Close the findings from the 2026-11-06 review of the StackWalker N+1 gate (`SelectTrace`) so the work on
`spike/stackwalker-select-gate` can merge.

**Architecture:** No design change. The review judged the strategy sound and the full `mvn verify` green. What remains
is one stale javadoc, limits the gate's own doc does not yet state, two untested rules, one small kernel cleanup, and
four decisions that belong to Pat. Each decision has a recommended default, and the tasks proceed on those defaults.

**Tech Stack:** Java 25, JUnit 5, AssertJ, Maven reactor, Kaname Engine atoms, OpenRewrite gate.

**Spec:** No separate design doc. The review was delivered in session on 2026-11-06; its findings are restated in the
table below so this plan stands alone. The gate's current description is
`kernels/framework-test/src/main/java/com/naturalist/test/query/nofanout/CLAUDE.md`.

## Global Constraints

- Never weaken the gate to make a test pass (root `CLAUDE.md` non-negotiable). Nothing in this plan stops a read from
  being counted.
- Every Java write goes through Kaname atoms (`apply_change`); Markdown is edited by hand.
- Stage only. The whole branch is uncommitted until Pat says "commit"; no task commits.
- `kernels/framework-test` carries the "no test infra for test infra" rule. `SelectTraceTest` exists under the
  stateful/lifecycle carve-out; Task 2 adds two cases to it and no new fixture file.

## Findings and where each one goes

| # | Finding | Resolution |
|---|---------|------------|
| 1 | The rdbms mapper gate was retired, so nothing proves an adapter answers a batch in one statement | Decision D1 |
| 2 | The rule is "no select twice per test call", stricter than N+1; multi-transaction Commands re-read legitimately | Task 1 (state it in Limits) |
| 3 | `@SuppressRepeatedSelect` covers everything beneath the method and cannot name a select | Decision D2 |
| 4 | About 45 accessors are hand-wrapped with no enforcement; the doc still says "no wrapper" | Task 1 (fix the sentence), Decision D3 |
| 5 | The gate counts source reads, not repository calls: a mock method reading its source twice would false-fail | Task 1 (state the rule) |
| 6 | A read reached only through `com.naturalist.data` frames is not counted | Task 1 (state it in Limits) |
| 7 | `SelectTrace.record` ships in the app, because `framework-test` is compile scope in `management-console` | Task 1 (state it in Limits) |
| 8 | `UsageQueryImpl` javadoc still calls itself a "head-of-DAG" for the gate | Task 1 |
| 9 | Reuse ids now come from search candidates only: a behaviour change with no test | Decision D4, Task 3 |
| 10 | `InMemoryFeatureSearch.findSimilarToEach` lacks `@Override` and re-tokenises each feature per query value | Task 3 |
| 11 | The page-0-only rule in `TestEntitySource.pageOf` has no case in `SelectTraceTest` | Task 2 |
| 12 | `rewrite:dryRun` and the credential grep were not run during the review | Task 4 |

## Decisions for Pat

Each has a default. Say otherwise before Task 4 and the plan changes; say nothing and the default stands.

- **D1 — rdbms mapper gate.** Default: accept the retirement. `<domain>-core` is the layer that matters and adapters
  stay thin. If an adapter N+1 ever ships, the MyBatis `Executor.query` interceptor never needed AspectJ; only the
  head marker did, and a stack walk to the repository frame replaces it.
- **D2 — suppression scope.** Default: leave `@SuppressRepeatedSelect` as a whole-subtree switch. Two sites exist
  (`InsectIdentificationCommandImpl.enrichIfNew`, `.writeCitations`). Revisit when a third arrives: that is the
  signal to add a `select` narrowing or a per-site inventory.
- **D3 — wrapping enforcement.** Default: no recipe. Forgetting a wrapper can only cause a false failure, which is
  its own reminder.
- **D4 — reuse ids from candidates only.** Default: accept. The model may reuse only a feature it was offered; a
  reuse target outside the candidates mints a new feature. Task 3 pins this with a test. If rejected, stop before
  Task 3 step 3: restoring whole-corpus reuse needs the value-to-id map carried out of the single corpus pass, which
  changes `FeatureSearch`.

## Review Focus

- A repository mock method that reads its source twice: the developer sees a `RepeatedSelectException` naming one
  repository method and needs the doc to say why. Task 1.
- A paged read whose data exceeds one page: must count once, not once per page. Task 2.
- The first page of the same select read twice in one call: must fail. Task 2.
- The model names an existing feature it was not offered: must mint a new feature, not throw and not reuse. Task 3.

---

### Task 1: Bring the docs in line with what the gate does

**Files:**
- Modify: `domains/usage/usage-core/src/main/java/com/naturalist/usage/UsageQueryImpl.java:14-27` (class javadoc)
- Modify: `kernels/framework-test/src/main/java/com/naturalist/test/query/nofanout/CLAUDE.md`

- [ ] **Step 1: Replace the first paragraph of the `UsageQueryImpl` class javadoc** (`java_javadoc_change`). The
  second paragraph ("Deliberately holds no lock…") stays as it is.

```java
/**
 * Read-side adapter for {@link UsageQuery}. Each of {@link #snapshot()} and
 * {@link #reserveState} makes exactly two repository selects — one
 * {@code counters.findByCounterName} and one batched
 * {@code events.findByCounterSince} — and counts in memory from there. No
 * per-rule or per-user repository call; the N+1 select gate
 * ({@code com.naturalist.test.query.nofanout}) holds it to that.
```

- [ ] **Step 2: Fix the opening paragraph of the nofanout `CLAUDE.md`.** "It needs no agent, weaving or wrapper."
  becomes "It needs no agent or weaving." The section "Test contexts wrap what they hand out" already describes the
  proxies.

- [ ] **Step 3: Add these four bullets to the `## Limits` section of the nofanout `CLAUDE.md`**, after "In-memory
  only":

```markdown
- **Stricter than N+1.** The rule is "no select twice in one call from the test", so two reads of one select with
  different arguments fail too. A Command that spans an external call or several transactions may re-read on
  purpose; that is what `@SuppressRepeatedSelect` is for.
- **It counts source reads, not repository calls.** A repository mock method must read its `TestEntitySource` once.
  A mock that reads twice in one method fails every caller, with a trace that names a single repository method.
- **Kernel-only stacks are not counted.** A read with no domain frame between the repository and the test, only
  `com.naturalist.data` classes (`AbstractEntityQuery`, `AbstractEntityCommand`, `Transaction`, `Pages`), is skipped.
  Those classes make single reads. A loop added to one of them is seen only through a domain caller.
- **The hook ships.** `framework-test` is compile scope in `apps/management-console`, so `SelectTrace.record` runs on
  every in-memory read in the running app. Unarmed, it is one `ThreadLocal` lookup.
```

- [ ] **Step 4: Verify.**

Run: `mvn -q -pl domains/usage/usage-core -am compile`
Expected: exit 0.

Run: `grep -rn "head-of-DAG" --include='*.java' domains kernels apps adapters`
Expected: no output.

- [ ] **Step 5: Stage.** `git add` the two files.

### Task 2: Pin the page-0 rule

**Files:**
- Modify: `kernels/framework-test/src/test/java/com/naturalist/test/query/nofanout/SelectTraceFixtures.java` (class `FooQuery`)
- Modify: `kernels/framework-test/src/test/java/com/naturalist/test/query/nofanout/SelectTraceTest.java`

**Interfaces:**
- Consumes: `AbstractEntityRepository.getPage(PageRequest)` (inherited by `FooRepository`),
  `Pages.stream(int, Function<PageRequest, Page<T>>)`, `PageRequest.of(int pageNumber, int pageSize)`.

- [ ] **Step 1: Add two methods to `FooQuery`** (`java_method_add`):
  `void firstPageTwice()` calls `foos.getPage(PageRequest.of(0, 10))` twice;
  `void everyPage()` runs `Pages.stream(1, foos::getPage).toList()`.

- [ ] **Step 2: Add two tests to `SelectTraceTest`**, beside `allowsABatchedRead`:

```java
@Test
void flagsTheFirstPageReadTwiceInOneCall() {
    query.firstPageTwice();
    assertRepeated("FooSource.getPage read 2 times");
}

/** Later pages continue one read: three one-row pages count once. */
@Test
void allowsWalkingEveryPageOfOneRead() {
    nte.getNamed(FooSource.class).insert(new Foo("q"));
    nte.getNamed(FooSource.class).insert(new Foo("r"));
    query.everyPage();
}
```

- [ ] **Step 3: Run.**

Run: `mvn -pl kernels/framework-test -am test -Dtest=SelectTraceTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: `Tests run: 24, Failures: 0, Errors: 0`. Both new tests pass on the current tracer; they pin the rule
rather than drive a change. If `flagsTheFirstPageReadTwiceInOneCall` reports a different select name, the message
names the outermost repository frame: correct the expected string, not the tracer.

- [ ] **Step 4: Stage.**

### Task 3: Feature search cleanup and the reuse-id pin

**Files:**
- Modify: `kernels/feature-search/src/main/java/com/naturalist/featuresearch/InMemoryFeatureSearch.java`
- Modify: `domains/insects/insects-core/src/main/java/com/naturalist/insects/InsectIdentificationCommandImpl.java` (javadoc of `resolveFeatures`)
- Test: `domains/insects/insects-core/src/test/java/com/naturalist/insects/InsectIdentificationCommandImplTest.java`

- [ ] **Step 1: `InMemoryFeatureSearch`.** Add `@Override` to `findSimilarToEach` (`java_annotation_add`). Tokenise
  each corpus feature once per call instead of once per query value: inside `findSimilarToEach`, map the loaded
  features to a private `record Tokenised<ID extends EntityId>(FeatureCorpus.Indexed<ID> feature, Set<String> tokens)`
  and change `closest` to `closest(List<Tokenised<ID>> features, Set<String> queryTokens, int limit)`. Results and
  ordering do not change.

- [ ] **Step 2: Run the existing search tests.**

Run: `mvn -pl kernels/feature-search -am test -Dtest=InMemoryFeatureSearchTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: all pass, including `searchesManyValuesInOneCorpusLoad`.

- [ ] **Step 3: Add the pin to `InsectIdentificationCommandImplTest`**, after the last case in the "reuse-aware
  feature resolution" block, lettered as the next case:

```java
/**
 * The model names an existing feature the search did not offer for any proposed value.
 * Reuse ids come from the offered candidates only, so the value is minted fresh.
 */
@Test
void identify_reuseTargetNotOffered_mintsNew() {
    var featureSource = nte.getNamed(InsectFeatureTestEntitySource.class);
    featureSource.insert(InsectFeature.of(InsectFeatureId.create(), "hovering flight pattern"));
    var unofferedId = InsectFeatureId.create();
    featureSource.insert(InsectFeature.of(unofferedId, "iridescent wing sheen"));
    long before = featureSource.entityStream().count();

    var turn2 = """
            {"resolutions":[
              {"proposed":"hovering flight","decision":"reuse","existingValue":"iridescent wing sheen"}
            ]}
            """;
    var exchange = twoTurn(SAMPLE_RESULT_JSON, turn2);

    buildCommand((image, tool, prompt) -> exchange).identify(ImageUpload.of(JPEG),
            new ImageMetadata("Chico, CA", null), NaturalistName.of("pat"), null);

    assertThat(featureSource.entityStream().count()).isEqualTo(before + 3);
    assertThat(nte.getNamed(InsectFeatureAssignmentTestEntitySource.class).entityStream()
            .map(OrganismFeatureAssignment::featureId).toList())
            .doesNotContain(unofferedId);
}
```

  "hovering flight pattern" makes turn 2 run; "iridescent wing sheen" shares no token with the three sample values,
  so it is never a candidate.

- [ ] **Step 4: Update the `resolveFeatures` javadoc** (`java_javadoc_change`): "an existing feature value present in
  the catalog" becomes "an existing feature value the search offered as a candidate".

- [ ] **Step 5: Run.**

Run: `mvn -pl domains/insects/insects-core -am test -Dtest=InsectIdentificationCommandImplTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: all pass. The new test passes on this branch; on `main` it would fail with `before + 2`, which is the
behaviour change D4 accepts.

- [ ] **Step 6: Stage.**

### Task 4: Completeness gate

- [ ] **Step 1: Full build.**

Run: `mvn verify`
Expected: `BUILD SUCCESS`.

- [ ] **Step 2: Architectural enforcement.** This installs the branch's snapshot jars into `~/.m2`.

Run: `mvn install -DskipTests && mvn rewrite:dryRun -Drewrite.failOnDryRunResults=true`
Expected: `BUILD SUCCESS`, no pending fixes or markers.

- [ ] **Step 3: The two greps from root `CLAUDE.md`** (the branch removed version entries from the root pom).

Run: `git ls-files -co --exclude-standard | xargs grep -nIiE '[a-z0-9]+[-_]api[-_]?key|sk-ant[-]'`
Expected: no output.

Run: `grep -rn "<version>" --include=pom.xml . | grep -v 1.0.0-SNAPSHOT | grep -v '\${'`
Expected: nothing outside the root `pom.xml`.

- [ ] **Step 4: Close out.** Set this plan's row in `docs/plans/README.md` to done and record the decisions D1–D4 as
  taken in the Decisions section above. Print the commit commands for Pat; do not commit.

## Not doing

- **Matching `isTestMethod` on the full method signature.** It matches by name today, while `suppressesRepeats`
  matches name and type. A mistake in a stricter match makes the gate go quiet with the build still green; the loose
  match can only widen what counts as a test frame. The inconsistency stays.
- **Making the unarmed path allocation-free.** One small `Scope` per thread is not worth a change to the tracer's
  lifecycle.
- **Stopping the stack walk at the test frame.** The measured cost is already below the AspectJ gate's.
- **`docs/work-tracker.md` "Last updated" line.** It has been stale since August across many efforts, not because of
  this change. Row 23 already names `SelectTrace`.
- **`FglOptimumGoldenMasterTest`.** Its read was hoisted out of a test loop; that is the documented answer for an
  object a context does not wrap.
