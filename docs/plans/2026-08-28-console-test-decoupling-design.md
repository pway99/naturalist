# Console Test De-scan + Naturalist Persistence Swap — Design

**Date:** 2026-08-28
**Status:** Design (refines the brief; ready for an implementation plan)
**Refines:** [`2026-08-28-console-restructuring-repository-implies-console-design.md`](2026-08-28-console-restructuring-repository-implies-console-design.md)
(the rough hand-off brief). Where this doc and the brief disagree, this doc wins.

## One-line goal

Stop management-console's integration tests from loading the whole-app
`DomainServiceScan`, so the naturalists domain can swap its in-memory mock repository
for the real Postgres/MyBatis rdbms adapter without forcing a database dependency onto
unrelated tests. Two coordinated moves: **swap naturalists' persistence marker** and
**convert every full-scan `@SpringBootTest` to an explicit-wiring slice**.

## Guiding principle (decided this session)

**No SpringBoot test scans the entire classpath.** Each test wires, through its own
configuration classes, exactly the beans it needs — from mocks. The production
composition root (`ConsoleApplication`) keeps the broad scan; tests never use it. This
dissolves the naturalist-rdbms problem at the root: if no test scans, nothing ever
discovers or force-constructs the rdbms adapter, so it may sit harmlessly on the test
classpath.

## Scope

**Built here (three workstreams):**

1. **Naturalist persistence swap** — *limited to the naturalist module.* Mocks lose
   `@DomainService`, rdbms repos gain it; management-console depends on the rdbms module
   and demotes the mock to test scope.
2. **Convert management-console full-scan tests to explicit-wiring slices** — all
   clusters, in place (auth, resilience, catalog, search, admin, usage). No console
   relocation, no `@Disabled` parking.
3. **Insects controller tests → `insects-console` `@WebMvcTest` slices.**

**Documented as the immediate follow-up (NOT built here):**

- **Persistence Spring wiring (makes `ConsoleApplication` bootable again).** A Spring
  `@Configuration` in the `persistence` kernel defining `DataSource` /
  `SqlSessionFactory` / mapper beans so the scanned `@DomainService` rdbms repos
  construct. Until it lands, `ConsoleApplication` scans the rdbms repos but cannot
  construct them (their MyBatis mappers are not Spring beans), so it is deliberately
  non-bootable. `mvn verify` stays green regardless, because no test loads
  `ConsoleApplication`.

**Deferred to their own efforts:**

- **Console relocation of catalog/search and admin/usage.** Their controllers/views stay
  in management-console for now; only their *tests* are converted here. Rehoming the
  catalog/search surface (candidate home: `library-console`) and sorting out admin/usage
  are separate efforts.
