# Adapter hexagonal review — 2026-11-01

Read-only review of every adapter against the rule that adapters are thin layers
delegating to `<domain>-api` ports and `<domain>-core` implementations. Scope:
the eight console modules (driving adapters), `apps/management-console` as both
composition root and driving adapter, the ten `*-repository-rdbms` modules plus
`kernels/catalog-rdbms` (driven adapters), and the seven members of `adapters/`.
Sources were read through the Kaname engine against a fresh envelope; every
High finding below was re-read from source by the reviewer of record, not only
by the fan-out reviewers. Nothing was changed; no build or gate was run.

Severity key — **High**: a domain rule, decision, or use-case write path lives in
the adapter. **Medium**: the adapter collates, filters, sorts or dedups domain
data by a domain criterion, orchestrates several ports, or duplicates logic
that has no core/kernel home. **Low**: borderline or presentation-adjacent.

## Headline

| Surface | High | Medium | Low | Verdict |
|---|---|---|---|---|
| insects-console | 5 | 9 | 5 | the bulk of the debt |
| plants-console | 1 | 5 | 4 | same shapes as insects, fewer of them |
| management-console (app) | 1 | 2 | 3 | registration + login composed in the app |
| library / chemistry / soil / garden / usage consoles | 0 | 4 | 5 | small, but two textbook N+1s |
| `*-repository-rdbms` + catalog-rdbms | 0 | 1 | 3 | essentially clean |
| `adapters/*` | 0 | 0 | 1 | clean |

The console modules carry the overwhelming majority of the violations, and
they cluster into six shapes rather than forty unrelated slips. Fixing the
shapes (a port or factory each) clears most rows at once.

## The six shapes

### 1. Write-side use cases executed inside controllers

| Where | Severity | What | Home |
|---|---|---|---|
| `domains/insects/insects-console/.../InsectsController.java:121-135` | High | Constructor builds `InsectCatalogIdentificationTransaction`, `InsectIdentificationCommand`, `InsectAddPhotoTransaction`, `InsectAddPhotoCommand` and an `InMemoryFeatureSearch` with `new`. All four core classes are package-private; the console reaches them only through the split package. The console is acting as a composition root. | Public `*Command` ports in insects-api with `@DomainService` impls in core; the FeatureSearch adapter wired in the app. |
| `InsectsController.java:921-942` (`reIdentify`) | High | Reads an observation, `withSubject(newRank)`, writes it back, and leaves a `// TODO: update linked images' parentName` — knowingly breaking the image-parent invariant the catalog-identification transaction enforces. No ownership check. | A re-identification transaction in insects-core behind a command port, owning subject change + image re-parenting + ownership. |
| `InsectsController.java:822-839` (`observe`) | High | Builds `new OrganismObservation<>(InsectObservationId.create(), me, InsectSpeciesName.of(name), Instant.now(), …)` and inserts through the raw entity command. Identical construction already lives in `InsectAddPhotoCommand.addPhoto` (core), so the rule exists twice. Hard-binds species although observations are rank-polymorphic. | A record-sighting command in core (or a no-photo overload of add-photo). |
| `InsectsController.java:855-896` (`updateNotes`, `owns`) | Medium | "Only the recording naturalist may edit notes" implemented as a static in the adapter around get → `withNotes` → update. | An update-field-notes command in core that owns the check. |
| `InsectsController.java:472-509` (`identify`), `:841-853` (`addImage`) | Medium | File is stored to disk *before* the use case runs; on budget/rate rejection the file is orphaned. Builds `vision.Image` with a hard-coded `image/jpeg` media type. `ImageStorageService` (20 MB cap, JPEG/PNG/WebP whitelist) has no port, so core trusts whatever `FileName` it is handed. | An image-store port in `kernels/observation` consumed by the core command, so the command orders store-then-persist and owns acceptance policy. |
| `apps/management-console/.../account/RegistrationTransaction.java:50-145` | High | A bespoke `@Component` composes two domains (`accountCommand.register` then `naturalistCommand.create`) and is the only place holding the password floor/ceiling (8 chars / 72 bytes) and the default `VISITOR`/`CURIOUS` for a self-registrant. Not built on the kernel `Transaction<A>` template that every core transaction uses. The design doc (`docs/plans/2026-08-30-accounts-auth-redesign-design.md` L131-138) placed it here to avoid "a cross-domain core dependency", but the dependency needed is naturalists-core → accounts-**api**, which DAG rule 2 allows. | Composition + role/stage default → naturalists-core (`NaturalistCommand.register(...)` or a transaction); password policy → accounts-api (a constraint on the raw password, with the encoder injected into accounts-core). |
| `apps/management-console/.../account/RegistrationController.java:91-130, 245-270` | Low | Three ordered side effects after the port call (verify URL, email body, auto-login) and `invalidDetailsMessage` rebuilds the password rule by string-matching constraint names. `register.jte:122-123` mirrors `minlength="8"`. | Dissolves with the row above; a `VerificationEmailer` collaborator keeps the controller to one call. |

