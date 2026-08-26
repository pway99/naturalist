# Identification cost controls — design

**Date:** 2026-08-25
**Status:** Approved design, pre-implementation
**Scope:** Enforcement in the identification **core** use case, behind a service
port, with counters/alerts persisted through a `usage` **domain module** (api /
core / repository-test / repository-rdms), plus app wiring (admin UI + email).

---

## REVISION 2 (2026-08-25) — `usage` as a domain module (supersedes the kernel-port + datastore-adapter structure described further below)

Per review, `usage` is built as a first-class **domain module** following the
house four-part split, not a kernel port plus a bespoke datastore adapter. Where
the older sections below conflict with this revision, this revision governs.

### Module layout — `domains/usage/`

- **`usage-api`** — public service ports `IdentificationBudget` (write) and
  `UsageMonitor` (read); value types `LimitKind`, `AlertScope`, `AlertKind`,
  `UsageLimits`, `UsageSnapshot`, `BudgetExceededException`; the domain entities
  `UsageCounter`, `UsageTally`, `UsageAlert`; and the package-private repository
  namespace (`UsageRepository` class with nested `protected interface
  CounterRepository`, `TallyRepository`, `AlertRepository` extending
  `EntityRepository`). Depends only on framework, identifiers, field-notes.
- **`usage-core`** — the `IdentificationBudget`/`UsageMonitor` implementation: a
  `@DomainService` budget service that orchestrates the reserve over the three
  repositories. The check-and-reserve (read/increment the four `UsageTally` rows,
  all-or-nothing against the limits, record `UsageAlert`s at thresholds) is a
  **`synchronized` core operation** — single-JVM atomic, matching the codebase's
  current persistence reality.
- **`usage-repository-test`** — `TestEntitySource`s (+ JSON seeds) for the two
  entities, the in-memory repository **mocks** (`AbstractTestEntityRepository`-
  backed), and the behavioral **contract tests**. The in-memory implementation
  lives HERE (a test-support module) — **not** on `usage-core`'s runtime classpath.
- **`usage-repository-rdms`** — `@DomainService` subclasses of the mocks, backed
  by the shared in-memory `NaturalistDatabase`, exactly like all six existing
  domains (`docs/plans/2026-08-22-repository-rdms-intermediate-design.md`).
  **Production wires this** (the app pom depends on `usage-repository-rdms`;
  `@DomainService` discovery picks the rdms beans).

### Entities

Three entities: one `NamedEntity` metered-type catalog, and two `Entity` records
(a mutable tally and an alert event) that reference it — matching the codebase's
"a record whose identity is a stable natural key is a `NamedEntity`; an
occurrence/mutable row is an `Entity` with a surrogate id" split. `EntityName`
enforces lower-kebab-case (`^[a-z0-9]+(-[a-z0-9]+)*$`); `period` strings are
hyphen-joined slugs (`daily-2026-08-25`, `monthly-2026-08`, `rate-2026-08-25-10-00`).

- **`UsageCounter`** — `NamedEntity<UsageCounterName>`. The metered **type**. One
  seeded row now: `identification` (generic — a single budget across all organism
  identification; per-domain counters like `insect-identification` become sibling
  rows later with no schema change). Component today: `name` only. **Future home
  for the limit thresholds** (see Limits below) — adding them here is the localized
  change that makes limits RDBMS-persisted and restart-free. FK target for the two
  records below. (`NamedEntity` requires only `name()`; the Durrell `Description`
  is a field-notes convention for catalogued *concepts*, not a requirement —
  `Element`, `Compound`, `GlossaryTerm`, `NaturalistCredential` are Description-less
  `NamedEntity` records. An operational counter type needs none.)
