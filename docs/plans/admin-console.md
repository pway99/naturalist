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

## Anticipated views (in likely landing order)

1. **`/admin/resilience` — registered strategies.** First view, drives
   the kernel-side `Resilience.registeredNames()` addition. Lists
   strategy names grouped by primitive (retry / timeout /
   circuit-breaker / bulkhead). Future enhancement: per-strategy
   config values; per-strategy live state (Resilience4j-side).
2. **`/admin/domain-services` — discovered beans.** Lists every class
   the `DomainServiceScan` registered, grouped by domain package.
   Confirms that a new `@DomainService` actually got picked up.
3. **`/admin/catalog` — assembly state.** Lists every
   `CatalogContribution` and `EntityReferences` provider the assembled
   `Catalog` knows about, with their `DomainId`. Surfaces a missing
   contribution before its absence becomes a missing search hit.
4. **(future) `/admin/schedules`** — when the scheduled-task runner
   lands.

Each view is small enough to fit in one PR.

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
