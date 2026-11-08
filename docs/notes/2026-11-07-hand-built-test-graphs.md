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

`InsectFactoryTest`'s two sites (a `@BeforeEach` factory over a stub `InsectLifeStageQuery`, and a second
factory per test over a stub returning given stages) are gone (2026-11-07). `InsectsTestContextInternal` now
provides `InsectFactory`, which is no `@DomainService`, from the graph's queries, the real life-stage query
included. The ordering test inserts its stages into `InsectLifeStageTestEntitySource` instead of stubbing. The
stub had been hiding stages that failed the `LifeStage` invariants (no phenology, no habitat).

## Does this argue for a `com.naturalist.Enforce*` recipe?

Here is the evidence. That decision is Pat's.

After the refactor, every remaining site in these two modules is one of three shapes:

- **A constructor null-rejection test** (16 sites).
- **An anonymous override** (1 site), plus the impl built over it (1 site).
- **A second instance of a type in the same test** (1 helper).

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

When this was written the other domains' core tests still hand-wired throughout. Every domain whose
core has code is now converted (sections below), so what a recipe would flag is the shapes listed here, plus each
domain's own "still hand-built" line.

## Plants (converted 2026-11-07)

plants-core now has six registered contexts, one per package: `PlantsTestContextInternal` and the
`catalog`, `cultivar`, `heritage`, `management` and `phytochemistry` `*TestContextInternal` classes. They ship
in plants-core's test-jar. Only the root context declares a provision: `PlantFactory`, which is no
`@DomainService` (production builds it inside `PlantQueryImpl`). All six test classes moved to `@Wired`
fields:

- `PlantFactoryTest` used to build the factory from ten hand-wired parts, three of them anonymous stubs
  (`CultivarQuery`, `PlantProgramQuery`, `PhytochemicalConstituentQuery`) that returned empty. It now
  `@Wired`s the context's factory over the real queries. A new test pins the species extras the stubs had
  hidden: `aristolochia-californica`'s seeded constituents.
- `PlantAncestryResolverTest`, `PlantFeatureQueryImplTest`, `PlantsCatalogContributionTest`,
  `PlantsEntityRefResolverTest` and `phytochemistry/PlantsCompoundReferencesTest` lost their hand-built
  impls and mocks.

The `plants-test-context` facades (`PlantsTestContext` and the four sub-context `*TestContext` factories)
keep their public API and are now facades over `ObjectGraph.create(db)`.

What is still hand-built in plants-core: the five constructor null-rejection tests (four in
`PlantsCatalogContributionTest`, one in `PlantsCompoundReferencesTest`). The newly traced graph found no
N+1, and no fixture data broke an invariant.

## Usage (converted 2026-11-07)

usage-core now has one registered context, `UsagesTestContextInternal`, shipped in usage-core's test-jar
with its services entry. The graph builds `UsageQueryImpl`, `UsageCommandImpl`, `IdentificationBudgetImpl`,
`NoOpEntitlementLookup` and the three repository mocks by reflection. The context provides only what
production takes from the composition root (`UsageConfiguration`):

- `WarningPercent`: 80 by default; `warningPercent(int)` chooses another before the first `get` (the
  usage-console slice needs 1).
- `Clock`: an outside-world port, `Clock.systemUTC()` by default; tests declare `@Fake Clock` for a fixed
  instant.
- `EntitlementLookup`: production's `NoOpEntitlementLookup` unless `entitlements(...)` chooses who is entitled.

The hand-wired `UsageCoreTestContext` is deleted. `UsageCommandImplTest` and `UsageQueryImplTest` take the
command, query, budget and repositories as `@Wired` fields and the fixed clock as `@Fake`. The seam test
that built `new IdentificationBudgetImpl(command)` now `@Wired`s the budget. The entitled-naturalist
scenario moved to `UsageCommandEntitlementTest`, which chooses the entitlement through the context and takes
the command from `nte.get` (a `@Wired` command would be built before the test could choose). It also now
proves a non-entitled naturalist is still refused under the same zero cap.

The `usage-test-context` facade `UsagesTestContext` keeps `create(db, warningPercent, clock)` and its three
accessors, and is now a facade over `ObjectGraph.create(db)`: it sets the warning percent on the context and
hands the caller's clock over as the graph's `Clock` fake. `identificationBudget()` is now traced too.

