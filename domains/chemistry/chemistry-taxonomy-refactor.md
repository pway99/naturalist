# Refactor: Split `CompoundType` into Orthogonal Classifications

**Audience.** Claude Code, working in `com.naturalist`.
**Scope.** `chemistry-api` and its test fixtures only. No upstream domain currently
depends on `chemistry-api`, so this is a self-contained refactor.
**Non-goals.** No new query methods on `CompoundQuery`. No new behaviors on
`CompoundCollection` beyond what this refactor requires. No upstream domain wiring.
No kernel changes.

---

## 1. Why

`CompoundType` (`INORGANIC_SALT`, `ORGANIC_ACID`, `MINERAL`, `ELEMENT`, `CHELATE`,
`BIOLOGICAL_COMPOUND`, `VOLATILE_ORGANIC`) conflates three orthogonal axes:

1. **Chemical nature** — organic vs. inorganic vs. organometallic.
2. **Physical form** — element, mineral, salt, acid, base, complex.
3. **Functional role** — what the compound *does* (chelator, fumigant, biological
   catalyst, fertilizer, acaricide).

A flat enum forces every consumer into awkward predicate combinations and
string-matching against `properties`. Insects, plants, and soil are about to attach
to chemistry as upstream cores; resolving this before they attach is much cheaper
than migrating call sites later.

**Behavioral predicates that currently rely on nullability** (e.g. "is a fumigant" ≡
`volatilization != null`) are also being promoted to first-class methods on
`Compound`, so upstream domains never see the structural form.

---

## 2. End State Summary

After this refactor:

- `CompoundType` is **deleted**.
- `CompoundInfo` gains three new components: `ChemicalNature chemicalNature`,
  `PhysicalForm physicalForm`, `Set<FunctionalRole> functionalRoles`.
- `FunctionalRole` is a **sealed interface**, not an enum.
- `Compound` gains four behavioral predicates: `isFumigant()`, `isHazardous()`,
  `isChelated()`, `playsRole(FunctionalRole)`.
- `CompoundCollection` gains two filtering methods: `withChemicalNature(...)`,
  `withFunctionalRole(...)`.
- `compounds.json` is migrated: `"type"` is removed; `"chemicalNature"`,
  `"physicalForm"`, `"functionalRoles"` are added under `compoundInfo`.
- Test fixtures, contract tests, and unit tests updated.

---

## 3. New Types — Specification

### 3.1 `ChemicalNature` (enum)

**Package.** `com.naturalist.chemistry.compound` (lives in `chemistry-api`).

```java
public enum ChemicalNature {
    ORGANIC,
    INORGANIC,
    ORGANOMETALLIC
}
```

**Classification rule (document in javadoc):** carbonates, oxides, cyanides, and
pure carbon allotropes are conventionally `INORGANIC` despite containing carbon.
Organic compounds are characterized by C–H bonds and their derivatives.
Organometallic compounds contain at least one carbon-to-metal bond.

### 3.2 `PhysicalForm` (enum)

**Package.** `com.naturalist.chemistry.compound`.

```java
public enum PhysicalForm {
    ELEMENT,
    MINERAL,
    SALT,
    ACID,
    BASE,
    COMPLEX
}
```

`PhysicalForm.SALT` and `ChemicalNature.INORGANIC` are independent axes — NaCl is
both, and that's the point. Do not collapse them.

### 3.3 `FunctionalRole` (sealed interface)

**Package.** `com.naturalist.chemistry.compound.role`.

A sealed interface so individual roles can carry attached state if/when they need
it. Initial permits are stateless record types; treat them as the sealed-interface
equivalent of enum constants until a concrete role acquires data.

```java
package com.naturalist.chemistry.compound.role;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
@JsonSubTypes({
    @JsonSubTypes.Type(value = FunctionalRole.Chelator.class,           name = "CHELATOR"),
    @JsonSubTypes.Type(value = FunctionalRole.Fumigant.class,           name = "FUMIGANT"),
    @JsonSubTypes.Type(value = FunctionalRole.BiologicalCatalyst.class, name = "BIOLOGICAL_CATALYST"),
    @JsonSubTypes.Type(value = FunctionalRole.Fertilizer.class,         name = "FERTILIZER"),
    @JsonSubTypes.Type(value = FunctionalRole.Acaricide.class,          name = "ACARICIDE")
})
public sealed interface FunctionalRole {
    record Chelator()           implements FunctionalRole {}
    record Fumigant()           implements FunctionalRole {}
    record BiologicalCatalyst() implements FunctionalRole {}
    record Fertilizer()         implements FunctionalRole {}
    record Acaricide()          implements FunctionalRole {}
}
```

