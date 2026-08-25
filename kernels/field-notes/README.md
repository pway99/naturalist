# field-notes

Shared value objects for describing and naming any catalog entity, at any level of
understanding. It carries no domain knowledge — a compound, a climate threshold,
and an organism all describe themselves through the same two types.

---

## What it provides

**`Description` — the same truth at four levels.** A `ValueObject` holding one
description of an entity at each of four resolutions: `preschool`, `elementary`,
`secondary`, and `university`. The levels are additive, never contradictory —
each is wholly accurate, and a reader picks the depth they want. The shape follows
Gerald Durrell's principle from *The Amateur Naturalist* that ecological truth is
layered rather than a single statement. Every domain api depends on this type; a
describable entity without a `Description` is a deliberate omission worth
questioning.

**`CommonName` — a vernacular label with its locale.** A `ValueObject` pairing a
vernacular name ("pipevine swallowtail", "borraja") with the `Locale` in which it
is meaningful. Common names are deliberately textual and often regional, so an
entity carries a `Set<CommonName>` alongside its scientific identifiers; the
catalog search index harvests them as additional surface forms under which the
entity is findable. `CommonName` holds no back-pointer to the entity that owns it.

**`DescriptionRenderer`** (`render` subpackage) — a helper for presenting a
`Description`.

---

## Learn more

- [`kernels/CLAUDE.md`](../CLAUDE.md) — the shared-kernel conventions and the
  Durrell principle in context.
- [`docs/briefings/shared-kernels.md`](../../docs/briefings/shared-kernels.md) —
  a type-by-type tour of the shared value-object kernels.
