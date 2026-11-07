# Hand-built test graphs left after the dynamic-test-context refactor

**Date:** 2026-11-07 · **Scope:** `domains/insects/insects-core/src/test`, `domains/library/library-core/src/test`
· **Plan:** [`../plans/2026-11-07-dynamic-test-context-plan.md`](../plans/2026-11-07-dynamic-test-context-plan.md) Task 5

The refactor converted every test that built what its context builds. Seventeen test classes moved to `@Wired`
fields (and, in `InsectIdentificationCommandImplTest`, per-method fakes handed to `InsectsTestContextInternal`).
A new context, `com.naturalist.insects.catalog.InsectsCatalogTestContextInternal`, lets the graph build
`InsectsCatalogContribution`. What is left is listed below.

Found with:

```bash
grep -rnE 'new [A-Z][A-Za-z]*(Impl|RepositoryMock|Transaction|Resolver|Commands)\(' \
  domains/insects/insects-core/src/test domains/library/library-core/src/test
```

That gives 15 lines. Two more kinds of construction fall outside the pattern and are listed after them.

## Sites the pattern finds

| File:line | What it builds | Why the context cannot build it |
|---|---|---|
| `insects/InsectQueryImplTest.java:60, 67, 74, 81, 88, 95, 102, 111, 120, 129, 138` | `InsectQueryImpl` with one argument (or all ten) `null`; the other arguments come from `@Wired` fields | Constructor null-rejection test. The graph never passes `null`. |
| `insects/lifestage/InsectLifeStageQueryImplTest.java:32` | `InsectLifeStageQueryImpl(null)` | Constructor null-rejection test. |
| `insects/InsectPhotoCommandImplTest.java:130` | An anonymous `InsectAddPhotoTransaction` subclass whose `doExecute` throws | The graph builds declared classes only. An anonymous override is not one. |
| `insects/InsectPhotoCommandImplTest.java:136` | `InsectPhotoCommandImpl(store, throwing)` over that subclass | A per-method fake of `InsectAddPhotoTransaction` would need to come before the first `get` of `PhotoCommand`, and the class `@Wired`s `PhotoCommand` for its other five tests. This one test needs a second `PhotoCommand` beside the wired one. |
| `insects/InsectIdentificationCommandImplTest.java:538` | `handBuiltCommand(...)`: an `InsectIdentificationCommandImpl` over a given `Resilience` and `IdentificationBudget`; every other argument comes from `nte.get(...)` | `identify_eachRefusal_recordsTheNaturalist_andObservesAWarnViolation` needs four commands in one test, each over a different resilience or budget. One test has one graph, and the graph builds a type once, so the first command comes from the graph and the other three are built by hand. This is a genuine limit of one graph per test. (Since ruling R11 a test's own requests go through its package's context, so the helper now takes the text generator, feature search, authority and image store from the graph instead of building them.) |

## Sites outside the pattern

| File:line | What it builds | Why the context cannot build it |
|---|---|---|
| `insects/InsectsCatalogContributionTest.java:40, 47, 54, 61` | `InsectsCatalogContribution` with one query `null` | Constructor null-rejection test. The contribution itself is now `@Wired`. |
| `insects/InsectFactoryTest.java:85` (`@BeforeEach`) | `InsectFactory` over the graph's queries plus a stub `InsectLifeStageQuery` | `InsectFactory` is neither a `@DomainService` nor a `@MockDomainService`: production builds it inside `InsectQueryImpl`'s constructor. The stub life-stage query is the test's own, too. |
| `insects/InsectFactoryTest.java:283` (`factoryWithStages`) | A second `InsectFactory` per test, over a stub that returns given stages | The same, plus a second instance in one test. |

## Does this argue for a `com.naturalist.Enforce*` recipe?

Here is the evidence. That decision is Pat's.

After the refactor, every remaining site in these two modules is one of four shapes:

- **A constructor null-rejection test** (16 sites).
- **An anonymous override** (1 site), plus the impl built over it (1 site).
- **A second instance of a type in the same test** (1 helper, plus the second `InsectFactory`).
- **A class the graph cannot build by design** (`InsectFactory`).

None is a test re-deriving the context's own wiring, which is what a recipe would exist to stop.

A recipe would therefore need a precise exemption rule. That rule is not clean today:

- **Null-rejection tests are recognisable**: a `new X(` inside `assertThatThrownBy` with a literal `null` argument.
- **The override and second-instance cases are not.** They look like any hand wiring, so they would need a per-site
  marker or suppression.

One gap in the graph produced the second-instance workaround: there is one graph per test, with no way to ask
for a second, differently-faked instance. (A second gap, that a test could not `get` a port another package's
context provides, was closed by ruling R11: a test's `@Wired` fields and `nte.get` calls now go through the
context registered for the test class's own package.)

If the second-instance gap also closed, the only exemption left would be null-rejection.

A recipe keyed on `new *Impl(` / `new *RepositoryMock(` outside `assertThatThrownBy` would then flag only the
anonymous-override shape.

The other domains' core tests (accounts, chemistry, plants, soil, usage and others) still hand-wire
throughout. A recipe armed now would fail on them until each domain gets its own context.
