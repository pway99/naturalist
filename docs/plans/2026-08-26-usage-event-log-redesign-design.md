# Usage cost controls — event-log redesign

**Date:** 2026-08-26
**Status:** Approved design, pre-implementation
**Supersedes:** the tally-based internals of
[`2026-08-25-identification-cost-controls-design.md`](2026-08-25-identification-cost-controls-design.md).
The problem statement, cost model, enforcement-in-the-core principle, alerting
delivery, admin UI, and the accepted single-JVM/in-memory durability trade from
that document all still stand — this redesign changes only *how usage is counted
and configured* inside the `usage` module (and moves burst control out of it).

---

## Why revisit a module that just shipped

The shipped design keeps a **rolling tally**: one mutable `UsageTally` row per
`(counter, naturalist, period)`, incremented on each reserve, with fixed period
buckets (`daily-2026-08-26`, `monthly-2026-08`, `rate-2026-08-26-10-30`). Three
things pushed a rethink:

1. **The tally optimizes for volume the domain forbids.** A rolling counter wins
   on storage/read cost only at high volume — but the entire purpose of this
   module is to hold volume *low* (650/month hard ceiling). A denormalized counter
   that must be kept consistent with reality is complexity paying for a race that
   cannot happen. The event table is **bounded by the very limits it enforces**:
   the events you count are exactly the events you rate-limit.

2. **A contract-driven, changeable monthly reset.** The operator's Anthropic
   monthly limit resets on a boundary set by contract, changes over time, and the
   feature may be disabled and re-enabled. With a stored tally, changing that
   boundary means recomputing/migrating a counter. With an event log, "current
   usage" is *always* `count(events since period-start)` — change the configured
   start and the count re-derives itself; disable/re-enable is just reserves
   pausing and old events aging out of the current window. **No migration, ever.**

3. **Burst control is a solved problem elsewhere.** The per-minute rate limit is a
   textbook Resilience4J `RateLimiter`, and the codebase already has the seam
   (`kernels/framework` Resilience facade + `adapters/resilience-resilience4j`,
   surfaced at `/admin/resilience`). Counting rate as usage events was the one
   counter that fit a rolling window; handing it to Resilience removes a window
   semantic from the module entirely.

The event log turns the shipped design's weakness (a changeable reset) into a
strength, right-sizes storage to a system whose volume is capped by construction,
and narrows the module's job to **budget** alone.

## Core principles (retained from the shipped design)

- **Enforce in the core, not at a surface.** `insects-core` calls
  `budget.reserve(naturalist)` at the top of `identify(...)`, before any spend.
  Every UI surface inherits the cap. Unchanged.
- **Count identifications, not raw API calls.** One reservation per identify; each
  identification is ≤ ~$0.06.
- **Single-JVM `synchronized` reserve, in-memory placeholder store.** The check-
  then-insert critical section is `synchronized`; the repository is the house
  in-memory placeholder shared with all six domains. Durability and multi-instance
  atomicity arrive with the project-wide real-store milestone. Accepted trade.

---

## Model

### Entities

- **`UsageEvent`** — `Entity<UsageEventId>`, append-only log and sole source of
  truth. Components: `UsageEventId id`, `UsageCounterName counterName`,
  `NaturalistName naturalist`, `Instant instant`. **Every event carries a real
  naturalist** — there is no null-naturalist "global" event. Global usage is
  `count(all events)`; per-user usage is `count(events where naturalist = X)`:
  same stream, two filters. Replaces `UsageTally`.

- **`UsageCounter`** — `Entity<UsageCounterId>`, the **editable budget rule**.
  Components: `UsageCounterId id`, `UsageCounterName counterName`,
  `UsageScope scope` (`GLOBAL | PER_USER`), `UsageWindow window`, `int limit`,
  `boolean active`. Multiple rows share a `counterName` (a daily rule and a
  monthly rule both meter `identification`). **No naturalist component** — the
  naturalist comes from the reserve call, and `scope` decides count-all versus
  count-for-that-user. This single rule entity collapses the earlier
  Shared/Naturalist split. Mutable via `with*` methods (`withLimit`,
  `withActive`, `withWindow`) so the operator edits it at runtime.

- **`UsageAlert`** — `Entity<UsageAlertId>`, unchanged from the shipped module.
  Threshold event with a `(counterName, scope, kind, period)` unique key that *is*
  the cross-threshold dedup; `emailed`/`acknowledged` flags drive the dispatcher
  and banner.

- **`UsageEntitlement`** — *future, credits; NOT built in this effort.* Would carry
  `(NaturalistName naturalist, UsageCounterName counterName, int granted,
  Instant since)`. Its presence moves a naturalist to the entitled bucket. Named so
  its naturalist is always present. See Buckets.

### Value types

- **`UsageCounterName`** — typed identifier (`EntityName` subclass), lower-kebab.
  Seeded value: `identification` (one generic budget across all organism
  identification). Per-domain siblings (`insect-identification`) become new rows
  later with no schema change. Lives in `domains/identifiers/.../usage/`.

