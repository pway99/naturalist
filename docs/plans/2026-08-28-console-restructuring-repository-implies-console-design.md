# Console Restructuring — "Repository ⇒ Console" — Design Brief

**Date:** 2026-08-28
**Status:** Rough design brief (hand-off for a fresh session to refine into a plan)
**Author intent:** capture the target architecture + the open questions; a new session
brainstorms the details and writes the implementation plan.

## One-line goal

Make each domain's controller integration testing live in that domain's `-console`
module, reduce `apps/management-console` to genuinely app-level integration tests, and
**decouple those tests from the production `@DomainService` classpath scan** — so a single
domain's persistence implementation can be swapped (in-memory mock ⇄ real RDBMS adapter)
without forcing a database dependency on unrelated tests.

Guiding principle (author's): **a module mature enough for a repository deserves a console
too.** `usage` and `naturalists` currently have repositories but no `-console`.

## Why now (the concrete trigger)

The naturalists domain just got a real RDBMS repository adapter
(`naturalists-repository-rdbms`, Postgres + MyBatis; see
`docs/plans/2026-08-27-rdbms-persistence-kernel-naturalists-design.md`). The next step is
to wire the **running app** to use it. But:

- `apps/management-console` has **22 `@SpringBootTest`** integration tests, all loading the
  full production context (`ConsoleApplication` does `scanBasePackages = "com.naturalist"`
  and `CatalogConfiguration` `@Import`s `DomainServiceScan`).
- That scan discovers every `@DomainService` bean. If the naturalist repository bean becomes
  the RDBMS adapter, **every one of the 22** must construct it → it needs a MyBatis mapper +
  `DataSource` → **Postgres**. So all 22 break, even the ones that never touch naturalists.

The load-bearing fix is to stop management-console's integration tests from depending on the
whole-app scan for beans they don't exercise. That fix is a prerequisite for the RDBMS
wiring, and it also resolves a standing smell: domain-specific controller tests living in
the composition root.

## Current state (grounded — a new session should trust this but re-verify)

- **`<domain>-console` modules** exist for `chemistry, garden, insects, library, plants,
  soil`. They hold the domain's Spring `@Controller`(s) + JTE templates + tests. Their tests
  use lightweight `spring-test` (JTE template rendering via a `TestTemplateEngine`, plus unit
  tests like linkers) — **not** `@SpringBootTest`. They do **not** depend on
  `spring-boot-starter-test` or Spring Security.
- **`apps/management-console`** is the Spring Boot composition root. Its 22 `@SpringBootTest`
  classes break down roughly as:
  - **Domain-specific (movable):** insects (~8, incl. `InsectsControllerWebMvcTest`),
    usage (2).
  - **App-level (stays):** admin (8), auth (4 — naturalist login/security), catalog (3 —
    cross-domain), search (1), resilience (2), architecture (2 — ArchUnit).
- **Repositories without a console:** `usage`, `naturalists` (the mature ones; other
  repo-bearing directories — apiary, arachnids, climate, fungi, microbes, molluscs, sensors,
  vertebrates, weather, worms, zone — are skeletons and out of scope until they grow).
- Beans reach the app via `adapters/spring-runtime`'s `DomainServiceScan` (registers every
  `@DomainService`) + `adapters/spring-test-data`'s shared `NaturalistDatabase` bean. The
  6 domains with consoles wire through `<domain>-repository-rdms` (mock-backed
  `@DomainService`); **naturalists is the exception** — management-console depends directly
  on `naturalists-repository-test` (the mock is `@DomainService`).

## Target architecture

1. **Decouple management-console integration tests from the production scan (load-bearing).**
   The retained app-level tests must wire the domain repository/query beans they actually use
   **from the mocks, via explicit test configuration**, rather than triggering the full
   `DomainServiceScan`. Production keeps the real scan (`ConsoleApplication`); tests use a
   test-only context. Open question below on shared-vs-per-test.

2. **Domain controller tests → `@WebMvcTest` slices in the `-console` modules.** Move the
   insects/usage controller integration tests down into their `-console` modules as
   `@WebMvcTest` slices that `@MockBean` the domain query — no scan, no DB, fast. Each
   `-console` module gains `spring-boot-starter-test`. management-console stops carrying
   domain-specific controller tests.

3. **`repository ⇒ console`: create `usage-console` and `naturalists-console`.** These hold
   the domain's views/controllers (the ones currently sitting in management-console under
   `console/usage` and the naturalist views). **Auth/login is explicitly NOT part of this**
   (see Out of scope).

