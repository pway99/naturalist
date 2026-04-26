# Product

Commercial SKU sub-context within the chemistry domain.

A `Product` is a brand-or-labeled formulation that contains one or more
`Compound` references — the consumer-facing artefact a beekeeper or grower
actually buys (e.g. *Apiguard*, *TPS Nutrients CalMag OAC*, *Bonide Rot-Stop
RTU*). The product authoritatively defines its ingredient list; the reverse
lookup ("which products contain compound X?") is a query.

The package lives at `com.naturalist.chemistry.product` rather than
`com.naturalist.chemistry.compound.product` so it can graduate to a
standalone `product` domain without disturbing chemistry consumers when
non-chemistry product attributes (supplier, COGS, regulatory class) start
to accumulate.

---

## Identity

- `Product` — `NamedEntity<ProductName>`. Identity is `ProductName` (slug,
  ADR-022). No surrogate key, no `id` component.
- `ProductName` lives in `domains/identifiers/` per the EntityName placement
  rule.

---

## Components

| Component | Type | Notes |
|---|---|---|
| `name` | `ProductName` | kebab-case slug, the canonical identity |
| `displayName` | `@UniqueValue String` | human-readable label, globally unique |
| `compounds` | `Set<CompoundName>` | non-empty; the formulation. Cross-`NamedEntity` reference by slug |
| `properties` | `Map<String, String>` | open-ended SKU-scoped attributes (concentration, application window, NPK, etc.) |

`property(String)` returns `Optional<String>` for ergonomic single-key reads.

The `properties` map is the open-ended attribute container per the chemistry
domain's documented pattern — recurring keys may be promoted to typed fields
when a consumer needs typed behaviour. Until then, the map is the right
shape for an SKU-scoped attribute set with no independent lifecycle.

---

## Api Surface

This package collapses under the **N=1 rule** (`domains/CLAUDE.md` —
namespace patterns): exactly one entity in the package, so there is no
`ProductRepository` / `ProductQuery` namespace type wrapping nested
`Repository` / `Query` interfaces. Top-level `ProductRepository` (package-
private interface) and top-level `ProductQuery` (public interface) are the
correct shape.

| Type | Visibility | Purpose |
|---|---|---|
| `Product` | public record | Entity |
| `ProductRepository` | package-private interface | Persistence port |
| `ProductQuery` | public interface | Read-side api |
| `ProductCollection` | public final class | `BehavioralCollection<Product>` return type |

`ProductQuery` exposes:

- `getByName(ProductName)` → `Optional<Product>` (inherited from `EntityQuery`)
- `findByNameSet(Set<ProductName>)` → `ProductCollection`
- `findByCompoundName(CompoundName)` → `ProductCollection`
- `allProductNames()` → `EntityNameSet<ProductName>`

`findByCompoundName` is the reverse lookup that lets `chemistry/{name}`
console pages render *"products containing this compound"*.

---

## Data

Catalog file:
`chemistry-repository-test/src/main/resources/chemistry/product/products-base.json`

Schema and authoring rules are documented in
`docs/chat-product-acquisition.md` at the repo root. New product entries
should be produced via that briefing so the file remains diff-friendly and
field order stays canonical.

---

## Cross-Domain Rules

- `Product` references `Compound` by `CompoundName` only — never by value.
- `Compound` does **not** reference `Product`. The relationship is owned by
  the product side.
- When/if `product` graduates to its own top-level domain, `chemistry-api`
  becomes a dependency of `product-api` (one-way), and the chemistry console
  product pages move with it.
