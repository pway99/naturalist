# observation

The two record types organism domains share for evidence: a naturalist's sighting
of an organism, and a photograph attached to one. Both are generic over the
domain's own identifier and rank-name types, so insects, plants, and the other
organism domains reuse one shape rather than re-deriving it. The kernel carries no
domain knowledge — it fixes the structure, not the taxonomy.

---

## What it provides

**`OrganismObservation<ID, R>` — the collection unit.** An `Entity` record that a
naturalist (`observedBy`) encountered an organism at a taxonomic rank (`subject`)
at a point in time, with optional `notes` and `location`. `subject` is the
domain's own rank name (`InsectSpeciesName`, `PlantGenusName`, …) widened to
`RankName`, so an observation can land at whatever rank the evidence supports.
`identification` is an optional machine result — a manual sighting leaves it null.

**`OrganismImage<IMG_ID, OBS_ID, R>` — the photographic sibling.** An `Entity`
record for a photo that identifies a taxon at rank `parentName`, with a
`resourceName` and `dateAdded`. Its `observationId` links the photo to the sighting
it belongs to, and is null for a photo attached directly to a catalog rank with no
owning observation. Both records serialize their rank name as a self-describing
`{"rank":…,"value":…}` object and rebuild the concrete permit through the reading
mapper's registered reconstructor.

**`Identification` — the machine result.** A `ValueObject` present only on
vision-identified observations, grouping the three co-occurring facts of a vision
call: the model's `confidence`, the `evidence` (which visible features supported
it), and the runner-up `Candidate`s it weighed but did not select. Keeping it as
one nullable component makes "was this vision-identified?" a single null check and
keeps machine data out of the naturalist's own `notes`. A `Candidate`'s
`scientificName` is a plain `String`, not a typed name — an alternative may name a
species absent from the catalog — so it carries no cross-entity reference.

---

## Why it looks the way it does

An observation and a photo are the same concepts in every organism domain; only
the identifier and rank-name types differ. Making the records generic over those
type parameters lets each domain bind its own typed identity while the shared
structure — the fields, the invariants, the rank-name serialization — is defined
once here.

---

## Learn more

- [`docs/plans/2026-08-19-organism-observation-kernel-design.md`](../../docs/plans/2026-08-19-organism-observation-kernel-design.md)
  — the design behind `OrganismObservation`.
- [`docs/plans/2026-08-19-organism-image-kernel-design.md`](../../docs/plans/2026-08-19-organism-image-kernel-design.md)
  — the design behind `OrganismImage`.
- [`docs/plans/organism-domain-blueprint.md`](../../docs/plans/organism-domain-blueprint.md)
  — the shared organism-domain design these records serve.
