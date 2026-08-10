# Design memo — associating soil profiles with a naturalist

**Date:** 2026-08-09
**Status:** Design agreed, implementation not started
**Scope:** Where the naturalist ↔ soil ownership link lives, its cardinality, and
what changes to build it. No code in this memo — this is the shape we implement from.

---

## 1. Current state

### What a "soil profile" is

`SoilProfile` ([soil-api](../../domains/soil/soil-api/src/main/java/com/naturalist/soil/SoilProfile.java))
is a **`ReadModel`** — assembled on read by `SoilProfileFactory`, never stored. It pairs:

- a persisted **`SoilProfileInfo`** root (`NamedEntity<SoilProfileName>`, `@AggregateRoot`), and
- an append-only **`List<LabAnalysis>`**, most-recent-last — each analysis carrying its
  nutrient panel and physical characteristics.

Identity is a **`SoilProfileName`** slug: `"box1"`, `"backyard-north"`,
`"backyard-center"`, `"backyard-south"`.

The "profile that builds up over time" **already exists** as that ordered `LabAnalysis`
history. A profile is one soil unit observed repeatedly; samples accrete into it. They do
**not** spawn a new profile per date.

### The association today is *spatial*, not *personal* — and has no top

The original hunch — "profiles are unattached, project-level catalog data" — is only half
right. They are attached, but to a **place**, not a **person**:

- `SoilProfileInfo` carries `ZoneName` (**always**) + optional `SubZoneName`
  ([SoilProfileInfo](../../domains/soil/soil-api/src/main/java/com/naturalist/soil/SoilProfileInfo.java)).
- `LabAnalysisInfo` carries `SoilProfileName` + `CropName`.
- **No `NaturalistName` appears anywhere in the soil domain.** Confirmed — zero references.

`Zone` is the site: *"A named physical space… Exists independently of its occupants."* But
`ZoneInfo` carries only `name` + `type` — no owner
([ZoneInfo](../../domains/zone/zone-api/src/main/java/com/naturalist/zone/ZoneInfo.java)) —
and there is **no concept above Zone at all.** The whole hierarchy —
Zone → SubZone → SoilProfile — is owner-less, rooted at nothing, implicitly single-property
("Oak Vista"). `SubZone` already holds a soft back-reference to its `SoilProfileName`.

`Naturalist` ([Naturalist](../../domains/naturalists/naturalists-api/src/main/java/com/naturalist/naturalist/Naturalist.java))
is a person — `NamedEntity<NaturalistName>` — and owns no collection referencing places or soil.

---

## 2. Proposed shape

### The key realization: places are shared *or* private, so ownership needs a root above Zone

Zones are not uniformly ownable. A **national forest** or public park is shared — many
naturalists observe there, nobody owns the soil. A **home garden** is private — one
naturalist owns it. A soil profile a naturalist builds on public land is still *an
observation of a shared place*, whereas one in their backyard is *theirs by virtue of
owning the place*. Ownership therefore cannot live on the Zone (which may be public); it
needs a **root above Zone that carries the public/private distinction**.

That root is **Property**.

```
Naturalist (1) ── owns ──> Property (many, PRIVATE only)

Property (PUBLIC | PRIVATE, optional owner)
  └─ Zone (belongs to a Property; the shared physical reference)
       └─ SubZone
            └─ SoilProfile (1 per spatial unit)
                 └─ LabAnalysis (many, time-ordered)
```

### The link: owner on Property only

- **New aggregate: `Property`** — `NamedEntity<PropertyName>`. Fields: `name`, a
  `PropertyVisibility` enum (`PUBLIC | PRIVATE`), and `@Nullable NaturalistName owner`.
  Aggregate invariant: **`PRIVATE ⇒ owner present; PUBLIC ⇒ owner absent`.** Property is
  the tenancy root — "a naturalist defines their property and the zones within it."
- **`Zone` gains `PropertyName property`** — a soft FK to its parent Property. This
  supersedes the earlier idea of putting `owner` on `Zone`; the owner moves **up** to
  Property, and Zone stays a shared physical reference.
- **The soil domain does not change.** `SoilProfileInfo`, `LabAnalysisInfo`, and the
  `SoilProfile` ReadModel are untouched. A profile's owner is *derived* by two hops:
  `SoilProfile.zoneName → Zone.property → Property.owner`.

### Consequence: public-land profiles are ownerless (accepted for v1)

On a `PUBLIC` Property the owner is absent, so soil profiles there resolve to **no owner**.
This is the deliberate simplification: v1 models *stewardship of one's own holdings*, not
personal attribution of observations on shared land. If "the profile I built at the
national forest is *mine*" must hold later, that is per-observation attribution — a
separate `NaturalistName` on the profile or its analyses — see open questions (a) and (d).
It is intentionally **not** in this design.

### Placement decisions (react at review)

- **`Property` lives in the zone domain** as the new aggregate root above Zone, in its
  **own sub-context package `com.naturalist.zone.property`** (package-private internals).
  Same spatial bounded context (holdings → zones → sub-zones), lowest ceremony — but the
  seam is drawn now *exactly where a future module boundary would fall*, so promotion is a
  package→module lift rather than a detangling. There is no reason to pay for a separate
  module today: Property needs only `NaturalistName` (which `zone-api` already imports from
  `identifiers`), nothing depends on Property except by name (`PropertyName`), and it has
  no separate lifecycle. Escape hatch: split into `domains/property/` when — and only
  when — one of those changes (see open question (f)).
- **`PropertyName` lives in `domains/identifiers/`**, alongside `ZoneName` and
  `NaturalistName`, so a future "properties I own" view from the naturalist side needs no
  later cycle-break. `zone-api` may import both `PropertyName` and `NaturalistName` from
  `identifiers`; no DAG violation.