- **`UsageTally`** — `Entity<UsageTallyId>`. The mutable running count — this is
  "the naturalist usage count." Components: `UsageTallyId id`, `UsageCounterName
  counter` (FK), `@Nullable NaturalistName naturalist` (**null = the global
  tally**), `String period` (`daily-<date>`, `monthly-<month>`,
  `rate-<minute>`), `int count`; `withCount(int)`. Unique on `(counter,
  naturalist, period)` — the reserve's four rows are `(identification, <me>,
  daily-…)`, `(identification, null, daily-…)`, `(identification, null,
  monthly-…)`, `(identification, null, rate-…)`. Looked up by a custom finder on
  that business key; idempotent-created at 0 on first touch.
- **`UsageAlert`** — `Entity<UsageAlertId>`. The threshold **event**. Components:
  `UsageAlertId id`, `UsageCounterName counter` (FK), `AlertScope scope`,
  `AlertKind kind`, `String period`, `String message`, `Instant at`, `boolean
  emailed`, `boolean acknowledged`; `withEmailed(...)`/`withAcknowledged(...)`.
  Unique on `(counter, scope, kind, period)` — **that constraint is the dedup**
  (a duplicate-threshold insert throws and is swallowed).

Intra-domain FKs (`UsageTally.counter`, `UsageAlert.counter` → `UsageCounter`) are
enforced by `TestEntitySource.foreignKeyConstraints()`. `UsageTally.naturalist`
is a **cross-domain** reference (`naturalists`), so it declares no in-memory FK
(per the DAG).

### Identifiers

In `domains/identifiers/.../com/naturalist/usage/`: `UsageCounterName`
(`EntityName`), `UsageTallyId` and `UsageAlertId` (`EntityId` subclasses).

### Limits — config now, data later

Thresholds (per-user daily 10, global daily 50, global monthly 650, rate/min 3,
warning 80%) are read from **`application.yml`** (`naturalist.usage.*` →
`UsageProperties` → a `UsageLimits` value) and passed into the reserve. This keeps
the first cut simple. The design deliberately isolates limits behind the
`UsageLimits` value object so the later switch to **RDBMS-persisted, restart-free**
limits is a localized change: add the threshold components to `UsageCounter` and
source `UsageLimits` from that entity instead of `UsageProperties` — no change to
the reserve logic, the tally, or the alerting.

### Enforcement (unchanged from Revision 1)

`insects-core` depends on **`usage-api`** (core → another domain's api is allowed)
and calls `budget.reserve(naturalist)` at the top of `identify(...)`. Every UI
surface inherits it. The port and the reserve semantics are unchanged; only their
home (kernel → `usage-api`) and their backing (adapter → repository stack) move.

### Reserve & the contract

`reserve(naturalist)` in `usage-core` is `synchronized`: it resolves the four
`UsageTally` rows for the `identification` counter (per-user-day, global-day,
global-month, rate-minute — created at 0 if absent), checks each against its limit
all-or-nothing, increments them on success, and records `UsageAlert`s at the 80%
and 100% thresholds (dedup by the alert's unique key). A `usage-core` test proves
the no-overshoot guarantee on the wired mock (which the rdms subclass shares). The
repository **behavioral contract tests** (`usage-repository-test`) cover the
inherited CRUD plus each entity's custom finders (the `UsageTally` business-key
lookup, the `UsageAlert` dedup lookup) on both mock and rdms.

### Durability scope — ACCEPTED trade

Because `usage-repository-rdms` mirrors the house **in-memory placeholder**, the
counters are **in-memory today — not durable across restart, not shared across
servers**. This is a deliberate, accepted trade for structural consistency: `usage`
rides the same future "wire a real transactional/JDBC adapter app-wide" milestone
as all six domains, at which point durability and multi-instance atomicity become
real for every domain at once. **Until then the cost cap is a single-instance,
resets-on-restart guard.** The synchronized reserve + the behavioral contract make
the durable behavior correct-by-construction the moment the real store lands.
**"Milestone D — durable store" is removed** (folded into that project-wide future
milestone); the earlier "durable, shared, transactional datastore / adapter"
language below is superseded.

### What still stands from Revision 1

Limits/defaults (Conservative: 10 / 3 / 50 / 650 / 80), the alerting behavior
(persisted alerts, in-console banner, `/admin/usage.json`, best-effort email, at
80% warning + 100% hard-stop on daily and monthly), and the admin UI — all
unchanged. They wire to `usage-core`'s `UsageMonitor` bean instead of a kernel bean.

---

## Problem

The organism-identification path spends real money on the production Anthropic
API key and is **completely ungoverned**. The only control today is a request
timeout. There is no per-user quota, no daily or monthly ceiling, and no rate
limit. A burst of use — or one misbehaving client — can run up an unbounded bill
before anyone notices, and there is no operational logging to notice it *by*.

Two Anthropic-backed ports spend money:

- `VisionService` (Sonnet 4.6) — insects identify. Up to **2 calls** per
  identification (turn 1 propose; turn 2 `VisionExchange.respond` feature-dedup,
  only when a proposed feature collides).
- `TextGenerationService` (Haiku 4.5) — insects identify enrichment (**up to 3**,
  one per new parent rank) *and* `kernels/authority` (`AuthorityContent`, not
  user-initiated — see Non-goals).

## Design principles that shaped this (from review)

1. **Enforce in the core, not at a surface.** An earlier draft put the per-user
   quota in a Spring `HandlerInterceptor` and the global limits in composition-root
   adapter decorators. Both are **surface-specific**: a future mobile app would hit
   different endpoints / wire its own composition root and re-open the hole. The
   identification command **already receives `NaturalistName`**, so enforcement
   belongs in the use case, keyed by the userId that is already a parameter. One
   enforcement point; every surface that invokes identification inherits it.

2. **Count identifications, not raw API calls.** One reservation per identify.
   Simpler, surface-independent, and it *is* "a reasonable number of requests per
   user." Cost stays bounded because each identification is ≤ ~$0.06.

3. **Durable, shared, transactional state.** The app may run **multiple instances**
   (scale-out, rolling restart). In-memory counters or a local JSON file would let
   each instance keep its own tally and enforce `limit × instance-count`. Counters,
   the rate window, and alerts all live in one PostgreSQL datastore; check-and-
   reserve is a single atomic transaction so racing instances cannot overshoot.

## Cost model (grounds the limits, not billing)

| Call | Model | ~$/call |
|---|---|---|
| Vision turn 1 | Sonnet 4.6 ($3/$15 per 1M) | ~$0.017 |
| Vision turn 2 (dedup) | Sonnet 4.6 | ~$0.021 |
| Text-gen enrichment (×0–3) | Haiku 4.5 ($1/$5 per 1M) | ~$0.006 |

Per identification: typical **$0.02–0.03**, worst case (2 vision + 3 enrichment)
**$0.05–0.06**. Image tokens are server-capped (~1,600); the prompt prefix is
cached. Estimates ±50% — they size limits, not bills.

## Architecture

### Kernel port — `IdentificationBudget`

New kernel (`kernels/usage`), mirroring the `Resilience` facade: a port interface,
a `NoOp` default, and the `BudgetExceededException`. Referenced by `insects-core`
(and future `plants-core`); depends only on `framework` + `identifiers`
(`NaturalistName`).

```java
public interface IdentificationBudget {
    /** Reserve one identification for this naturalist, or throw. Atomic. */
    void reserve(NaturalistName naturalist);   // throws BudgetExceededException
    static IdentificationBudget noOp() { ... }  // unwired composition roots / unit tests
}
```

`BudgetExceededException` (unchecked) carries `LimitKind { PER_USER, RATE, DAILY,
MONTHLY }` and a `resetAt` instant.

### Enforcement site — the core command

`InsectIdentificationCommand.identify(image, file, naturalist, notes)` calls
`budget.reserve(naturalist)` **once, first**, before any vision work. Verified
2026-08-25: the command calls `visionService.identify`, `exchange.respond`, and
`textGenerationService.generate` with **no try/catch**, so `BudgetExceededException`
propagates to the caller (console controller today, any surface tomorrow).

`naturalist` is already required in practice (the controller only calls the command
for a signed-in user). `reserve` makes it a hard precondition of identification.

### Datastore adapter — concrete technology deferred

The `IdentificationBudget` impl lives in a datastore adapter behind the port. The
**specific store is not decided at design time** and does not block anything: the
port contract is the interface, and the adapter's only obligation is to provide four
properties. Any store that offers them (a relational DB with atomic conditional
upsert, Redis with atomic `INCR`/Lua, a cloud KV with conditional writes) is a valid
target; the choice is made when the adapter is built.

**Required properties of the store:**

1. **Durable** — survives restart/redeploy; the budget is not re-opened by a bounce.
2. **Shared** — one store for every app instance (not per-process memory or local disk).
3. **Atomic check-and-reserve** — "increment iff below limit" is a single atomic
   operation, so racing instances cannot overshoot.
4. **Atomic dedup** — an alert can be claimed exactly once across instances (unique
   constraint / conditional insert), so exactly one instance emails.

The rest of this section is a **reference implementation against a relational DB
(illustrative, not a commitment)** — plain JDBC + a `javax.sql.DataSource` supplied
by the app, schema via a startup migration, holding the configured `UsageLimits`.

**Reference schema:**

```sql
CREATE TABLE usage_counter (
  counter_key text PRIMARY KEY,   -- see key scheme below
  count       integer NOT NULL DEFAULT 0
);