### 2. Read-model assembly and multi-port joins in controllers

| Where | Severity | What | Home |
|---|---|---|---|
| `InsectsController.java:511-571` (`list`) | Medium | Collates species page → images (`forParentNames`) → roles (`getByParentNames`, partitioned by `instanceof`) → genera → families, then applies the "my collection" membership rule by filtering the *page* against `observations().forNaturalist(me)` — pages go sparse. | A species-card read model from a core factory; `SpeciesQuery.collectedBy(NaturalistName, PageRequest)` for the lens. |
| `InsectsController.java:754-801` (`detail`), `:205-224` (`observationLookup`) | Medium | Filters `Insect.observations()` to the viewer's observation ids, derives `collected`, pairs each image with its observation by linear search, and fetches `forNaturalistAndSubjects` twice. | A viewer-aware `InsectFactory` build, or a collection query answering `collected`/`myImages` in one call. |
| `InsectsController.java:944-966` (`lifeStages`) | Medium | Walks species→genus→family→order with four `getByName` calls; `insectQuery.getByName(species)` already returns the composed `Insect`. | The existing port. |
| `apps/management-console/.../auth/NaturalistUserDetailsService.java:64-107` | Medium | Joins `accountQuery.getByEmail` and `naturalistQuery.byAccount` in the app, decides an account with no naturalist cannot log in, derives vision/locked from `AccessLevel`/`AccountStatus` enums. `naturalists/CLAUDE.md` documents this join as the current design. | A login-identity read model from a naturalists-core query (naturalists-core may import accounts-api); at minimum `Account.hasVision()`/`isLocked()` so the enum tests are not re-derived. |
| `domains/plants/plants-console/.../PlantsController.java:98-109` (`species`) | Medium | Loads *every* ecological role via `Pages.stream(1000, …)` into a `Map<String, …>` keyed by raw slug to join against one species page. `EcologicalRoleQuery` lacks the batched `forPlantNames(Set)` the domain rules require. | Batched method on `PlantQuery.EcologicalRoleQuery`, or a list read model in plants-core. |
| `domains/soil/soil-console/.../SoilsController.java:52-60` (`profiles`) | Medium | `getBySoilProfileName(...).orElseThrow()` once per row of the info page — an N+1 inside a stream map. Console modules sit outside both N+1 gates. | `SoilProfileQuery.findPage(PageRequest)` assembled by `SoilProfileFactory`. |
| `domains/garden/garden-console/.../GardenController.java:40-55` (`plantedZones`) | Medium | Derives "a planted zone exists iff some planting names it" from the first page of plantings, `distinct()`, then one aggregate assembly per zone (N+1); zones beyond page 0 silently vanish. | `PlantedZoneQuery.findPage` in garden-core; `PlantingCollection.zones()` for the distinct set. |
| `domains/chemistry/chemistry-console/.../ChemistryController.java:76-79` (`detail`) | Medium | Loads the whole element catalog to compute "is this constituent element catalogued" as a `Set<String>` for the template. `ElementQuery.findByNameSet(Set<ElementName>)` already exists and is bypassed. | The existing batched port, or `Catalog.findBySlug` through the linker as `NutrientChemistryLinks` does. |
| `apps/management-console/.../search/SearchController.java:27-31, 44-62` | Low | Drops hits whose link resolves null while still reporting `results.size()`; hard-codes `DomainId → display name` for three domains. | Display name on `DomainId` (catalog kernel). |