**Notes.**

- The discriminator property is `"kind"` and the values are SCREAMING_SNAKE_CASE
  strings matching the conceptual role name. This matches the briefing's enum
  serialization convention (§3 of `chat-briefing.md`) while still being a sealed
  hierarchy.
- These records are `ValueObject`-shaped but **do not implement `ValueObject`** —
  they have no `invariants()` and no observability surface; they are pure
  classification tokens, equivalent in role to enum constants. If a role ever
  acquires state and behavior, that's the moment to promote it to `ValueObject`
  and add to the `Compound` graph walk.
- **Do not add `@JsonCreator`** to the records (briefing §3 Jackson rules: records
  deserialize natively).
- Per the briefing's package convention, this lives at
  `com.naturalist.chemistry.compound.role.*` — a sub-package of the compound
  context. **Do not** place under `com.naturalist.chemistry.role` (would imply
  domain-wide scope).

---

## 4. `CompoundInfo` Changes

### 4.1 Component changes

- **Remove:** `CompoundType type`.
- **Add (in this order, as the last three components before any existing
  `constituentElements`):**
    - `ChemicalNature chemicalNature`
    - `PhysicalForm physicalForm`
    - `Set<FunctionalRole> functionalRoles`

The component order matters because the canonical record header determines JSON
field order on serialization. Place them after the existing `phCharacter` /
`molecularWeight` cluster and before `constituentElements`. Confirm by reading
the existing `CompoundInfo` record header before editing.

### 4.2 `invariants()` updates

Add to the existing body (do not rewrite the existing checks):

```java
i.notNull(this, CompoundInfo::chemicalNature, "chemicalNature")
 .notNull(this, CompoundInfo::physicalForm,   "physicalForm")
 .notEmpty(this, CompoundInfo::functionalRoles, "functionalRoles");
```

**Rationale per `constraints-ubl.md` §3:**

- `notNull` for the two enum fields — enums are scalars, not `Observable`, so
  presence check only.
- `notEmpty` (by-fn form, the only form that exists per §3) for `functionalRoles`
  — every compound plays at least one role. There is no `notEmpty(value, name)`
  direct form; do not invent one.
- **Do not** wrap the `Set<FunctionalRole>` in `valueObjectCollection`. The role
  records are not `ValueObject` — they have no `invariants()` to descend into.
  Per `constraints-ubl.md` §11, mixing the two would be a category error.

Remove any prior `notNull(this, CompoundInfo::type, "type")` line as part of the
deletion.

---

## 5. `Compound` Behavioral Predicates

Add to `Compound` (the aggregate root). These are **behavior methods**, not
component accessors — placement after the component accessors and any existing
behaviors, before `invariants()`:

```java
public boolean isFumigant()  { return volatilization != null; }
public boolean isHazardous() { return safety != null; }
public boolean isChelated()  { return bioavailability.isChelateEnhanced(); }

public boolean playsRole(FunctionalRole role) {
    return compoundInfo.functionalRoles().contains(role);
}
```

**Naming check (briefing §3, accessor rules):** `is*` prefix is allowed only for
behavior methods that are not component accessors. `Compound` has no `fumigant`,
`hazardous`, or `chelated` component, so these are safe. Do not rename
`isChelateEnhanced()` on `BioavailabilityProfile` — that name is established and
the briefing flags JSON field name preservation as a hard rule.

**Do not add Optional-returning variants.** The predicates are queries on
existing state; `volatilizationOptional()` and `safetyOptional()` already exist
for callers that need the profile itself.

---

## 6. `CompoundCollection` Filtering Methods

Per the briefing §7, `BehavioralCollection` subclasses use a package-private
constructor and return new instances from filtering methods. Add to
`CompoundCollection` (in `chemistry-api`):

```java
public CompoundCollection withChemicalNature(ChemicalNature nature) {
    return new CompoundCollection(
        elements().stream()
            .filter(c -> c.compoundInfo().chemicalNature() == nature)
            .toList()
    );
}

public CompoundCollection withFunctionalRole(FunctionalRole role) {
    return new CompoundCollection(
        elements().stream()
            .filter(c -> c.playsRole(role))
            .toList()
    );
}
```