- **`UsageScope`** — enum `{ GLOBAL, PER_USER }`.

- **`UsageWindow`** — `ValueObject` describing which events a rule counts. Two
  implementations ship:
  - **`FixedCalendarDay`** — the current calendar day (UTC). Used by the daily
    counters; gives the user-legible "resets at midnight" promise.
  - **`FixedSince(Instant since)`** — every event at or after a configured instant.
    Used by the monthly budget counter (**Option B**): the operator sets `since`
    to match the current Anthropic contract period and bumps it (or toggles
    `active`) when the contract resets. Maximally flexible, cannot drift out of
    phase with reality, and tolerates disable/re-enable and irregular periods.

  A rolling-window implementation is deliberately **not** built — burst is
  Resilience4J's job. The sealed/interface shape leaves room for it if a future
  counter needs one.

- **`BudgetExceededException`** — unchecked, carries `LimitKind { PER_USER, DAILY,
  MONTHLY, CREDITS }` and a `resetAt` instant. (`RATE` is dropped from
  `LimitKind` — burst rejection is Resilience4J's `RequestNotPermitted`, mapped
  separately.) `resetAt` is now precise: `startOfNextDay` for `FixedCalendarDay`,
  and for `FixedSince` the operator-configured next boundary (or "unknown/contact"
  when the operator has not set a successor).

### The counters shipped

| `counterName`  | `scope`   | `window`               | `limit` | Notes |
|----------------|-----------|------------------------|---------|-------|
| identification | PER_USER  | FixedCalendarDay       | 10      | per-user daily quota; resets midnight |
| identification | GLOBAL    | FixedCalendarDay       | 50      | global daily cap |
| identification | GLOBAL    | FixedSince(`instant`)  | 650     | **Option B** monthly budget; `since` + `active` operator-set to match the Anthropic contract |

Defaults match the shipped "Conservative (~$40/mo)" numbers. Warning threshold
80%. These seed as `UsageCounter` rows and are edited at runtime, not redeployed.

---

## Buckets

A naturalist's bucket is decided by one lookup — **do they have an active
entitlement?**

- **Public bucket** (no entitlement — the only bucket built now): gated by the
  three `UsageCounter` rules above (per-user daily, global daily, global monthly),
  drawing from the operator-funded pool.
- **Entitled bucket** (future — has purchased credits): gated by *their own credit
  balance* and **exempt from the global caps** (they pre-funded that spend). Still
  subject to the Resilience rate limiter (that protects the API key's RPM, not the
  budget, so it applies to everyone).

A credit entitlement is *mechanically the same shape* as the Option-B monthly
counter: a per-user `FixedSince(grantInstant)` window with `limit = granted`.
Balance = `granted − count(that naturalist's events since since)`. Credits add **no
new counting mechanism** and — because an entitled naturalist's events are *all*
credit events, and exhaustion blocks rather than falls back to public — **no bucket
tag on `UsageEvent`**. Everything stays derived from the log.

**Scope:** the entitled-bucket branch exists in `reserve` from day one but
`entitlements.activeFor(naturalist)` always returns absent — no `UsageEntitlement`
entity, no purchase flow — until credits are a real feature.

---

## Reserve

`reserve` lives in `usage-core`'s write adapter and is `synchronized` — the check-
then-insert critical section is what prevents two concurrent callers from both
observing `used == limit - 1` and both inserting. The read it depends on is the
one batched, gated query (below).

```
reserve(counterName = identification, naturalist, now):
  # burst/rate is handled upstream by Resilience4J — not in this method

  entitlement = entitlements.activeFor(naturalist)     # future; today always absent
  if entitlement present:                              # ENTITLED bucket
      used = count(events: counterName, naturalist, since = entitlement.since)
      if used >= entitlement.granted:
          throw BudgetExceeded(CREDITS, entitlement-has-no-reset)
      # exempt from the global caps — fall through to insert
  else:                                                # PUBLIC bucket
      for rule in counters.activeFor(counterName):     # per-user-daily, global-daily, global-monthly
          window = rule.window.current(now)
          used   = count(events matching counterName, rule.scopeFilter(naturalist), window)
          if used >= rule.limit:
              if rule.scope == GLOBAL:
                  recordAlert(HARD_STOP, rule, used)   # deduped by alert unique key
              throw BudgetExceeded(rule.limitKind(), window.resetAt())
          if rule.scope == GLOBAL:
              checkWarning(used + 1, rule)             # WARNING alert at 80%, deduped

  insert UsageEvent(UsageEventId.create(), counterName, naturalist, now)
```

- `rule.scopeFilter(naturalist)` = the reserving naturalist for `PER_USER`, no
  naturalist filter (all events) for `GLOBAL`.
- Alerts fire on **GLOBAL rules only** — per-user rejection is a user-facing flash,
  not an admin alert (unchanged from the shipped design).
- `now` is captured once per call from an injected UTC `Clock` (as
  `UsagePeriods`/its successor does today).

### Counting stays in core — the repository stays a pure entity cache

The repository does **not** grow a `count(*)` responsibility. It exposes a windowed
fetch:

```java
List<UsageEvent> findByCounterSince(UsageCounterName counter,
                                    @Nullable NaturalistName naturalist,
                                    Instant since)
```

and the **count happens in the query/core adapter** (`list.size()`), one batched
read over the widest active window with the sub-windows filtered in memory. This:

- keeps the four repository responsibilities intact (no aggregate logic in the
  cache);
- avoids fan-out — one read per reserve, not one-per-counter — so the N+1
  no-fan-out select gate stays green (the read-side adapter is a recognised
  `*QueryImpl` head-of-DAG, exactly as the shipped CQS split arranged);
- is cheap precisely because the table is bounded by the limits it enforces.

When a real transactional store lands, `COUNT(*)` can be pushed down behind the
*same* port as a deliberate optimization. Not before.

---

## Burst / rate — Resilience4J, outside the module

A global `RateLimiter` (`limitForPeriod = 3`, `limitRefreshPeriod = 1m`) wraps the
vision spend via the Resilience facade in `insects-core`, beside the
`budget.reserve` call. `RequestNotPermitted` is mapped by the app's
`@ControllerAdvice` to the same friendly "try again in a moment" flash as
`BudgetExceededException`. Rate is ephemeral by nature, so the single-JVM caveat
does not bite. No rate `UsageCounter`, no rolling window in the usage model.

---

## Alerting & admin — as shipped, reading off events

`UsageAlert`, the `@Scheduled` best-effort email dispatcher, the `/admin/usage`
dashboard + `.json` endpoint + acknowledge, and the persistent banner all stay.
The only change: the dashboard's gauges and per-user table read **event counts**
(via the read adapter's snapshot) instead of tally rows. Threshold semantics
(warning at 80%, hard stop at 100%, on the two GLOBAL counters, deduped by the
alert unique key) are unchanged.

---

## What changes versus the shipped module

This is a rewrite of internals, **not a data migration** — the shipped module is
the in-memory placeholder with no durable data, so there is nothing to migrate.

- `UsageTally` → **`UsageEvent`** (append-only; `withCount` and the business-key
  finder go away, replaced by the windowed fetch).
- `UsageCounter` reshaped from the metered-**type** catalog into the editable
  budget **rule** entity (gains `scope`, `window`, `limit`, `active`).
- New value types: `UsageScope`, `UsageWindow` (+ `FixedCalendarDay`,
  `FixedSince`). `LimitKind` loses `RATE`, gains `CREDITS`.
- The CQS `UsageQuery` / `UsageCommand` split, the `IdentificationBudget` seam for
  insects, and the three-repository namespace **keep their shape**; their bodies
  change to count-over-events.
- `UsagePeriods` shrinks (no rate slug; period logic folds into `UsageWindow`).
- `insects-core` gains the Resilience `RateLimiter` around the vision call.
- Repository test sources/seeds swap `usage-tallies.json` for `usage-events.json`
  and reshape `usage-counters.json` to the rule schema.

---

## Testing

Same "real infrastructure, real tests" bar as the shipped module (not the
`framework-test` "no test infra for test infra" case):

- **Reserve no-overshoot** under the `synchronized` boundary — concurrent reserves
  at the limit boundary never exceed it.
- **Window boundaries** — `FixedCalendarDay` rollover at midnight; `FixedSince`
  cutoff includes/excludes correctly around `since`.
- **Public-bucket all-or-nothing** — at-limit on any of the three rules rejects
  with the correct `LimitKind` and a precise `resetAt`, and inserts no event.
- **Alert record-once** — warning at 80% and hard-stop at 100% each recorded once
  per period (dedup by the alert unique key); GLOBAL only.
- **Resilience rate wrapper** — the 4th call within a minute is rejected with
  `RequestNotPermitted`; the mapping produces the friendly flash.
- **Repository contract tests** — the windowed fetch (`findByCounterSince`) across
  argument-validation / empty / expected, on both mock and rdms, plus the
  inherited CRUD.
- **Entitled-bucket seam** — with `entitlements.activeFor` stubbed absent, reserve
  always takes the public path (proving the branch is wired but inert).

## Completeness gate

`mvn verify`, then:

```bash
mvn install -DskipTests && mvn rewrite:dryRun -Drewrite.failOnDryRunResults=true
```

## Non-goals (this effort)

- **Credits / purchase flow** — designed as a seam, not built. No
  `UsageEntitlement` entity, no payment.
- **Auto-rolling monthly period** — Option B is explicit `since`; auto-roll from an
  anchor day is a later convenience once the contract cadence is stable.
- **Durable / multi-instance store** — rides the project-wide real-store milestone.
- **Token-accurate dollar accounting** — still count identifications.
- **Authority text-gen coverage** — `kernels/authority` spend stays outside this
  budget.

## Future options

- `UsageEntitlement` + purchase flow (the entitled bucket).
- Auto-rolling monthly window from a configurable anchor day.
- Real transactional store: push `COUNT(*)` down behind the existing fetch port.
- Per-domain counters (`insect-identification`, …) as new `UsageCounter` rows.