### 3. Clade and placement knowledge held by consoles

| Where | Severity | What | Home |
|---|---|---|---|
| `domains/insects/insects-console/.../InsectCladeAnchors.java:28-47, 63-73` | High | A hand-curated `OVERRIDE` map (`battus→troidini`, `apis→anthophila`, `drosophila-melanogaster→sophophora`, …) plus a lineage walk picking the deepest mapped clade. Every placement already exists on the entities as `placedIn` (asserted by `ParaphylyPlacementMonotonicityTest`), and core already has the walk (`InsectLifeStages.stagesOf` does `firstNonNull(species.placedIn(), genus…, family…, order…)`). Keyed on slug strings, it will drift from catalog data; plants reads the same fact off its read model. | `Insect.placedIn()` on the read model, populated by `InsectFactory`; the console maps `Clade → CladeView` only. |
| `InsectsController.java:373-416` (`*PlacedAt` ×4); `PlantsController.java:338-343` (`ordersPlacedAt`) | High / Medium | Full-table scans (`Pages.stream(1000, …)`) filtered by `clade.equals(x.placedIn())`, sorted, species joined to genus for the binomial. No port filters by clade. | `placedIn(Clade)` on the rank queries (repository-indexed), returning the existing collections. |
| `PlantsController.java:167-174, 191-203` (`catalogCladeRoot`, `lowestCommonAncestor`) | High | Streams every order, walks `CladeTraversal.ancestry` of each `placedIn`, and computes the lowest common ancestor on every landing-page request. A phylogenetic fact derived in a controller. | `CladeTraversal.lowestCommonAncestor(List<Clade>)` in `kernels/clades`, exposed via a plants-core query. |
| `domains/plants/plants-console/.../PlantCladeTree.java:24-47`; `insects/.../CladeNav.java:21`; `InsectsController.java:346` | Medium | `isPlant`/`isAnimal` via `instanceof Plantae/Animalia` over the ancestry; `narrower` via reflective `CladeCatalog.childrenOf` per node from a template — while `CladeView.children()` is already fetched. | `kernels/clades` (`descendsFrom`), children from the library read model. |
| `InsectsController.java:233-236, 248-275` (`CLADE_RANK_LABEL`) | Medium | Private map `animalia→Kingdom, arthropoda→Phylum, insecta→Class` re-encodes clade↔rank; `CladeStep.rank()` from the library port already answers it. | Derive labels from `CladeStep.rank()`. |
| `domains/library/library-console/.../CladesController.java:45-47`, `.../CladeRankLinks.java:34-36`, `InsectCladeAnchors.FLOOR` | Medium | `"insecta"` hard-coded as the insects catalog root in three places. | Resolve through `Catalog.findBySlug`/the insects `EntityRefLinker`, or a `CladeView` attribute set by library-core. |
| `PlantsController.java:262-271` (`cladeTrailFor`), `:313-331` (`cladeDetail`); `CladesController.java:80-95` | Low | "Plant clade resolves through the order" encoded in the controller; clade description/children pulled from the kernel after the `CladeView` was already fetched. | `Plant.placedIn()`; carry description/children on `CladeView`. |
| `insects/.../cladeTrail.jte:20-21`; `plants/.../cladeTrail.jte:25` | Low | Templates re-resolve `Clade.of(step.cladeSlug())` and ask the catalog for children. | Carry children per step in the trail view model. |

### 4. Domain ordering and selection decided in presentation