What remains: `EntitlementLookup` lives in `com.naturalist.usage`, which has a context, so a test cannot
`@Fake` it; the context's `entitlements(...)` choice is the one stand-in for a domain-package type in usage.
It stays until a real entitlement (the credit-balance check `accounts` is to own) exists to run instead. No
constructor null-rejection tests exist in usage-core. The newly traced graph found no N+1, and no fixture
data broke an invariant.

## Chemistry (converted 2026-11-07)

chemistry-core now has four registered contexts, one per package with classes to build:
`catalog/ChemistryCatalogTestContextInternal`, `compound/CompoundTestContextInternal`,
`element/ElementTestContextInternal` and `product/ProductTestContextInternal`. None declares a provision:
every query impl, both catalog classes and every repository mock is a `@DomainService` /
`@MockDomainService` the graph builds by reflection. They ship in chemistry-core's test-jar. The three
hand-wired `*QueryTestContextInternal` helpers are deleted. Six test classes moved to `@Wired` fields:
`ChemistryCatalogContributionTest`, `compound/CompoundQueryImplTest`, `compound/CompoundEntityQueryImplTest`,
`element/ElementQueryImplTest`, `product/ProductQueryImplTest` and `product/ChemistryCompoundReferencesTest`
(`ElementQueryImplTest` and `ProductQueryImplTest` also take the package-private repository port as a
`@Wired` field, since they read it directly).
`CompoundQueryImplTest`'s `isSameAs` checks still hold: the impl is built over the graph's traced
sub-queries, and the trace hands those instances back as they are.

The `chemistry-test-context` facades (`ChemistryTestContext` and the `CompoundTestContext`,
`ElementTestContext` and `ProductTestContext` factories) keep their public API and are now facades over
`ObjectGraph.create(db)`. Plants needed no change: it reaches chemistry only through the `CompoundName`
identifier.

What is still hand-built: seven constructor null-rejection tests (three in `ChemistryCatalogContributionTest`,
three in `CompoundQueryImplTest`, one in `ChemistryCompoundReferencesTest`). The newly traced graph found no
N+1, and no fixture data broke an invariant.

## Soil (converted 2026-11-07)

soil-core now has two registered contexts: `SoilsTestContextInternal` (rewritten from the hand-wired class of
that name) and `observation/SoilObservationTestContextInternal`. Neither declares a provision.
`SoilProfileFactory` and `SoilProfileQueryImpl` already carry `@DomainService` in production (the factory is
injected into the query like any collaborator), so the graph builds them as Spring does; no production
annotation changed. The five `observation/*Queries` wiring seams are deleted from both soil-core's tests and
soil-test-context: they existed only so the parent package could hand-wire the observation sub-context, and
nothing else used them. Five test classes moved to `@Wired` fields: `FglOptimumGoldenMasterTest`,
`SoilProfileFactoryTest`, `SoilProfileViewTest`, `SoilProfileInfoQueryImplTest` and
`observation/LabAnalysisInfoQueryImplTest`.

`SoilTestContext` keeps `create(db)`, `soilProfileQuery()` and `soilProfileInfoQuery()` and is now a facade
over `ObjectGraph.create(db)`. Nothing in soil-core is hand-built any more. The newly traced graph found no
N+1 (the factory's per-analysis fan-out was already batched), and no fixture data broke an invariant.

## Garden (converted 2026-11-07)

garden-core now has one registered context, `GardenTestContextInternal`, rewritten from the hand-wired class
of that name, with no provisions: `PlantingQueryImpl`, `PlantedZoneFactory` and `PlantedZoneQueryImpl` are all
`@DomainService` in production. `PlantedZoneFactoryTest` takes its `PlantedZoneQuery` as a `@Wired` field.
`GardenTestContext` keeps `create(db)`, `plantingQuery()` and `plantedZoneQuery()` and is now a facade over
`ObjectGraph.create(db)`. Nothing in garden-core is hand-built. No N+1, no invariant failure.

## Accounts (converted 2026-11-07)

accounts-core now has one registered context, `AccountsTestContextInternal`, shipped in accounts-core's test-jar
with its services entry. The graph builds `AccountQueryImpl`, `AccountCommandImpl` (both `@DomainService`)
and the two repository mocks by reflection. The context provides what production takes from the composition
root (`AccountConfiguration` and the shared clock):

- `SecureTokens`: production's `SecureTokens.jdk()`. The test used a deterministic fake (`raw-token-N` /
  `hash:raw`); no test depended on the values, so it now reads a token's hash back through the graph's
  `SecureTokens`. `SecureTokens` lives in `com.naturalist.account`, which has a context, so it could not be
  `@Fake`d anyway.