**Confirm before writing:** read the current `CompoundCollection` source to find
the actual base-class accessor name (the briefing implies the elements are held
by `BehavioralCollection<T>`'s internal list, but the accessor name — `elements()`
vs. `items()` vs. something else — is not in the briefing). Match what's there.
If the accessor isn't visible from the subclass, flag it in chat rather than
inventing one.

**Do not** add `withPhysicalForm(...)` yet. It's a candidate but no consumer is
asking for it; the briefing's anti-patterns include not pre-emptively building
query surfaces (ADR-019: PR size discipline).

---

## 7. JSON Migration — `compounds.json`

**File location:** `chemistry-repository-test/src/main/resources/chemistry/compound/compounds.json`
(per `CLAUDE.md` data extraction pattern).

For every entry, under `compoundInfo`:

- **Remove** the `"type"` field.
- **Add** three fields:
    - `"chemicalNature": "ORGANIC" | "INORGANIC" | "ORGANOMETALLIC"`
    - `"physicalForm": "ELEMENT" | "MINERAL" | "SALT" | "ACID" | "BASE" | "COMPLEX"`
    - `"functionalRoles": [ { "kind": "CHELATOR" }, { "kind": "FUMIGANT" }, ... ]`

### 7.1 Mapping table for migration

The existing `CompoundType` values map as follows. **Confirm each mapping against
the actual compound** before applying — these are reasonable defaults, not
authoritative for every entry. If a compound's existing record contradicts the
default, ask in chat.

| Old `type`            | `chemicalNature` | `physicalForm` | `functionalRoles` (defaults; verify per compound) |
|-----------------------|------------------|----------------|---------------------------------------------------|
| `INORGANIC_SALT`      | `INORGANIC`      | `SALT`         | `[FERTILIZER]` (most are; verify)                 |
| `ORGANIC_ACID`        | `ORGANIC`        | `ACID`         | `[CHELATOR]` if chelating, otherwise `[]` — flag  |
| `MINERAL`             | `INORGANIC`      | `MINERAL`      | `[FERTILIZER]` if soil amendment, else verify     |
| `ELEMENT`             | `INORGANIC`      | `ELEMENT`      | verify per compound                               |
| `CHELATE`             | depends — verify | `COMPLEX`      | `[CHELATOR]` + whatever else applies              |
| `BIOLOGICAL_COMPOUND` | `ORGANIC`        | verify         | `[BIOLOGICAL_CATALYST]`                           |
| `VOLATILE_ORGANIC`    | `ORGANIC`        | verify         | `[FUMIGANT]` (formic acid, thymol)                |

### 7.2 Cross-check against existing data

- Any compound with `volatilization != null` **must** have `Fumigant` in its
  `functionalRoles`. If migration produces a compound that's volatilization-bearing
  but not flagged as `Fumigant`, it's a data error — flag in chat.
- Any compound with `bioavailability.isChelateEnhanced == true` **must** have
  `Chelator` in its `functionalRoles`. Same rule.
- Any compound whose `properties` map contains `"biologicalCatalyst"` should have
  `BiologicalCatalyst` in `functionalRoles`. The string property becomes redundant
  after this refactor — **leave the string property in place for now** (removal is
  Phase 3 and out of scope; flag for follow-up).

### 7.3 Validation gate

After migrating the JSON, run `CompoundTestEntitySource` deserialization and the
existing `CompoundTestEntitySourceTest`. Both should pass. If deserialization fails
on `FunctionalRole`, the most likely cause is the `@JsonTypeInfo` discriminator
property name not matching the JSON. Re-check §3.3.

---

## 8. Test Updates

### 8.1 `CompoundTest` (unit test)

Per `constraints-ubl.md` §9, the invalid-case assertion uses
`containsExactlyInAnyOrder` with exact domain-relative paths. The current
expected set under `compoundInfo` likely includes `"type"` — replace with
`"chemicalNature"`, `"physicalForm"`, `"functionalRoles"`. Read the current test
before editing; preserve any other paths verbatim.

The valid-case test must populate the three new fields. If the test uses a
test-data builder, extend the builder; if it constructs `CompoundInfo` inline, add
the new arguments.