| Where | Severity | What | Home |
|---|---|---|---|
| `InsectsController.java:771-773, 955-957` | Medium | Life stages sorted by `kind().ordinal()`. The domain owns the developmental sequence (`InsectLifeStages.stagesOf` → `Metaboly.stages()`, whose javadoc claims it is wired into `detail` — it is not). | `InsectFactory` orders `Insect.lifeStages()`; or `LifeStageCollection` defines order. |
| `InsectsController.java:190, 315, 410-412, 442`, `detail.jte:43`; `PlantsController.java:226-254` | Medium | Binomial assembled as `genus + " " + epithet` in five insects sites and the plants breadcrumb, which also re-derives the subject rank from nulls although `Plant.identifiedTo()` exists. | `binomial()`/`scientificName()` on the read models; `Plant.lineage()`. |
| 9 insects templates (`detail/family/genus/order/list/families/genera/orders/life-stages.jte`); 8 plants templates | Low | `commonNames().stream().findFirst().map(label).orElse(slug)` — "primary common name" chosen nondeterministically from a `Set`. | `displayName()` on the rank records. |
| `InsectsController.java:742-744`; `PlantsController.java:119-127, 402-404`; library `Concepts/Glossary/CitationsController` | Low | Name/title sorts in the adapter while `PlantFactory` sorts siblings in core; library loads `MAX_PAGE_SIZE` and sorts, defeating paging. | Ordered collections or ordered `findPage`. |
| `garden/plantedZone.jte:44-48`; `usage/.../UsageController.java:76-86` | Low | "still growing" from `removedDate == null` although `Planting.isActive(asOf)` exists; gauge bands restate the WARNING/HARD_STOP policy. | Existing predicate; `UsagePolicy.band(...)`. |
| `insects/.../stageFacts.jte:100` | Low | Renders `getClass().getSimpleName()` of a sealed type as the user-facing value. | A label on the type. |

### 5. Duplicated console helpers signalling a missing kernel/core home

| Where | Severity | What | Home |
|---|---|---|---|
| `plants/.../BreadcrumbSegment.java` ≡ `insects/.../BreadcrumbSegment.java` | Medium | Identical record and factories ("a direct copy" per its javadoc). | One shared console kernel/helper module. |
| `plants/.../PlantRankLabel.java:9-12` ≡ `insects/.../RankLabel.java:11-14` ≡ inline in `library/.../clades/detail.jte:18-19, 27-28` | Medium | Same capitalisation of `LinealRank.name()` three times. | `LinealRank.label()` in `kernels/taxonomy`. |
| `plants/.../PlantImageStorageService.java` ≡ `insects/.../ImageStorageService.java` | Medium | Identical magic-byte detection, 20 MB cap, UUID naming, traversal guard; both `new`-ed with a hard-coded `Path`; plants `store()` is dead. | One filesystem adapter under `adapters/` behind a kernel image-store port (see shape 1). |
| `apps/management-console/.../catalog/CompositeEntityRefResolver.java:42-87` ≡ `library-repository-test/.../CompositeEntityRefResolverMock.java:32-80` | Medium | Near line-for-line duplicate with divergent edge behaviour (empty list accepted vs rejected; `IllegalStateException` vs `IllegalArgumentException`). | One composite in library-core, used by both the app bean and the mock stack. |
| `library-console` `GlossaryLinker` consumed by soil-console and insects-console at compile scope | Low (structural) | Console → console dependency outside the stated DAG; the matching rules themselves are presentation. | A shared console-support module. |

### 6. Driven adapters (persistence, kernel adapters, `adapters/`)

