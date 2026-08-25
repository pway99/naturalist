# Chemistry

The compound catalog the rest of the system reads from — the molecules,
elements, and commercial products that soil amendments, plant phytochemistry,
and beekeeping treatments all reference by name.

It is a reference-data domain rather than an observation one: no photographs, no
field records, no naturalist. Its job is to be a stable, well-typed source of
chemical facts that other domains point at.

---

## Core concepts

**`Compound` is the aggregate root and the unit of cross-domain reference.** It
is a `NamedEntity` with a slug identity and a unique common name, and it owns its
classification facts (`CompoundInfo`) plus a set of profile value objects —
`SolubilityProfile`, `BioavailabilityProfile`, and the nullable
`VolatilizationProfile` and `SafetyProfile`, present only for fumigant and
hazardous compounds respectively. Other domains reference a compound only by its
`CompoundName` slug; the Java model is agnostic to any specific molecule, because
the catalog is data, not code.

**`Element` is an independent building block.** A `NamedEntity` carrying symbol,
atomic weight, and ionic form, referenced by compounds but standing on its own.
The `PeriodicElement` enum names all 118 elements by IUPAC symbol (`Ca`, `Mg`,
`K`) and backs a compound's constituent-element set.

**Classification is split across orthogonal axes, none of them nullable.**
`ChemicalNature` (organic / inorganic / organometallic) and `PhysicalForm` (salt,
acid, mineral, …) are independent enums — table salt is both inorganic and a
salt. `StructuralType` is a sealed interface of stateless permits over the
carbon-skeleton families (alkaloids, terpenoids by carbon count, phenolics,
glycosides, glucosinolates, …), with explicit `Element` and `Inorganic` permits
so the "this molecule has no carbon skeleton" case is a positive answer rather
than a missing value. Each permit rolls up to a `CompoundCategory`, and family
membership surfaces as behavioral predicates on the compound (`isAlkaloid()`,
`isTerpenoid()`, `isPhenolic()`) so consumers never pattern-match the sealed
hierarchy.

**What a compound does is a first-class question.** `FunctionalRole` is a second
sealed interface — `Chelator`, `Fumigant`, `BiologicalCatalyst`, `Fertilizer`,
`Acaricide` — carried as a non-empty set, with `isFumigant()`, `isHazardous()`,
and `isChelated()` predicates. Upstream domains ask what a compound does without
inspecting its structure.

**`Product` is a commercial SKU.** A named formulation carrying its ingredient
compounds and a map of SKU-scoped attributes (concentration, application window,
NPK ratio). It lives in its own sub-package so it can graduate to a standalone
domain without disturbing chemistry's consumers; the product defines its
ingredient list, and the reverse lookup is a query.

---

## Module layout

Follows the standard domain split described in the
[top-level README](../../README.md#architecture-at-a-glance). Domain-specific notes:

- **`chemistry-api`** is organized into sub-packages by subject — `compound/`
  (with `structure/` and `role/` for the two sealed hierarchies), `element/`,
  `product/`, and `reaction/`.
- **`chemistry-repository-rdms`** is the production-named persistence adapter; it
  currently delegates to the in-memory mock — a temporary state while the app is
  built out behind a stable port.

---

## Learn more

- [`CLAUDE.md`](CLAUDE.md) — the domain vocabulary, the two-axis classification
  model, and the compound-properties rules.
- [`docs/briefings/chemistry-domain.md`](../../docs/briefings/chemistry-domain.md) —
  a type-by-type tour of `chemistry-api`.
- [`docs/ubiquitous-language/CHEMICAL_SCIENCE_LANGUAGE.md`](docs/ubiquitous-language/CHEMICAL_SCIENCE_LANGUAGE.md) —
  the chemical-science vocabulary the model is built on.
- [ADR-013 — Value-object contract](../../docs/adr/ADR-013-value-object-contract.md) —
  why the profiles and classification facts are modeled as value objects.