### 8.2 `CompoundInfoTest` (if it exists)

Same migration as 8.1, scoped to the `CompoundInfo` invariants.

### 8.3 Repository contract test

`CompoundRepositoryContractTest` (or whatever the actual contract test is called
under `chemistry-repository-test`) walks the full constraint graph via the Observer
framework on insert/update (briefing §6, ADR-002). Re-running it after the JSON
migration is the integration check that the new fields are wired through the
adapter correctly. No changes to the contract test itself should be needed unless
it asserts specific paths — read first.

### 8.4 New tests (do not skip)

Add unit tests for the four new behavioral predicates on `Compound`:

- `isFumigant()` — true when `volatilization` present, false otherwise.
- `isHazardous()` — true when `safety` present, false otherwise.
- `isChelated()` — true when `bioavailability.isChelateEnhanced()`, false otherwise.
- `playsRole(FunctionalRole)` — true when role is in the set, false otherwise; test
  with at least one present role and one absent role.

Add unit tests for the two new `CompoundCollection` methods:

- `withChemicalNature(ORGANIC)` returns only organic compounds.
- `withFunctionalRole(new FunctionalRole.Fumigant())` returns only fumigant
  compounds. Note role records are value-equal — `new Fumigant()` equals any
  other `new Fumigant()`.

---

## 9. Things to Confirm Before Writing Code

These are gaps the chat briefings don't cover. Read the actual source or ask in
chat — do not invent.

1. **Current `CompoundInfo` component order and exact accessor names.** The record
   header determines what's already there.
2. **`CompoundCollection` base-class accessor for the underlying list.** Briefing
   §7 doesn't name it. Read `kernels/framework/src/main/java/.../BehavioralCollection.java`.
3. **Whether `CompoundCollection` already has any methods** (the user confirmed
   "no behaviors at this time" — verify by reading the file).
4. **Existence and exact path of `CompoundInfoTest` and `CompoundTest`.**
5. **Whether any non-test code in `chemistry-core` or `chemistry-repository-rdms`
   references `CompoundType`.** Likely none, but `grep -r CompoundType domains/chemistry/`
   before deleting the enum.
6. **Whether `CompoundType` is referenced in `chemistry-console` (JTE templates,
   Spring MVC).** Templates render the type label in compound detail pages; if
   they reference `compoundInfo.type`, those templates need to be updated to
   render `chemicalNature` / `physicalForm` / `functionalRoles` instead.

---

## 10. PR Discipline (ADR-019)

This is a single coherent refactor and should land as **one PR**. The change
touches one domain, no public api consumers, and the migration is mechanical
once the mapping table is settled. Splitting it would create an interim state
where `CompoundInfo` carries both `type` and the new fields, which is worse than
landing the cutover atomically.

If the PR exceeds the size guideline materially, the splittable seam is:

- **PR 1:** Add new types (`ChemicalNature`, `PhysicalForm`, `FunctionalRole`)
  and new components on `CompoundInfo`. Keep `type` in place. Migrate JSON to
  populate both old and new fields. Tests updated to assert both.
- **PR 2:** Remove `type` field and `CompoundType` enum. Remove the old field
  from JSON. Remove old test assertions.

Default to a single PR; only split if size review pushes back.

---

## 11. Out of Scope (Explicit)

Do **not** do any of the following in this refactor:

- Add new query methods to `CompoundQuery`. No upstream domain is asking yet.
- Promote any `properties` map entries to typed fields on `Compound`. Phase 3
  decision, deferred.