| Where | Severity | What | Home |
|---|---|---|---|
| `kernels/catalog-rdbms/.../RdbmsCatalog.java:40-70` and `kernels/catalog-inmem/.../InMemoryCatalog.java:128-162` | Medium | Both adapters independently keep the best `MatchKind` per ref and sort by kind ordinal then slug; `Catalog.search` javadoc calls that ordering "contract common to every adapter" yet `SearchResults.of` enforces nothing. The trigram tiebreak and 2-char floor are declared adapter-defined and are fine. | A ranking/dedup step in `kernels/catalog` beside `ReferenceRouting`; adapters return raw `(ref, kind, token, score)` rows. |
| `naturalists-repository-rdbms/.../NaturalistMapper.java:44`, `NaturalistEntityRepositoryRdbms.java:92-98`; mock `NaturalistEntityRepositoryMock.java:19-26` | Low | `getByAccount` is `… LIMIT 1` with no `ORDER BY` (mock: `findFirst()`); `account_name` is not unique, so two adapters silently pick an arbitrary row. | A UNIQUE on `account_name` (DDL + `@DboSchema`), or a `List` return. |
| `accounts-repository-rdbms/.../AccountEntityRepositoryRdbms.java:91-97`; mock `AccountRepositoryMock:23-30`; app `NaturalistUserDetailsService:77-83` | Low | Both adapters throw on a malformed email; the app catches that to turn it into "not found". | `getByEmail(EmailAddress)` so the format rule lives once in the value type. |
| `insects-repository-rdbms/.../lifestage/InsectLifeStageDbo.java` `toEntity`; `InsectLifeStageEntityRepositoryRdbms.java:116-147` | Low | Empty `layers` child set is read back as `null`, collapsing "characterised, empty" into "not characterised" — a documented distinction on `HabitatProfile`. | `HabitatProfile.invariants()` forbidding empty `layers`, or a presence column. |
| `adapters/anthropic-vision/.../AnthropicVisionService.java:217-223` | Low | The vendor adapter writes the user-turn text itself ("Identify this insect. Location: …"); the port only lets the caller supply the system prompt. Only `InsectIdentificationCommand` calls it today, so nothing is wrong yet, but a generic `kernels/vision` port should not let the adapter choose domain wording. | A user-prompt parameter on `VisionService.identify`, built by the command. |

Observation, not a violation: `UsageAlertMapper.selectUnsent` orders `at DESC` while the mock returns fixture order and the port is silent.

## Checked and clean

- `adapters/resilience-resilience4j`, `mail-spring` (its swallow-on-transport-failure policy is the `EmailSender` port's stated contract), `spring-runtime`, `spring-test-data`, `spring-console-test`, `anthropic-text-generation` (aside: annotated `@Resilient` but never wraps its HTTP call in the facade, unlike the vision adapter — a resilience-policy gap, not a hexagonal one).
- All ten `*-repository-rdbms` modules beyond the four Low rows: DBO column invariants, SQLState translation, name↔id nested-select/JOIN, batched child assembly, null-group VO reconstruction, paging arithmetic — all acceptable persistence mapping. `InsectRankKey`, `CitationSubjectKey`, `ObservationSubjectKey`, `PlantFeatureAssignmentRankKey` are pure MyBatis parameter shapes. Seeds are FK-order replay only.
- management-console: alert dispatch/emailer, usage/account/email/resilience configuration, `ResilienceNameValidator`, `LoginRateLimitFilter`, `AuthenticationEventObserver`, principal/current-naturalist plumbing, security/web/vision/JTE configuration, catalog assembly, the composite linker, admin controllers, verify flow (token validity and status rules confirmed in `AccountCommandImpl`), all templates.
- Consoles: plants rank detail pages read one `plantQuery.getByName` and unpack the `Plant` read model without re-collation; `NutrientChemistryLinks` delegates to `Nutrients.chemistryOf`; chemistry product detail uses the batched port; soil/garden/usage templates use read-model predicates; HEIC conversion and caching are infrastructure.

## Where to start

Three moves clear most rows: (1) give insects public command ports and a viewer-aware factory build, which retires shape 1 and most of shape 2 in `InsectsController`; (2) add `placedIn(Clade)` rank queries and `Insect.placedIn()`/`Plant.placedIn()`, which retires the override map, the five catalog scans and the LCA walk; (3) move registration into naturalists-core and the password rule into accounts-api. The persistence layer needs no structural work.

## Caveats

- Line numbers are from the current working tree at review time.
- Two findings are documented, deliberate placements (`RegistrationTransaction` in the app per the auth design doc; the app-side login join per `naturalists/CLAUDE.md`). They are reported because the stated justification — avoiding a cross-domain core dependency — does not hold under DAG rule 2.
- Whether `NoSelectInIteration` and the runtime N+1 gate are intended to exclude `*-console` was not checked; the soil and garden fan-outs would fail either gate inside core.
- Child DBOs and some mappers were triaged by sibling pattern plus a regex sweep for policy-shaped SQL rather than opened individually.