4. **`@DomainService` moves off the mocks as RDBMS equivalents arrive.** When a domain gains
   a real RDBMS adapter, its mock loses `@DomainService` (so production scans exactly one
   implementation — the adapter), and that domain's tests wire the mock explicitly. For now
   this applies only to naturalists; the other domains keep their mock-backed `-rdms`
   `@DomainService` beans until they get adapters. Production is therefore a deliberate,
   phased mix (naturalists on Postgres, others on in-memory mocks).

Net effect: management-console holds only app-level integration tests (auth, admin, catalog,
search, resilience, architecture), each wiring the few domain beans it needs from mocks; no
test depends on the full production scan; a domain's persistence impl can be swapped on the
classpath without rippling into unrelated tests.

## Out of scope (deferred to their own efforts)

- **Auth & security overhaul — the next big task.** Moving naturalist authentication/session
  handling to a **production-ready approach that can serve a JavaScript front end** (token
  /session model TBD). Until then, naturalist login/security **stays app-level in
  management-console** (it is coupled to Spring Security, which the `-console` modules
  deliberately avoid — see the `page.jte` classpath-trap note in project memory).
  `naturalists-console` holds views only, not the login/security flow. Do not pull Spring
  Security into a `-console` module in this effort.
- **The naturalists RDBMS app-wiring itself** — MyBatis-Spring `SqlSessionFactory`/mapper/
  `DataSource` beans, `@DomainService` on the rdbms adapters, production DataSource config,
  and production schema provisioning. This is the downstream effort this restructuring
  unblocks; design it after Slice 1 lands.
- Consoles for skeleton domains (apiary, arachnids, …) — when they mature.

## Suggested slicing (for the new session)

1. **Slice 1 — test-context decoupling (unblocks RDBMS).** Give management-console's
   app-level `@SpringBootTest`s a test context that wires domain beans from mocks without the
   full `DomainServiceScan`. This is the minimum that lets naturalists→RDBMS proceed.
2. **Slice 2 — pilot relocation on insects.** Move insects controller integration tests into
   `insects-console` as `@WebMvcTest` slices; establish the pattern (module deps, test app,
   mock-query wiring).
3. **Slice 3 — roll out + new consoles.** Relocate remaining domain controller tests;
   create `usage-console` and `naturalists-console`.
4. **(Downstream) naturalists RDBMS app-wiring** — separate design/effort.

Slice 1 is the only hard prerequisite for the RDBMS work; 2–3 are the architectural cleanup
and can follow.

## Open questions (for the new session to resolve)

- **Shared vs per-test config.** One `ConsoleTestApplication` (a `@SpringBootConfiguration`
  in management-console test sources that omits `DomainServiceScan` and imports per-domain
  mock-wiring `@TestConfiguration`s), or per-test `@Configuration` classes? The author leans
  toward "a single configuration class per console / test group prevents the full scan and
  defines beans from the mock repositories." Shared is far less churn across 22 tests;
  per-test is maximally isolated. Recommend prototyping the shared approach first.
- **`@WebMvcTest` vs `@SpringBootTest` for the relocated domain tests.** `@WebMvcTest` +
  `@MockBean` query is the idiomatic, DB-free slice; confirm it covers what the current
  full-context insects tests assert (security filters, nav, catalog links may need
  `@Import`s or a slice config).
- **What exactly moves for `usage`/`naturalists`.** Inventory the `console/usage` and
  naturalist view code/tests in management-console and decide what constitutes
  `usage-console` / `naturalists-console` (views only; login stays behind).
- **`spring-test-data` / `NaturalistDatabase` in tests.** The shared in-memory
  `NaturalistDatabase` bean still backs the other domains' mocks; confirm the decoupled
  test context still provides it where needed.
- **Does management-console remain independently runnable** (`spring-boot:run`) on mocks for
  dev, or does it only run in production wiring? Affects whether the mock stays a main-scope
  or test-scope dependency.

## References

- RDBMS adapter (the trigger): `docs/plans/2026-08-27-rdbms-persistence-kernel-naturalists-design.md`
- `adapters/spring-runtime/.../DomainServiceScan.java`, `adapters/spring-test-data/.../TestDataConfiguration.java`
- Reference `-console` module: `domains/insects/insects-console/` (controller + JTE + template/unit tests)
- Project memory: `project_rdbms_persistence_kernel`, `project_shared_layout_jte_classpath`,
  `project_naturalist_auth_login`, `project_repository_module_split`.