- `AccountPolicy`: `selfServe()`, the default of `naturalist.access.grant-policy`, unless `policy(...)` chooses
  another before the first `get`.
- `Clock`: an outside-world port, `Clock.systemUTC()` by default.

`AccountQueryImplTest` `@Wired`s its query. `AccountCommandImplTest` `@Wired`s the two repositories and
`SecureTokens`, `@Fake`s the clock with a small `MovingClock` the test advances, and takes the command from
`nte.get`, because three tests choose the request policy first (a `@Wired` command would be built before they
could). The expired-token cases used to build a second command over a later clock; they now advance the one
clock instead, so one command sees the token minted and then expired.

The decision-metric tests read `AccountCommandImpl.<method>.<predicate>` counters by exact name. A graph-built
command is a traced `AccountCommandImpl$ByteBuddy$…` subclass; the graph marks it synthetic and `Observer`
names a synthetic class for the class it extends, so metric names are the production class's in every
graph-built test, not only here.

No `EmailSender` appears in accounts-core: email delivery is the app's `RegistrationTransaction` and the
`adapters/mail-spring` sender, outside this module. No accounts-test-context facade was added: no test
outside accounts-core builds accounts objects (the console tests get `AccountQuery` / `AccountCommand` from
Spring over the `@MockDomainService` doubles). Nothing in accounts-core is hand-built. The newly traced graph
found no N+1, and no fixture data broke an invariant.

## Naturalists (converted 2026-11-07)

naturalists-core now has one registered context, `NaturalistsTestContextInternal`, with no provisions:
`NaturalistQueryImpl` and `NaturalistCommandImpl` are `@DomainService` and the repository mock is a
`@MockDomainService`. It ships in naturalists-core's test-jar. `NaturalistCommandImplTest` `@Wired`s the
command and the repository port. No naturalists-test-context facade was added (the console tests get
naturalists from Spring). Nothing is hand-built, no N+1, no invariant failure.

The `*-repository-rdbms` integration tests of both domains still build their adapters directly; the graph
is for core tests only.

## Management console (2026-11-07)

ByteBuddy is excluded from management-console's compile-scope `framework-test` so it stays out of the jar
(`PackagedJarIT`). It was already on the test classpath through `spring-boot-starter-test` (Mockito). The
pom now declares `usage-test-context` and `net.bytebuddy:byte-buddy` at test scope, so the plain unit tests
build a usage graph while the exclusion and `PackagedJarIT` stay as they were.

Converted:

- `NaturalistHeaderInterceptorTest`: three anonymous `UsageQuery` stubs became the real query from
  `UsagesTestContext` over a `NaturalistTestExtension`. A test that needs a pending alert inserts one into
  `UsageAlertTestEntitySource` (the seeded catalog is empty). The two non-admin tests used a stub that failed
  if `activeAlerts()` was called (the interceptor promises not to pay for it on a non-admin's behalf). They
  now prove both halves with an alert pending: no leak, and `UsageAlertTestEntitySource.reads()` stays zero.
  The admin test asserts it is positive, so the zero is not vacuous.
- `AlertDispatchJobTest`: the `ClaimOnceCommand implements UsageCommand` stub became the real command over
  one inserted unsent alert. "Email once" is now proved through real state: the second dispatch finds the
  alert marked emailed. `CountingEmailSender` stays (the mail sender is the outside world).
- `IdentifyRateLimitFilterTest`: the anonymous recording `IdentificationBudget` became the real usage budget.
  Refusals are read back from the usage event log (`identification-refused` events).
- `NaturalistLoginWebMvcTest`: the two `@MockitoBean`s (`AccountQuery`, `NaturalistQuery`) and Mockito are
  gone. The `@SpringBootTest` already runs the real queries over the `@MockDomainService` doubles. Each test
  rewrites the fixture account `gerald.durrell@oakvista.example` (linked to the fixture naturalist
  `patrick-way`) with its own password hash, access and status in the shared database, and restores it
  afterwards.

Left as is, because none stubs a domain query, command or transaction:

- `IdentifyRateLimitFilterTest`'s anonymous `CurrentNaturalist`: an app-level port over the security
  context, not a domain service.
- `TransactionProxyReachesDoExecuteTest.RecordingTransaction`: a minimal subclass of the kernel `Transaction`.
  The kernel class's proxying is the subject of the test.