CREATE TABLE usage_alert (
  id           bigserial PRIMARY KEY,
  scope        text NOT NULL,     -- DAILY | MONTHLY
  kind         text NOT NULL,     -- WARNING | HARD_STOP
  period       text NOT NULL,     -- e.g. 2026-08 or 2026-08-25
  message      text NOT NULL,
  created_at   timestamptz NOT NULL DEFAULT now(),
  emailed      boolean NOT NULL DEFAULT false,
  acknowledged boolean NOT NULL DEFAULT false,
  UNIQUE (scope, kind, period)    -- cross-instance dedup
);
```

**Counter keys** (one durable row each):

| Key | Enforces |
|---|---|
| `user:<slug>:<yyyy-mm-dd>` | per-user daily quota |
| `global:<yyyy-mm-dd>` | global daily cap |
| `global:<yyyy-mm>` | monthly budget |
| `rate:<yyyy-mm-dd-hh-mm>` | global rate (fixed 1-min window) |

**`reserve(naturalist)` — one transaction, all-or-nothing.** In a fixed key order
(monthly → daily → rate → per-user, to avoid deadlocks), each counter is
atomically conditional-incremented:

```sql
INSERT INTO usage_counter(counter_key, count) VALUES (:key, 1)
ON CONFLICT (counter_key)
  DO UPDATE SET count = usage_counter.count + 1
  WHERE usage_counter.count < :limit
