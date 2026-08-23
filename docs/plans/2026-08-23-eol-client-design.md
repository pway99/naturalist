# EOL Client — design, learnings & swap checklist

**Date:** 2026-08-23
**Status:** Intermediate placeholder shipped; real HTTP client deferred.

## Current state (shipped)

`external-authorities/eol/eol-client` is a new production module holding
`EolClient` — the app's `ExternalAuthority` bean. **For now it is a placeholder that
`extends EolClientMock`**, so the deployment runs on a production-named,
`@DomainService`-discovered bean temporarily backed by the in-memory fixture
authority. This deliberately mirrors the repository intermediate pattern:

| Repositories | EOL |
|---|---|
| `<domain>-repository-test` (holds the mock) | `eol-client-mock` (holds `EolClientMock`) |
| `<domain>-repository-rdms` → `<Entity>RepositoryRdms extends …Mock`, `@DomainService` | `eol-client` → `EolClient extends EolClientMock`, `@DomainService` |
| app depends on `-repository-rdms`; mock is test-support | app depends on `eol-client`; mock is test-support |
| bounded, temporary DAG inversion (rdms → repository-test) | bounded, temporary inversion (eol-client → eol-client-mock) |

Changes made:
- New module `external-authorities/eol/eol-client` (parent `eol`; depends only on
  `eol-client-mock`, which brings `eol-api` + `framework-test` + `framework` transitively).
- `EolClient` — `@DomainService`, `@ResilienceExempt` (no I/O yet), ctor `(NaturalistDatabase)`
  → `super(...)`.
- `EolClientMock` de-`final`ed so the intermediate can extend it (the repository mocks are
  likewise non-final).
- `apps/management-console` now depends on `eol-client` (was `eol-client-mock`); `EolClient`
  is discovered by `DomainServiceScan` as the sole `ExternalAuthority` bean, so the manual
  `IdentificationConfiguration` `@Bean` was deleted (the config held nothing else).
- Root `dependencyManagement` + `external-authorities/eol/pom.xml` `<modules>` updated.

Net effect: identical runtime behaviour to before (fixture-backed EOL lookups, no network),
but the production seam now exists — the real client is a body swap, not a rewire. Every
`@SpringBootTest` stays network-free.

## Decisions locked for the real client (do not re-litigate)

- **HTTP client: OkHttp** (`com.squareup.okhttp3:okhttp`). Already recognised by
  `ResilienceComplianceTest.HTTP_CLIENT_PREFIXES` (`okhttp3`), so the resilience gate applies
  and is satisfied by taking `Resilience` + wrapping calls.
- **Selection = classpath (rdms pattern).** The real client ships on the app classpath as the
  bean; the mock is test-support constructed directly by tests. No property/profile gate.
- **Port scope only:** implement the three `ExternalAuthority` methods — `source()`,
  `lookup(EntityName) → Set<AuthorityReference>`, `fetchContent(AuthorityReference) →
  AuthorityContent`. Enriching `Eol.citation(...)` metadata from the pages API is out of scope
  (the port does not ask for it).
- **Offline test suite:** OkHttp `MockWebServer` + captured JSON fixtures. No test performs
  real network I/O; live calls happen only in dev-time verification and manual smoke.

## What the real client needs (deferred work)

### Contract to satisfy (`kernels/authority`)
- `AuthoritySource source()` → `Eol.SOURCE` (`"eol"` / `"Encyclopedia of Life"`).
- `Set<AuthorityReference> lookup(EntityName subject)` — empty set = "nothing known" (never
  null); validate `subject` via `Observer`.
- `AuthorityContent fetchContent(AuthorityReference ref)` — `content` must be **non-blank**
  (record invariant); decide the no-text fallback (throw vs minimal title summary) and test it.
- `eol-api` already provides `Eol.SOURCE`, `Eol.deepLink(EolPageId)` →
  `https://eol.org/pages/{id}`, and `EolPageId`. Build refs as
  `new AuthorityReference(Eol.SOURCE, Eol.deepLink(pageId))`.

### EOL API — MUST verify against live docs before coding (not yet done)
The exact endpoints/JSON were **not** confirmed this session. Before writing request/parse
code, WebFetch EOL's API docs (start: `https://eol.org/docs/what-is-eol/classic-apis`) and
capture live fixtures for a known taxon (e.g. *Apis mellifera*). Confirm:
- **Search** endpoint + params: name → results carrying a numeric page id. (Classic v1.0 was
  `https://eol.org/api/search/1.0.json?q=<name>`.) Record the JSON path to the page id.
- **Pages** endpoint + params to return descriptive text. (Classic v1.0:
  `https://eol.org/api/pages/1.0/<id>.json?...`.) Record the JSON path to the overview/
  description text.
- **Whether a token is required** by the current supported API — if so, it changes the config
  (add an API-key field, sourced from env like `VisionConfiguration` reads `ANTHROPIC_API_KEY`)
  and the wiring; STOP and flag before proceeding.
Save the two responses as fixtures under `eol-client/src/test/resources/eol/`.

### Implementation shape
- `EolClientConfig` record (`baseUrl`, search/pages paths, timeout, `searchLimit`) with
  `defaults()`; keep `baseUrl` injectable so `MockWebServer` tests point at `localhost`.
- `EolClient(EolClientConfig, Resilience)` builds an `OkHttpClient`; `lookup`/`fetchContent`
  execute the GET **through the `Resilience` facade** (bulkhead + timeout + retry, mirroring
  `AnthropicVisionService`), then delegate to package-private static parse methods.
- Package-private Jackson DTOs (`EolSearchResponse`, `EolPageResponse`) with
  `@JsonIgnoreProperties(ignoreUnknown = true)`, matching the verified field paths.

### Resilience + ArchUnit (enforced the moment OkHttp is on the classpath)
`ResilienceComplianceTest` (runs in management-console over `com.naturalist` main classes)
will require any class depending on an `okhttp3` type to be `@Resilient` AND to reach the
`Resilience` facade. So the real `EolClient` must:
- carry `@Resilient(name = "authority.eol")` (replacing `@ResilienceExempt`),
- take `Resilience` and actually call it, and
- have `authority.eol` registered as a `ResilienceConfig` in
  `apps/.../resilience/ResilienceConfiguration` (and in `ResilienceNameValidator` if it
  enumerates names).

### Testing
- `MockWebServer` enqueues the captured fixtures; assert `lookup` → deep-link ref, empty
  results → empty set, null subject → `InvariantViolationException`; `fetchContent` → non-blank
  `AuthorityContent`. Parse methods also unit-tested directly against fixtures.

## Swap checklist (turning the placeholder into the real client)
1. Add `okhttp3` (+ `mockwebserver` test) to `eol-client/pom.xml` and pin the OkHttp version in
   root `dependencyManagement`.
2. Verify the EOL API + capture fixtures (above).
3. Replace `EolClient extends EolClientMock` with the OkHttp body + `EolClientConfig` +
   DTOs + parse methods; ctor `(EolClientConfig, Resilience)`.
4. Swap `@ResilienceExempt` → `@Resilient(name="authority.eol")`; register the config.
5. Drop the `eol-client-mock` dependency from `eol-client/pom.xml` (inversion severed).
6. Re-`final` `EolClientMock` if nothing else extends it (optional tidy).
7. `mvn verify` — `ResilienceComplianceTest`, `InsectsControllerWebMvcTest` (context boots on
   the real bean), and the new `MockWebServer` tests all green; suite stays offline.
