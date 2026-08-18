# KnownOrganism — Design

**Date:** 2026-08-18
**Status:** Design agreed (brainstorm). Not scheduled — captured so the collection-unit
question the [plants↔insects gap review](../../plans/2026-08-16-plants-insects-gap-review.md)
raised has a decided answer.

## Problem

A naturalist has an ongoing relationship with *specific individual organisms* they
recognise and follow over time — **my peach tree**, **the orb-weaver on the back fence**,
**the toad under the porch**. None of the existing concepts is this:

- `plants` / `insects` catalogs hold **taxa** (`PlantSpecies`, `InsectSpecies`), shared and
  objective — not per-naturalist individuals.
- `garden.Planting` is **a batch in a bed** (`plantCount`, zone, planted/removed dates) —
  a group with a lifecycle, not a named individual, and garden-only.
- `insects.FieldObservation` is **a one-time sighting** ("I saw subject X at time/place"),
  deduped by taxon — ephemeral, not a persistent individual.

The missing concept is the naturalist's **subjective layer** over the objective catalogs: a
registry of *known individuals*.

## Decision

A new top-of-DAG domain **`known-organisms`** with a single entity, **`KnownOrganism`**.

Rejected alternatives and why:

- **`garden.MyPlant`** — too narrow (wild specimens have no zone) and plant-only.
- **`naturalist/holdings`** — the concept references *upward* into `plants` (its taxon, the
  care guidance it follows) and `garden` (its location). Putting it in the `naturalist`
  base identity domain would make `naturalist` depend on `plants` + `garden`, dragging both
  onto the classpath of everything that only wanted a `NaturalistName` (e.g. `insects`). The
  DAG must refuse that inversion.
- **Typed `MyPlant` / `MyInsect` per domain** — a new type per organism domain, and the
  cross-domain-ness would be convention rather than mechanism. Rejected in favour of one
  general entity whose subject is a cross-domain reference.

"Holdings" was renamed **known organisms** deliberately: the relationship is *recognition*,
not *possession* — you do not "hold" a wild toad you watch, but it is a known organism. The
name also documents the domain's reach (any organism, cultivated or wild) without requiring
the rest of it to be built now.

## The entity

`KnownOrganism` — `Entity<KnownOrganismId>` (UUIDv7). Identity is a surrogate id plus a
human `nickname`, **not** a natural-key slug: two naturalists would both want
`my-peach-tree`, so a slug identity collides. The id is private; the nickname is the label.

Sketch (components subject to the plan):

| Component | Type | Notes |
|---|---|---|
| `id` | `KnownOrganismId` | UUIDv7 surrogate |
| `owner` | `NaturalistName` | whose known organism it is (required) |
| `nickname` | `String` | "Nick's peach", "the back-fence orb-weaver" (required, non-blank) |
| `subject` | `EntityRef` *(nullable)* | the taxon it is identified as — `EntityRef(DomainId, EntityName)`, the **catalog kernel's** cross-domain reference. Nullable to allow "a thing I'm following but haven't identified yet." |
| `location` | `KnownLocation` *(nullable)* | polymorphic: a garden site (`ZoneName` [+ `SubZoneName`]) *or* a wild place (free description [+ `Bioregion`]). Absent when unknown. |
| `notes` | `String` *(nullable)* | free narrative |

**The `subject` is the linchpin.** It is an `EntityRef(DomainId, EntityName)` — the same
reference `Catalog` search hits and cross-domain console links already use. "My peach tree"
is a `KnownOrganism` whose `subject` points at `plants` / `prunus-persica`; a future toad
points at `amphibians` / `…`. Adding the toad costs **zero** new known-organisms code — you
point a ref at it. To render a plant-specific view, resolve the ref through the `Catalog`.

`KnownOrganism` is an `Entity`, immutable, with explicit `with*` methods per mutable field
(nickname, subject as identification firms, location, notes).

## Management: taxon guidance vs. individual stewardship

The peach-tree-with-a-program case is what forced the placement, and it splits cleanly:

- **Taxon-level `PlantProgram`** (today's `plants/management`, keyed by `PlantRankName`) —
  *general* care for a taxon ("peaches want a dormant spray"). Shared reference. **Unchanged.**
- **The known organism's own stewardship** — *this* tree: when you actually pruned it,
  which programs it follows, its espalier schedule. Per-individual, lives with the
  `KnownOrganism`.

So a `KnownOrganism` **enrolls in / references** the taxon-level programs it follows
(`known-organisms → plants`, a legal downward reference) and carries its own care log. A
program specific to one individual is stewardship, not catalog data, so it never pollutes
`plants/management`. (Exact shape of "enrollment" + "care log" — a child collection vs. a
sibling entity — is an open question for the plan; it is **not** modelled by widening
`PlantProgram` to point back at a known organism, which would cycle `plants ↔ known-organisms`.)

## Architecture

New domain, standard module layout (`known-organisms-api`, `-core`, `-repository-test`),
following `domains/CLAUDE.md` scaffolding.

**DAG (new edges only):**

```
known-organisms  →  naturalist (NaturalistName), plants (EntityRef targets, PlantProgram),
                    garden (ZoneName/SubZoneName/PlantingId), catalog kernel (EntityRef/DomainId/Catalog)
```

Nothing depends on `known-organisms` except the composition root / console. It sits at the
**top** of the organism DAG, above every domain it references — which is exactly why it can
reference all of them and why it cannot live inside any one of them.

Console: a "My known organisms" surface owned by the current naturalist; each card resolves
its `subject` `EntityRef` through the `Catalog` to link into the relevant catalog page
(reusing the existing `EntityRefLinker` machinery).

## Scope

- **Now (when scheduled):** the entity + repository + query + a read-only console list/detail,
  populated only with plant subjects. The domain is general; the *data* is plant-first.
- **The "My…" convention** lives in the ubiquitous language / console copy ("my known
  organisms"), **not** as a type prefix. It extends to other organisms later for free via
  `EntityRef` — without touching `insects` or any other domain.

## Non-goals / boundaries

- **Not** an ephemeral sighting. `insects.FieldObservation` stays the "I saw one" concept;
  `KnownOrganism` is a persistent individual. The MVP vision-ID flow may *feed* either (an
  identified photo could register a new known organism, or just log a sighting) — that
  boundary is a separate decision, noted here so it is not conflated.
- **No changes to `insects`.**
- **No write side beyond `KnownOrganism`'s own `with*` methods** in the first cut.

## Open questions (for the implementation plan)

1. `KnownLocation` shape — sealed VO (`GardenSite | WildPlace`) vs. nullable fields.
2. Care log / program-enrollment shape — child collection on `KnownOrganism` vs. a sibling
   `StewardshipEvent` entity keyed by `KnownOrganismId`.
3. Whether `subject` is required or genuinely nullable (the unidentified-individual case).
4. How vision-ID relates: does identifying a photo create a `KnownOrganism`, a
   `FieldObservation`-analogue, or offer both?
5. Ownership/tenancy interplay with the deferred soil↔naturalist `Property` design (owner by
   `NaturalistName` directly vs. via a property/zone chain).