- `usage-console` / `naturalists-console` (no clean content today — every naturalist
  view is login/security; usage's UI is an admin surface).
- The auth/security overhaul for a JavaScript front end.
- Production schema provisioning (`DboSchema` / seeder) and standing-Postgres ops.

## Grounded findings (verified 2026-08-28)

1. **The annotation swap breaks every full-scan test.** `NaturalistQueryImpl` /
   `NaturalistCredentialQueryImpl` (`naturalists-core`) are `@DomainService`, always
   scanned, always instantiated, and inject `NaturalistRepository` /
   `NaturalistCredentialRepository`. The moment the mock repos lose `@DomainService`, any
   context that loads the broad scan (i.e. every `@SpringBootTest` today) fails to wire.
   Hence *every* full-scan test must become an explicit slice (or the swap can't land).

2. **Full-scan `@SpringBootTest` inventory** (22 classes). After Workstream 3 moves the 8
   insects tests out, 14 remain, clustered by subject:
   - **Catalog + Search (3):** `CatalogConfigurationTest`,
     `NutrientChemistryCatalogLinkingTest`, `SearchControllerWebMvcTest`.
   - **Admin + Usage (7):** `AdminCatalog`, `AdminDomainServices`, `AdminResilience`,
     `AdminSecurity`, `AdminUsage`, `AdminUsageAlertAcknowledge`, `AdminUsageSecurity`.
   - **Auth (2):** `HeaderWebMvcTest`, `NaturalistLoginWebMvcTest`.
   - **Resilience (2):** `ResilienceConfigurationTest`, `ResilienceNameValidatorTest`.
   - Already lightweight (not `@SpringBootTest`, untouched): `AdminPropertiesTest`,
     `CurrentNaturalistTest`, `NaturalistHeaderInterceptorTest`, `CatalogResilienceTest`,
     `AlertDispatchJobTest`, `AlertEmailerTest`, and the two ArchUnit
     architecture tests.

3. **Two tests exist to exercise the scan.** `CatalogConfigurationTest`
   (`discoversEveryDomainSubtype` / `discoversEveryDomainLinker`) and
   `AdminDomainServicesControllerWebMvcTest` (lists *every* `@DomainService` grouped by
   domain). Converted to explicit wiring they become *logic* tests over a hand-wired
   representative set; the "the production scan auto-discovers every domain" coverage is
   intentionally dropped (covered by running the app). This is an accepted consequence of
   the no-scan-in-tests principle.

4. **JTE resolves from the filesystem at runtime** (`JteConfiguration` and
   `insects-console`'s `TestTemplateEngine` both compile from `apps/*/src/main/jte` +
   `domains/*/*-console/src/main/jte`). The `layout/page.jte` classpath trap is only
   about Java compilation of template tests. A console `@WebMvcTest` can render full pages
   via that same filesystem resolver.

5. **`InsectsController` does not use Spring Security** — it reads the current naturalist
   from the request attribute `naturalist.currentNaturalistName`. A slice sets it
   directly; no security filter chain needed.

6. **No production DataSource/MyBatis Spring wiring exists.**
   `persistence.MyBatisSupport.sessionFactory(DataSource, mappers…)` is a plain factory;
   the rdbms `*IT` tests hand-build a `SqlSessionFactory` via `persistence-test`'s
   `RdbmsDataSource`. The `persistence` kernel pom has no Spring dependency yet. Hence the
   follow-up.

7. **`naturalists-repository-rdbms` already test-scopes `naturalists-repository-test`**,
   and no main rdbms source touches the mock — the mock does not leak onto the production
   classpath through the rdbms module.

---

## Target architecture: explicit-wiring test slices

Replace "load the whole app" with a small library of focused test `@Configuration`s under
`apps/management-console/src/test/java`, each composing a coherent set of beans from
mocks. A test declares `@WebMvcTest(SomeController.class)` (web slice) or
`@SpringBootTest(classes = {…})` / `@ContextConfiguration(classes = {…})` (context slice)
and `@Import`s only the slice configs it needs. Nothing imports `DomainServiceScan`.

Proposed slice configs (final set determined during implementation):

- **`NaturalistMockSlice`** — the mock `NaturalistRepository` / `NaturalistCredentialRepository`
  beans plus `NaturalistQueryImpl` / `NaturalistCredentialQueryImpl`, so naturalist-facing
  code wires without the scan.
- **`WebSecuritySlice`** — `SecurityConfiguration`, `NaturalistUserDetailsService`,
  `NaturalistHeaderInterceptor`, `PasswordEncoder`, over `NaturalistMockSlice`. For the
  tests that assert login/redirect/CSRF/role behavior (auth + the app-security assertions
  displaced from insects).
- **`CatalogSlice`** — a `Catalog` assembled by `CatalogConfiguration.catalog(...)` from an
  *explicit, representative* set of `DomainId` / `CatalogContribution` /
  `EntityReferences` beans (plants + chemistry + insects) + `Resilience`. Serves the
  catalog, search, and admin-catalog tests.
- **`ResilienceSlice`** — `ResilienceConfiguration` + the assembled `Resilience` bean.
- **`UsageSlice`** — `UsageQuery` / `UsageCommand` (from mocks) + `UsageProperties`, for
  the admin-usage tests.

Reuse is by composition: e.g. an admin-usage web test = `@WebMvcTest(AdminUsageController.class)`
+ `@Import({WebSecuritySlice.class, UsageSlice.class})`.

---

## Workstream 1 — Naturalist persistence swap (naturalist module only)

- Remove `@DomainService` from `NaturalistEntityRepositoryMock` and
  `NaturalistCredentialRepositoryMock`; add it to `NaturalistEntityRepositoryRdbms` and
  `NaturalistCredentialRepositoryRdbms`. **No other domain is touched** — every other
  `*-repository-rdms` mock keeps `@DomainService` until it has a real adapter.
- management-console pom: add `naturalists-repository-rdbms` (main scope), demote
  `naturalists-repository-test` to **test** scope.
- Result: `ConsoleApplication`'s production scan now discovers the `@DomainService` rdbms
  repos (but can't construct them until the follow-up — non-bootable, by design). Tests
  never see this because they don't scan.

## Workstream 2 — Convert management-console full-scan tests to explicit-wiring slices

Convert all 14 remaining full-scan `@SpringBootTest`s (auth, resilience, catalog, search,
admin, usage) to slices composed from the slice-config library above. No test imports
`DomainServiceScan`; no test constructs the rdbms adapter.

- **Catalog/Search/AdminCatalog** → `CatalogSlice` (+ `WebSecuritySlice` for the web
  ones). `CatalogConfigurationTest` and `AdminDomainServicesControllerWebMvcTest` become
  logic tests over the explicit bean set (scan-completeness coverage dropped per finding 3).
- **Admin/Usage** → `@WebMvcTest(controller)` + `UsageSlice` + `WebSecuritySlice`.
- **Auth** → `@WebMvcTest` (or a security context slice) + `WebSecuritySlice`.
- **Resilience** → `ResilienceSlice` (+ `CatalogSlice` where the test asserts resilience
  around catalog ops).
- The app-security assertions displaced from the insects tests (anonymous→`/login`,
  missing-CSRF→403, role gating on a representative protected route) land here on
  `WebSecuritySlice`, as app-security tests rather than insects tests.

## Workstream 3 — Insects controller tests → `insects-console` `@WebMvcTest` slices

- Move `console/insects/*` + the render/behavior assertions of `InsectsControllerWebMvcTest`
  into `insects-console` as `@WebMvcTest(InsectsController.class)` slices.
- `insects-console` gains `spring-boot-starter-test` (test scope); **not** Spring Security.
- Current naturalist via `.requestAttr("naturalist.currentNaturalistName", …)`.
- Collaborators from `InsectsTestContext` / `LibraryTestContext` (real mock-backed queries
  over real fixtures — the tests assert on `battus-philenor`/`apis-mellifera`, so Mockito
  `@MockBean`s won't do), exposed via a small `@TestConfiguration`.
- Full-page JTE render: wrap the existing filesystem `TestTemplateEngine` into a
  `TemplateEngine` + JTE view-resolver bean for the slice (in `insects-console` test
  sources for this pilot; a shared console-test support module is a later rollout concern).
- `@WebMvcTest` needs a `@SpringBootConfiguration` anchor — add a minimal one in
  `insects-console` test sources.
- Security-filter assertions do **not** move (they belong on `WebSecuritySlice` in
  management-console).

---

## Follow-up (documented, not built here) — Persistence Spring wiring

Add a Spring `@Configuration` **in the `persistence` kernel** (author's decision) defining
the Hikari `DataSource` (from `application.yml`), the `SqlSessionFactory` (via
`MyBatisSupport`), and the naturalist mapper beans, so the scanned `@DomainService` rdbms
repos construct and `ConsoleApplication` boots on Postgres.

*Design note / dissent:* this couples the `persistence` kernel to Spring, against the
convention that confines Spring to `adapters/` + `apps/` (`spring-runtime`,
`spring-test-data` are adapters). An `adapters/spring-persistence` module wrapping the
kernel would preserve that convention. Recorded as the author overrode this in favor of
the kernel.

---

## Testing & verification

- `mvn verify` from repo root is green with no management-console test loading the broad
  scan; then the completeness gate:
  `mvn install -DskipTests && mvn rewrite:dryRun -Drewrite.failOnDryRunResults=true`.
- Confirm no remaining `@SpringBootTest` in management-console imports `DomainServiceScan`
  (grep gate; optionally an ArchUnit rule).
- Insects slices pass inside `insects-console` with no DB, no scan, no Spring Security.
- Kernel-signature-touching changes may need a clean `mvn install` to avoid stale-class
  `NoSuchMethodError`s.

## Decisions recorded

- **No full-classpath scan in tests** — explicit-wiring slice configs instead. **No Spring
  profiles.**
- **Annotation swap limited to the naturalist module** — swap now; other domains untouched.
- **Convert everything now** — all clusters converted in place; nothing parked/`@Disabled`;
  catalog/admin/usage *controllers* stay in management-console (console relocation is a
  separate future effort), only their tests convert.
- **Scan-completeness coverage** (Catalog/AdminDomainServices auto-discovery) intentionally
  dropped from tests; covered by running the app.
- **Mock wiring lives only in test sources** (slice configs); never in main sources.
- **Production naturalist wiring:** `ConsoleApplication` depends on
  `naturalists-repository-rdbms` and scans it; mock demoted to test scope.
  `ConsoleApplication` non-bootable until the persistence-wiring follow-up.
- **Insects tests:** `@WebMvcTest` slices with request-attribute auth and
  `InsectsTestContext`-backed beans; security assertions stay app-level.
- **DataSource/SqlSessionFactory config home:** `persistence` kernel (author's decision;
  convention dissent recorded).

## References

- Trigger: [`2026-08-27-rdbms-persistence-kernel-naturalists-design.md`](2026-08-27-rdbms-persistence-kernel-naturalists-design.md)
- Brief this refines: [`2026-08-28-console-restructuring-repository-implies-console-design.md`](2026-08-28-console-restructuring-repository-implies-console-design.md)
- `adapters/spring-runtime/.../DomainServiceScan.java`,
  `adapters/spring-test-data/.../TestDataConfiguration.java`
- Reference console module: `domains/insects/insects-console/`
- `kernels/persistence/.../MyBatisSupport.java`,
  `kernels/persistence-test/.../RdbmsDataSource.java`
- Project memory: `project_rdbms_persistence_kernel`,
  `project_shared_layout_jte_classpath`, `project_naturalist_auth_login`,
  `project_repository_module_split`, `project_console_restructuring_direction`.
