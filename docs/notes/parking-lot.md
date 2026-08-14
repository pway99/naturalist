# Parking Lot

Short entries (~5 lines each) for forks discovered mid-work and decisions in flight. Not loaded into conversation context — reference explicitly when working the relevant question. Entries move to [`parking-lot-resolved.md`](parking-lot-resolved.md) when the answer lands in code or a strategy doc.

**Shape per entry:** *what surfaced* / *when raised* / *where in the work it came up* / *blocking the current slice yes/no* / *resolution path if known*.

---

## PL-3 — Aggregate × CatalogEntity / Entity composition ADR

**Raised:** Pre-Phase-1b (rolled forward from old `Q0`).
**Where:** Identity model — `Aggregate` sits orthogonal to `NamedEntity` / `Entity`. A `Colony` (Apiary) or `NaturalistJournal` is an Aggregate (consistency boundary) but may also be a `CatalogEntity` (stable, named, referenced).
**Blocking:** Not currently — no aggregate-shaped work is in flight on the path. Becomes relevant if PL-1's resolution promotes `InsectSpecies` / `InsectGenus` / `InsectFamily` to true `Aggregate`.
**Resolution path:** ADR documenting how `Aggregate`, `CatalogEntity`, and `Entity` compose, before any aggregate root is implemented.

---

## PL-4 — IrrigationEvent repository adapter shape

**Raised:** Pre-Phase-1b (rolled forward from old `Q1`).
**Where:** Soil/Sensor backlog (no active slice).
**Blocking:** No.
**Resolution path:** Decide between PostgreSQL standard table, TimescaleDB hypertable, or separate time-series store. In-memory adapter is sufficient until RDBMS work begins.

---

## PL-5 — Elemental Sulfur amendment type

**Raised:** Pre-Phase-1b (rolled forward from old `Q2`).
**Where:** Soil chemistry domain; placeholder currently uses potassium sulfate.
**Blocking:** Blocks nitrogen status computation (PL-6).
**Resolution path:** Add a dedicated `ELEMENTAL_SULFUR` amendment type before that computation lands.

---

## PL-6 — Nitrogen status computation

**Raised:** Pre-Phase-1b (rolled forward from old `Q3`).
**Where:** Soil domain.
**Blocking:** No (no active soil work).
**Resolution path:** Needs biological amplification factor from Zone — cross-module dependency mediated through a `ZoneService` port.

---

## PL-7 — Plants growth form vs category

**Raised:** Pre-Phase-1b (rolled forward from old `Q5`).
**Where:** Plants domain.
**Blocking:** No.
**Resolution path:** `GrowthForm` enum (tree/vine/etc.); proposed categories — FruitTree, FruitVine, VegetableCrop, CoverCrop, OrnamentalWoody, PollinatorPlant. `PlantRole` as many-to-many. `SeedLineage` for the Italian Pear adaptation program. Entity model sketched, no Java written.

---

## PL-8 — Custom WH51 calibration

**Raised:** Pre-Phase-1b (rolled forward from old `Q6`).
**Where:** Sensor domain.
**Blocking:** No.
**Resolution path:** Factory calibration is for mineral soil; Oak Vista worm casting/coco coir blend reads 3–5% high. AD values usable for custom calibration curve. Deferred pending gravimetric correlation study.

---

## PL-9 — Event-based production adapter

**Raised:** Pre-Phase-1b (rolled forward from old `Q7`).
**Where:** Cross-domain (event sourcing question).
**Blocking:** No.
**Resolution path:** Append-only PostgreSQL tables for `AmendmentEvent`, `SensorReading`, `IrrigationEvent` as a middle path. Deferred; hexagonal architecture makes this a swap of adapters.

---

## PL-10 — `with*` mutator observability pattern

**Raised:** 2026-05-12.
**Where:** Surfaced while threading `namedEntityOrNull` through `InsectFamily`. Piloted on `InsectFamily.withEgg` in commit `150009d`:

```java
public InsectFamily withEgg(@Nullable EggStage value) {
    observer.arguments("withEgg", i -> i.namedEntityOrNull(value, "value"))
            .throwWhenInvalid();
    return new InsectFamily(name, …, value, …);
}
```

**The smell.** Today's `with*` mutators are plain constructors — they accept whatever the caller passes and return a new record. An invalid child entity (e.g. a `PupaStage` with a stale enum string from a downstream deserialiser) flows through `family.withPupa(stage).withEgg(…)` and only surfaces at the next insertion site, or worse, never. The InsectFamily pilot validates input at the mutation boundary instead, throwing `InvariantViolationException` immediately and surfacing the failure on the observer-framework dashboard.

**Blocking:** No. Phase 3 lands cleanly without it.