- Remove the `"biologicalCatalyst"` string property from `properties` (even
  though it's now redundant with `FunctionalRole.BiologicalCatalyst`). Removal
  needs a separate deprecation cycle once consumers exist.
- Add Insects/Plants/Soil dependencies on `chemistry-api`. Each upstream domain
  attaches in its own PR when its own work calls for it.
- Modify the kernel framework. Out of scope per briefing §13.4 — flag any
  apparent need in chat.
- Add caching to `CompoundCollection` filtering methods. Briefing anti-pattern.
- Add `@JsonCreator` to any record. Briefing §3 hard rule.

---

## 12. Done Criteria

- [x] `CompoundType` enum file deleted.
- [x] `ChemicalNature`, `PhysicalForm` enums created in
  `com.naturalist.chemistry.compound`.
- [x] `FunctionalRole` sealed interface + permits created in
  `com.naturalist.chemistry.compound.role`.
- [x] `CompoundInfo` updated: `type` removed, three new components added,
  `invariants()` updated.
- [x] `Compound` updated: four behavioral predicates added
  (`isFumigant`, `isHazardous`, `isChelated`, `playsRole`).
- [x] `CompoundCollection` updated: two filtering methods added
  (`withChemicalNature`, `withFunctionalRole`).
- [x] `compounds.json` migrated; deserialization passes.
- [x] `CompoundTest`, `CompoundInfoTest`, and other affected tests updated.
- [x] Repository contract test passes unchanged.
- [x] New unit tests for predicates and collection filters added.
- [x] `grep -r CompoundType domains/chemistry/` returns zero results in
  production code (only this doc and the historical
  `docs/chemistry-api-briefing.md` retain the name as historical context).
- [x] Console templates updated — `chemistry/detail.jte` renders
  `chemicalNature`, `physicalForm`, and `functionalRoles` instead of
  the deleted `type`.
- [x] No new dependencies introduced; DAG unchanged.

**Status:** Refactor complete as of 2026-04-26.

---

## 13. Follow-on Work Landed After This Refactor

The taxonomy refactor unblocked a sequence of adjacent chemistry work. None of
the following was in the original scope — record here so the next contributor
can see the current shape of the domain at a glance.

### 13.1 Product entity (new sub-context)

- New `NamedEntity<ProductName>` at `com.naturalist.chemistry.product` with
  `displayName`, `Set<CompoundName> compounds`, and `Map<String, String> properties`.
- Full stack: `ProductRepository` (package-private), `ProductQuery`
  (`findByNameSet`, `findByCompoundName`, `allProductNames`), `ProductCollection`,
  `ProductEntityRepositoryMock`, `ProductEntityRepositoryTest` contract,
  `ProductTestEntitySource`, and `products-base.json` (12 products).
- Wired into `ChemistryTestContext` alongside `compoundQuery()`.
- Product owns the compound→product relationship; reverse lookup is a query.
- N=1 collapse applied — single entity in the package, no namespace types.

### 13.2 Property migration: compound → product

- All amendment-application properties (concentration, application window,
  applied rates, optimum range, NPK ratios, efficacy claims, BER protocol roles,
  phEffect, insituReaction, biologicalCatalyst chemistry-context entries)
  migrated out of `compounds-base.json` onto the relevant product.
- Created 7 new generic-amendment products (gypsum, epsom-salt, lime,
  formic-acid-mite-treatment, oxalic-acid-mite-treatment, neem-oil,
  kirkland-ultra-shine) for compounds without an explicit branded SKU but
  carrying product-shaped properties.
- `calcium-pectate` retains its biology-context properties (plant-tissue
  biology, not an amendment) — the only compound with a non-empty `properties`
  map after migration.

### 13.3 Compound depictions

- New `CompoundDepiction` aggregate with its own repository, query, and
  test-entity source. SVG rendering via `DepictionRenderer` in the console.
- Depictions are persisted (not generated on the fly).

### 13.4 Chemistry console

- `/chemistry` — compound list (with depiction thumbnails on cards that have
  one).
- `/chemistry/{name}` — compound detail with depiction, profiles, and
  "Products containing this compound" section.
- `/chemistry/{name}/depiction.svg` — SVG endpoint.
- `/chemistry/products` — product catalog, compound names on cards link
  through to compound detail.
- `/chemistry/products/{name}` — product detail with compound list and
  properties.
- Console CSS: card-body links rendered in `--pico-primary` (green) with a
  dotted underline so they read as links against sepia card text.

### 13.5 Open follow-ups (not blocking)

- Promote ad-hoc product property keys (concentration, application window,
  NPK) to typed fields on `Product` if/when a consumer needs them — currently
  `Map<String, String>` is the right shape for an open-ended SKU attribute set.
- Remove the `"biologicalCatalyst"` string property from any remaining compound
  `properties` maps once a deprecation window passes (see §11 — still out of
  scope).
- `ChemistryController` instantiates `ChemistryTestContext` directly with a
  `// TODO` — promote to a Spring-managed bean when the production wiring
  story is decided.
- Add `withPhysicalForm(...)` to `CompoundCollection` if/when a consumer asks
  (deferred per §6).