### Cardinality

- A **naturalist owns many properties** (private ones); a **private property has exactly
  one owner**; a **public property has none**.
- A **property has many zones**; a **zone belongs to exactly one property**.
- A **zone has many soil profiles** (via its sub-zones / whole-zone profile); a **profile
  has many analyses over time** — the existing append-only history. Depth strata and dates
  are *not* separate profiles; profile identity is spatial.

### Multi-tenant is now genuinely required

Defining "my property and its zones" is a real per-naturalist write + scope path, not a
UI lens over a shared catalog. Two mechanisms:

- **Session-identity seam** — the console learns who is acting from the
  `"naturalist.currentNaturalistName"` request attribute (written by
  `NaturalistHeaderInterceptor`, read as `currentNaturalist(...)` in
  [InsectsController](../../domains/insects/insects-console/src/main/java/com/naturalist/insects/console/InsectsController.java);
  console modules must **not** import Spring Security — the `page.jte` classpath invariant).
  On the future create-property write path, `owner` is set from this seam.
- **`PropertiesByOwner` query** — the tenancy-scoping read port ("the properties, and thus
  zones and soil, this naturalist owns"), with a mock method that validates
  (`observer().arguments(...).throwWhenInvalid()`) and a null-rejection contract test. This
  is what makes the app multi-tenant rather than single-property.

The current soil console is read-only, so this design requires no *soil* console change; the
console work lands with the Property write path and the tenancy scope.

---

## 3. Impact list

**New — Property (in the zone domain):**

- `Property` aggregate + `PropertyVisibility` enum in `zone-api`, in the
  `com.naturalist.zone.property` sub-context package, with the `PRIVATE ⇔ owner` invariant.
- `PropertyName` in `domains/identifiers/` (cross-domain identifier).
- Property `TestEntitySource` + JSON catalog; a seed **private** Property for Oak Vista
  owned by a seed `Naturalist`, plus at least one **public** Property to exercise the
  ownerless path.
- Property repository + mock + behavioral contract test.
- `PropertiesByOwner` query (mock method + null-rejection contract test) — the tenancy seam.

**Zone domain:**

- `ZoneInfo.java` — add `PropertyName property`. **Record arity change**: every
  `new ZoneInfo(...)` construction site breaks — Zone aggregate assembly / factory, the
  `ZoneInfo` TestEntitySource, JTE templates, tests. Grep the whole repo; do not trust a
  partial file list.
- Zone `TestEntitySource` + `zone/*.json` — every zone gains a `property` slug; Oak Vista
  zones point at the seed private Property.
- Referential integrity: zone → property is a new soft FK; property → naturalist another.
  Both deferred to the RDBMS layer per convention.

**Naturalist domain:** a real `Naturalist` must exist as the seed owner referenced by the
private Property. No record change.

**Soil domain:** **no record changes, no fixture changes.** Owner is derived through
`zoneName → property → owner` at the application layer (no repo-layer cross-domain join).

**Console:** **no change required by this design.** Soil console is read-only; the
session seam already exists. Console work arrives with the Property write path and tenancy
scoping, not here.

**Migration / nullability:** existing zones need a `property`. Land it non-null by
creating the seed Oak Vista Property and pointing every current zone at it in the same
change. `Property.owner` is `@Nullable` by type (public case) but constrained non-null for
the private seed by the aggregate invariant.

---

## 4. Open questions

- **(a) Per-observation attribution on public land.** The chosen model leaves public-land
  profiles ownerless. If "the profile I built at the national forest is mine" must hold,
  add a `NaturalistName observedBy`/`createdBy` on `SoilProfileInfo` (one profile per
  naturalist per shared zone) or on `LabAnalysisInfo` (shared profile, per-sample
  attribution). Distinct from Property ownership; different cardinality. Revisit when
  public-land observation is a real workflow.
- **(b) Co-stewardship of a private property.** One owner today. Family, a teaching group,
  or a caretaker handoff wants multiple naturalists per Property with roles — a
  `PropertyStewardship` membership entity, not a single FK.
- **(c) Ownership transfer + history.** `owner` is a mutable field with no audit trail; a
  property changing hands is a silent overwrite today.
- **(d) Visibility granularity.** Visibility sits on Property. Can a private property expose
  a single public zone (a community garden plot), or a public property hold a privately
  managed zone? If so, visibility (and possibly a zone-level owner override) moves partly
  onto Zone. Kept at Property-only for now.
- **(e) Where tenancy is *enforced*.** Setting `owner` records ownership; it does not yet
  stop naturalist A from loading naturalist B's properties. True isolation — row scoping via
  `PropertiesByOwner` at the query/RDBMS layer — is a security decision to make when the app
  actually serves multiple tenants, distinct from the data-model annotation.
- **(f) Property as its own domain.** Kept in the zone domain for now (see §2 placement).
  Promote to `domains/property/` when a real trigger appears — Property needs a dependency
  Zone shouldn't have, something must depend on Property beyond a `PropertyName` reference,
  or it gains a separate lifecycle (billing, membership, access grants). Because Property
  already lives in its own `com.naturalist.zone.property` sub-context and `PropertyName` is
  already in `identifiers`, the promotion is a package→module lift: move the package, add
  the module stack, update imports — no identity rework, no reference-by-value untangling.
- **(g) `ZoneInfo` doc drift.** `zone/CLAUDE.md` calls `ZoneInfo` a `ValueObject`, but in
  code it is `NamedEntity<ZoneName>`. Reconcile the doc while adding the `property` field.