RETURNING count;
```

- **No row returned** for some key → its limit is reached. Roll back the whole
  transaction (any earlier increments undone), record the hard-stop alert
  (`INSERT … ON CONFLICT DO NOTHING`), and throw `BudgetExceededException(kind,
  resetAt)`. Because increments are serialized by the DB, two instances at the
  boundary cannot both pass — no overshoot, no lost counts.
- **All rows returned** → committed. If any returned `count` equals the warning
  threshold `ceil(limit × warningPercent)`, exactly one reservation observes that
  equality (serialized increments), so it records the warning alert (deduped).

Old `rate:*` and past-day `user:*` rows are pruned by the scheduled sweeper (below);
they are tiny, so pruning is housekeeping, not correctness.

### App wiring (`apps/management-console`)

- **Datastore + migration.** First real datastore in the console — the app provisions
  the store the chosen adapter needs (for the relational reference: `DataSource` +
  startup migration, config from env). Domain data stays file-based JSON for now —
  out of scope.
- **Wire the adapter** as the `IdentificationBudget` bean (composition root), so the
  insects command receives it. `NoOp` remains the default for tests / unwired roots.
- **Alert dispatcher** — a `@Scheduled` sweeper: claims un-emailed alert rows
  (`UPDATE usage_alert SET emailed = true WHERE id = ? AND NOT emailed`, rows-affected
  gate) and emails them. Cross-instance safe: exactly one instance sends each alert.
  Email out of the request hot path.
- **Admin UI** — `UsageController`: `GET /admin/usage` (dashboard, beside
  `/admin/resilience`), `GET /admin/usage.json` (machine-readable), `POST
  /admin/usage/alerts/{id}/ack`. Reads counters + alerts straight from Postgres, so
  every instance shows the true cluster-wide picture.
- **Banner** — the existing `NaturalistHeaderInterceptor` publishes an
  "unacknowledged alerts present" request attribute; the console layout renders a
  persistent banner until acknowledged.
- **Exception handling** — an app `@ControllerAdvice` maps `BudgetExceededException`
  to a friendly flash + redirect to the identify form.

## Defaults — Conservative (~$40/mo), all editable in `application.yml`

Counted in **identifications** (~$0.06 worst / ~$0.025 typical each):

```yaml
naturalist:
  usage:
    per-user-daily: 10          # ~$0.60/user/day worst case
    global-rate-per-minute: 3   # burst control
    global-daily: 50            # ~$3/day worst case
    global-monthly: 650         # ~$40/mo worst case  ← hard ceiling
    warning-percent: 80
    alert-email: patway99@gmail.com
