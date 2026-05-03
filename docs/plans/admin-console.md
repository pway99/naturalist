# Admin Console — Sketch

A secure `/admin/**` surface in `apps/management-console` for inspecting
runtime configuration and assembly state. Driven by the operational
need to review what is wired without ssh-ing into a host, scraping
logs, or standing up a log aggregator.

This is a sketch, not a binding plan. Promote to a numbered milestone
list (the runtime-architecture-refactor plan's shape) when the first
view ships.

---

## Why this exists

Three operational pains converge:

1. **Resilience strategy review.** `ResilienceConfiguration` registers
   named strategies referenced by `@Resilient(name = ...)` call sites.
   A typo on either side is a deployment defect that should be visible
   on demand, not buried in startup logs.
2. **`@DomainService` discovery audit.** `DomainServiceScan` scans
   `com.naturalist.**` and registers everything carrying the marker.
   "What got picked up?" is a question with no current answer short of
   reading code.
3. **Catalog assembly state.** The assembled `Catalog` is the fan-in
   of every domain's `CatalogContribution` and `EntityReferences`
   provider. A missing contribution surfaces as missing search hits,
   not a clear error.

A log aggregator solves none of these — they are pull-on-demand
questions, not stream-of-events questions. Spring Boot Actuator solves
some, but the project deliberately stays light on operational
infrastructure and the actuator JSON shape leaks Resilience4j
specifics into a kernel-neutral surface.

---

## Architectural decisions (declarative)

**Surface lives in `apps/management-console` under `com.naturalist.console.admin`.**
Each view is a controller + JTE template pair under
`apps/management-console/src/main/jte/admin/`. No new module — these
are app-specific views over already-wired beans.

**Auth via the existing `SecurityConfiguration` + an `ADMIN` role.**
`/admin/**` requires the `ADMIN` authority. Credential source: a
property-bound user (`naturalist.admin.username`,
`naturalist.admin.password`) with no defaults — the app refuses to
start without both set. No hard-coded admin/admin. (Revisit when a
real identity provider lands.)

**Diagnostics surfaces are vendor-neutral.** The kernel facade grows
the methods needed to expose state (e.g. `Resilience.registeredNames()`
returning `Set<String>`); the controller calls only the kernel
facade. Resilience4j-specific detail (open/closed/half-open state,
metrics) is a follow-up that justifies a small adapter-side
extension, not direct vendor imports in the controller.

**One view per concern, one PR per view.** Resists the temptation to
ship "an admin dashboard" as a single sprawling change. Each view
ships with its own tests and its own auth assertion.

---

## Milestones — `/admin/resilience` (view 1)

The first view ships in four milestones, each its own PR and each a
clean context-clear point. Each milestone names its acceptance criteria
so the next session can pick up cold from the plan alone.

**M1 — Kernel facade: registered-names accessors.**
Add per-primitive `Set<String>` accessors to `Resilience` (`retryNames()`,
`timeoutNames()`, `circuitBreakerNames()`, `bulkheadNames()`) — mirrors
the existing four-method facade shape. `NoOpResilience` returns
`Set.of()` for all four. `Resilience4jResilience` returns the keysets
of its already-immutable registry maps. Adapter test covers a populated
case per primitive. Acceptance: kernel + adapter tests green; no
controller or security work in this milestone.

**M2 — Security: ADMIN role and property-bound credentials.**
Add `naturalist.admin.username` / `naturalist.admin.password` property
binding with no defaults — the app refuses to start without both set.
Wire `/admin/**` to require the `ADMIN` authority in
`SecurityConfiguration`. Acceptance: app fails to boot when either
property is missing or blank; anonymous `GET /admin/**` is rejected
(401 or login redirect, whichever the existing security chain produces);
ADMIN-authenticated traffic continues through the existing chain.

**M3 — Controller + template: render registered strategies.**
Add `AdminResilienceController` (`com.naturalist.console.admin`) and
`admin/resilience.jte` under `apps/management-console/src/main/jte/`.
Inject the kernel `Resilience` facade only — no Resilience4j imports
in the controller. List names grouped by primitive (retry / timeout /
circuit-breaker / bulkhead). Acceptance: ADMIN-authenticated
`GET /admin/resilience` renders a 200 containing every currently
registered strategy name (`catalog.fanout`, `image.conversion`) under
its correct primitive heading.

**M4 — Tests + polish.**
Auth-assertion test (anonymous → reject, ADMIN → 200), render-assertion
test (each registered strategy name appears under its primitive
section), any styling that brings the page in line with existing
console pages. Acceptance: full test suite green; manual smoke against
a locally running console with the dev profile passes.

Future enhancements (not part of view 1): per-strategy config values
(retry attempts, timeout duration, breaker thresholds) and per-strategy
live state (open/closed/half-open). Live state crosses the kernel
facade boundary into Resilience4j-specific territory and justifies a
small adapter-side extension rather than direct vendor imports in the
controller.

**Status: Shipped.** M1 added `retryNames()` / `timeoutNames()` /
`circuitBreakerNames()` / `bulkheadNames()` to the kernel facade and
the Resilience4j adapter. M2 added `naturalist.admin.username` /
`naturalist.admin.password` property binding (refuses to start without
both) and gated `/admin/**` on the `ADMIN` authority. M3+M4 landed
`AdminResilienceController` + `admin/resilience.jte`, auth- and
render-assertion tests, and styling.

---

## Milestones — `/admin/domain-services` (view 2)

The second view lists every class the Spring-runtime adapter
registered from the `@DomainService` marker, grouped by domain
package. Two milestones, each its own PR.

Design notes that shape the milestone count:

- **No new kernel facade.** The kernel-level abstraction for view 2
  is the `@DomainService` marker itself; the registry is owned by
  `adapters/spring-runtime/`. The controller injects
  `ApplicationContext` and calls
  `getBeansWithAnnotation(DomainService.class)`. The console is a
  Spring Boot composition root and may legitimately read its own
  container. (This is consistent with the "vendor-neutral" rule from
  view 1: that rule kept Resilience4j-specific types out of the
  controller, not Spring out of a Spring-Boot app.)
- **No new security work.** M2 of view 1 already gates `/admin/**` on
  the `ADMIN` authority.
- **Grouping** is the second segment of the bean class's package —
  `com.naturalist.<domain>.…` → `<domain>` — alphabetised, with each
  bean's simple name and FQCN listed under its domain heading.

**M1 — Controller + template: render registered domain services.**
Add `AdminDomainServicesController` (`com.naturalist.console.admin`)
and `admin/domain-services.jte` under
`apps/management-console/src/main/jte/`. The controller injects
`ApplicationContext`, calls `getBeansWithAnnotation(DomainService.class)`,
and groups results by the second segment of `Class#getPackageName()`.
Template renders each domain section alphabetically with simple class
name + FQCN per bean. No imports of Resilience4j, of `DomainServiceScan`
internals, or of any domain `*-core` package. Acceptance: ADMIN-
authenticated `GET /admin/domain-services` renders a 200 containing
every currently registered `@DomainService` class under its correct
domain heading.

**M2 — Tests + polish.**
Auth-assertion test (anonymous → reject, ADMIN → 200), render-assertion
test (a representative `@DomainService` bean per domain appears under
its expected heading; absence of any heading for a domain that has no
`@DomainService` beans), navigation link added between
`/admin/resilience` and `/admin/domain-services` (or a small admin
index that links both), styling matched to existing console pages.
Acceptance: full test suite green; manual smoke against a locally
running console with the dev profile passes.

Future enhancements (not part of view 2): per-bean dependency edges
("this `@DomainService` depends on these other beans"); marking which
beans contribute to which kernel ports (`CatalogContribution`,
`EntityReferences`, `EntityRefLinker`). Those overlap with view 3
(`/admin/catalog`) and should be designed against view 3's milestones,
not bolted onto view 2.

**Status: Shipped.** M1 landed `AdminDomainServicesController` +
`admin/domain-services.jte`, injecting `ApplicationContext` and grouping
`@DomainService` beans by the second segment of their package. M2 added
auth- and render-assertion tests and an `admin/nav.jte` partial linking
both admin views (with `aria-current="page"` on the active one). Visual
styling — the `.admin-nav` rules and any consolidation of the inner-group
classes — is owned by the parallel `banner-styling` branch and is
intentionally not part of this PR.

---

## Milestones — `/admin/catalog` (view 3)

The third view lists every `CatalogContribution` and `EntityReferences`
provider the assembled `Catalog` knows about, grouped by `DomainId`.
Two milestones, each its own PR.

Design notes that shape the milestone count:

- **No new kernel facade.** The contributions and providers are
  already Spring beans the `Catalog` `@Bean` consumes at assembly time
  in `CatalogAssembly.from(domains, contributions, providers, …)`. The
  controller injects `List<CatalogContribution>` and
  `List<EntityReferences<?>>` directly — the same authoritative source
  the assembly itself reads. Adding introspection to the `Catalog`
  interface would burden every future adapter (e.g. a Solr-backed one
  with no in-memory list to expose) with a method whose only caller is
  this view. Consistent with view 2's "Spring out of a Spring-Boot
  app is fine; vendor types out of the kernel facade are not" rule.
- **No new security work.** M2 of view 1 already gates `/admin/**` on
  the `ADMIN` authority.
- **Grouping** is by `DomainId.value()`, alphabetised. Each group
  lists forward-direction contributions first (simple class name +
  FQCN), then inverse-direction providers (simple class name + FQCN +
  the `EntityName` subclass simple name resolved from
  `referenceType()`). A domain that registers only one direction
  appears with only that section.
- **No live counts.** The controller does not call
  `searchableEntities()` or `referencesTo(...)`. Both are live calls
  whose cost is shaped by the contributing domain, and a diagnostic
  page that triggers a fan-out on every render is the wrong shape.
  "Did my contribution get picked up?" is answered by presence in the
  list, not by a count.
- **`DomainId` list parity with the assembly.** The injected
  `List<DomainId>` (the same one `CatalogAssembly` validates for slug
  uniqueness) is rendered alongside contributions and providers so a
  `DomainId` registered without either direction still surfaces — the
  same defect class slug-uniqueness validation guards against at
  startup is the one this view makes visible at runtime.

**M1 — Controller + template: render registered contributions and providers.**
Add `AdminCatalogController` (`com.naturalist.console.admin`) and
`admin/catalog.jte` under `apps/management-console/src/main/jte/`. The
controller injects `List<DomainId>`, `List<CatalogContribution>`, and
`List<EntityReferences<?>>`, groups them by `DomainId.value()`, and
sorts groups alphabetically. No imports of `InMemoryCatalog`, of
`CatalogAssembly`, or of any domain `*-core` package. Acceptance:
ADMIN-authenticated `GET /admin/catalog` renders a 200 containing every
registered `CatalogContribution` and `EntityReferences` provider under
its correct `DomainId` heading; a `DomainId` registered with neither
appears as an empty heading.

**M2 — Tests + polish.**
Auth-assertion test (anonymous → reject, ADMIN → 200), render-assertion
test (a representative `CatalogContribution` and a representative
`EntityReferences` provider per domain appear under their expected
heading; a domain registered with neither renders an empty section;
a `referenceType()` is rendered as expected for an inverse provider),
`admin/nav.jte` extended to include `/admin/catalog` with
`aria-current="page"` on the active link, styling matched to existing
console pages. Acceptance: full test suite green; manual smoke against
a locally running console with the dev profile passes.

Future enhancements (not part of view 3): per-contribution emitted-token
counts (with explicit click-through or cached compute, not on every
render); per-provider live invocation against a sample target;
cross-linking from each row into `/admin/domain-services` for the
contributing bean's `@DomainService` discovery state.

**Status: Shipped.** M1 landed `AdminCatalogController` +
`admin/catalog.jte`, injecting `List<DomainId>`, `List<CatalogContribution>`,
and `List<EntityReferences<?>>` directly and grouping by
`DomainId.value()`. M2 added `AdminCatalogControllerWebMvcTest`
(auth, ordered-headings, plants contribution + provider with
`CompoundName` referenceType, empty-section rendering for chemistry and
insects, and nav `aria-current`) and extended `admin/nav.jte` with the
Catalog link. Visual styling stays with the `banner-styling` lane that
owns the `.admin-nav` and inner-group rules.

---

## Anticipated views (in likely landing order)

1. **`/admin/resilience` — registered strategies.** ✅ Shipped (M1–M4).
   Lists strategy names grouped by primitive (retry / timeout /
   circuit-breaker / bulkhead). Future enhancement: per-strategy
   config values; per-strategy live state (Resilience4j-side).
2. **`/admin/domain-services` — discovered beans.** ✅ Shipped (M1–M2).
   Lists every class the Spring-runtime adapter registered from the
   `@DomainService` marker, grouped by domain package. Confirms that a
   new `@DomainService` actually got picked up.
3. **`/admin/catalog` — assembly state.** ✅ Shipped (M1–M2). Lists
   every `CatalogContribution` and `EntityReferences` provider the
   assembled `Catalog` knows about, grouped by `DomainId`. Surfaces a
   missing contribution before its absence becomes a missing search
   hit.
4. **(future) `/admin/schedules`** — when the scheduled-task runner
   lands.

---

## Open questions (defer until first view lands)

- Form login vs. basic auth vs. mTLS for the admin surface. Form
  login is the lowest-friction starting point; revisit when there is
  a second admin and a real audit need.
- One ADMIN role or fine-grained roles per view? Start with one;
  split when a real reason appears (e.g. a read-only oncall view that
  shouldn't see secrets).
- JSON sibling endpoints (`/admin/resilience.json`) for scripting?
  Defer until a script wants one.
- Log of admin actions (who viewed what, who changed what). Not
  needed for read-only views; required when the first write action
  lands.

---

## Out of scope

- Mutating actions of any kind in v1. Read-only views only. Adding a
  "reset circuit breaker" button is a separate concern with its own
  audit and authorization story.
- Replacing Spring Security. The existing form login + in-memory user
  pattern is sufficient for the operator-of-one phase.
- Any kind of multi-tenant admin model.