**Why it may earn its keep, despite the cost:**

- Each `with*` adds ~3 lines of Observer.arguments boilerplate.
- Each pattern adoption requires at least one new test confirming invalid input throws (and that valid input — including `null` for nullable fields — passes through).
- BUT — adoption gives **per-mutation control and awareness**: you know exactly when and where an object got into a bad shape, which is a real diagnostic win. The pattern's analogue in Pat's day-job framework caught a production bug (DB returned a String with extra whitespace that failed to deserialise to an enum); the dashboard metric pinpointed it in minutes where boundary-only validation would have surfaced it as a downstream EntityNotFoundException with no breadcrumb.

**Resolution path (when revisited):**

1. Decide scope: just the holometabolous-stage `with*` family across InsectFamily / InsectGenus / InsectSpecies, or every `with*` on every record project-wide?
2. Settle the Observer-source question. Today each pilot adds `private static final Observer observer = Observer.forClass(X.class);`. Acceptable; or possibly a thread-local / injected observer if dashboard metrics need consumer routing.
3. Write a "withFoo preserves Bar" round-trip test per mutator at the same time (catches the *other* silent failure — a `with*` method that drops an unrelated field, which the Lombok-`@With` discussion identified as an ongoing tax of the no-Lombok rule).

---

## PL-11 — `ResilienceNameValidator` belongs in `spring-runtime`

**Raised:** 2026-08-14.
**Where:** Shipped in commit `39e50d38` inside `apps/management-console` under `com.naturalist.console.resilience`. It is the `DomainServiceScan` shape — a runtime bridge reading a kernel marker (`@Resilient`, from `framework`) and turning it into Spring behaviour — so `adapters/spring-runtime/` is its home, with the `@Bean` registration staying in the app. `resilience-resilience4j` is ruled out: it would put Spring on the vendor bridge's classpath, which `adapters/CLAUDE.md` forbids. `ResilienceComplianceTest` stays in the app regardless — `@AnalyzeClasses` sees only the scanning module's classpath, so from an adapter it would pass vacuously.
**Blocking:** No. Correct behaviour today; the cost is a copy-paste when app #2 (sync daemon, importer, field-guide API) needs the same gate, which `apps/CLAUDE.md` forbids resolving by app-to-app dependency.
**Resolution path:** Move the class, make it public, genericise the error message (it hardcodes `ResilienceConfiguration.class.getName()`), and swap `Resilience4jResilience` for a stub in the test — adapter-to-adapter test deps are disallowed. Decide the lifecycle interface in the same change: `ApplicationRunner` needs `spring-boot` added to `spring-runtime`, narrowing that adapter from "any Spring context" to "Spring Boot"; `SmartInitializingSingleton` is spring-context only and fires during refresh, so the app dies before the web server binds rather than a moment after.

---

## PL-12 — Shared console module and the `page.jte` ownership inversion

**Raised:** 2026-08-14.
**Where:** Surfaced asking whether the resilience gates belonged in an adapter. Two distinct problems get conflated under "each console module defines its own dependencies". (a) *Pom duplication* — the genuinely universal set across all five `*-console` modules is only the `spring-boot-dependencies` BOM import, `spring-web`, `spring-context`, and test-scope `jte`; everything else is per-domain. A new artifact to carry four lines is the wrong shape; this is `dependencyManagement` in `domains/pom.xml` or a shared console parent, and each console currently parents to its *domain* pom, so it is a re-parenting decision. (b) *Shared code* — every domain console template calls `@template.layout.page(...)`, but `page.jte` lives in `apps/management-console/src/main/jte/layout/`. Five library modules render against a template owned by the app that composes them, which is why `page.jte` may reference only spring-web types and never app/security/servlet-api. That constraint is enforced by nothing but memory.
**Blocking:** No. Console modules build and render correctly today.
**Resolution path:** Treat (a) and (b) separately — (a) is a parent-pom change with no DAG edge. For (b), inventory the actual shared surface (`layout/page.jte`, `admin/nav.jte`, whatever the per-domain `EntityRefLinker` implementations share) before proposing a module shape; note that `insects-console` and `soil-console` already depend on `library-console`, so console-to-console coupling has started ad hoc and a shared module would need to say whether it replaces that.

---

## Conventions

- New entries get the next `PL-N` ID; numbers are never reused.
- Resolving an entry means the answer landed in code or in a strategy doc (identification.md, structural-commitments.md, an ADR). Move the entry to [`parking-lot-resolved.md`](parking-lot-resolved.md) with a `**Resolved:**` line; the live file stays scannable, grep still finds the history.
- Don't ripple to other docs when raising an entry. The parking lot is the one place a fork lands. Strategy docs only update when the question is *resolved*.