```

Monthly budget is the hard cost ceiling; when reached, identification returns the
"temporarily unavailable" flash until the next month.

## Alerting & monitoring (there is no operational logging)

Triggers — **global daily cap and monthly budget only** (per-user quota is a
user-facing flash, not an admin alert):

- **Warning** at `warning-percent` (default 80%) of daily and of monthly.
- **Hard stop** at 100% (the trip that pauses identification).

Dedup is structural: the `usage_alert` unique key `(scope, kind, period)` guarantees
one alert per threshold per period across all instances.

**Delivery — all three:**

1. **In-console banner** — persistent on every page until acknowledged; state is in
   Postgres, so it survives restarts and is consistent across instances.
2. **Machine-readable endpoint** — `GET /admin/usage.json` (usage + active alerts)
   for an external uptime monitor to poll.
3. **Email** — `spring-boot-starter-mail` + `JavaMailSender`, SMTP from
   `spring.mail.*` env, recipient `naturalist.usage.alert-email`. Sent by the
   scheduled dispatcher, **best-effort**: if SMTP is unconfigured it no-ops (banner +
   endpoint still work). This is the admin notifying their own address — an
   operational capability, not user-facing mail.

**Dashboard `/admin/usage`:** daily and monthly gauges (green → amber at
warning-percent → red at 100%), per-user table (slug → today's identifications /
limit), current rate-window count, and an active-alerts panel with acknowledge
buttons.

## Testing

Enforcement logic gets real tests (app/adapter operational infrastructure, not the
`framework-test` "no test infra for test infra" case).

- **The datastore adapter** against a real instance of its store (e.g. Testcontainers):
  reserve increments the right counters; at-limit rejects with the correct `LimitKind`;
  **concurrent reservations from parallel connections do not overshoot** (the core
  multi-instance guarantee); day/month rollover; warning + hard-stop alerts recorded
  once (dedup under contention); prune removes stale rows.
- **`InsectIdentificationCommand`**: calls `reserve` before any vision call; a thrown
  `BudgetExceededException` aborts identification and performs no API call.
- **Alert dispatcher**: claims each alert once across simulated instances; emails when
  configured, no-ops (no throw) when not.
- **`UsageController`**: `/admin/usage.json` shape; acknowledge clears the banner.
- **`NoOp` budget**: `reserve` is a pass-through (unit-test/default path).

## Rollout notes

- New kernel `kernels/usage`; new datastore adapter (technology chosen at build time);
  new app dep `spring-boot-starter-mail`, plus whatever the adapter's store needs.
- Provision the durable store; supply its connection config (and `spring.mail.*` for
  email) via env. The store is the budget of record — it must be the **same** shared
  store for every app instance.
- `mvn verify`, then the completeness gate:
  `mvn install -DskipTests && mvn rewrite:dryRun -Drewrite.failOnDryRunResults=true`.

## Non-goals (deliberate)

- **Token-accurate dollar accounting.** Count identifications; the per-identification
  ceiling is a safe over-estimate. Reading `response.usage()` is a future option.
- **Authority text-gen coverage.** `kernels/authority` calls `TextGenerationService`
  outside the identification use case (system-initiated citation/EOL content). It is
  **not** covered by this budget. Known boundary; if needed later, authority can call
  the port with a system key.
- **Migrating domain data to Postgres.** Only the usage counters/alerts use the DB;
  domain catalog data stays file-based JSON for now.
- **Per-user monthly limits; runtime-editable limits.** Config-only, redeploy to change.

## Future options

- Token-accurate accounting via `response.usage()`.
- Spring Actuator health contributor mirroring `/admin/usage.json`.
- Extend the budget port to authority / other cost use cases with a system key.
