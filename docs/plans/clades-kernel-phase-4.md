# Clades Kernel — Phase 4 Slice Plan

> Promoted from [`clades-kernel.md`](clades-kernel.md) Phase 4 on 2026-05-13.

**Goal.** Land the first real clade placement on a catalogued insect,
so the end-to-end resolution path (species → `placedIn` → traversal →
`MetabolyTrait` → stage set) runs against real catalog data rather
than synthetic test fixtures.

The validation case is *Battus philenor* — its `placedIn` points to
the `Papilionidae` clade, which inherits `MetabolyTrait` via
`Lepidoptera` → `Holometabola`. The clade tree built in Phase 1, the
trait declaration from Phase 2, and the `placedIn` field added in
Phase 3 all converge here.

This is the **first-placement phase**. Behavioural shift is still
zero — no consumer code yet asks "what stages does this species have"
through the resolver; that's Phase 5. Phase 4 just proves the chain
links.

---

## Scope decisions

### Records that gain `placedIn`

Two records, both pointing at the `papilionidae` clade:

1. **`battus-philenor`** (existing `InsectSpecies` entry, the
   *Battus philenor* / Pipevine Swallowtail).
2. **`papilionidae`** (new `InsectFamily` entry — currently no
   Papilionidae family record exists in the catalog).

The family-rank record and the family-clade share the scientific name
by convention but remain two distinct records — the rank lives in
`InsectFamily` / `LinnaeanFamily` (Rank DAG); the clade lives in
`Clade` permits (Clade DAG). The `placedIn` field is the bridge.

No other species or family gains `placedIn` in this phase. Extending
to the remaining ~16 catalogued insects is incremental and not gated
on Phase 4 (parent plan's "extend as needed for Phase 5").

### Why a new Papilionidae family entry

The parent plan calls for it explicitly: *"Place Papilionidae
(LinnaeanFamily) alongside Papilionidae (Clade)."* Without the family
record, the catalog has the species-level placement but not the
rank-level placement — and Phase 5 will want to resolve from any
rank (species, genus, family), not just species. Adding the family
record now is the smallest unit of work that closes both halves of
Phase 4's "Delivers" list.

The InsectFamily entry carries the standard four-level Durrell
`Description`, the order/family epithets, locale-tagged common names,
and the new `placedIn` field. No life-stage fields are populated at
the family level (deferred until needed).

### What's not in scope

- No bulk reorganisation across the rest of the catalog. Each future
  placement is a one-line JSON edit; we add them lazily as Phase 5
  uncovers the need.
- No `InsectGenus` entry for `battus`. Genus-rank placement is a
  separate opt-in; the species can resolve its placement directly
  without needing the genus record.
- No traversal-routing changes — Phase 5 owns that.
- No new clade permits. `papilionidae` already exists in the Phase 1
  clade tree.

---

## Changes

```
domains/insects/insects-repository-test/src/main/resources/insects/
└── insect-families.json                — new "papilionidae" entry with placedIn
└── insect-species.json                 — battus-philenor gains "placedIn": "papilionidae"

domains/identifiers-test/src/main/java/com/naturalist/insects/
└── TestInsectsIdentifiers.java         — add InsectFamily.Papilionidae

domains/insects/insects-repository-test/src/test/java/com/naturalist/insects/  (or insects-core test)
└── (new test)                          — end-to-end resolution from loaded species
```

The end-to-end test lives wherever the existing repository-loading
plumbing is reachable — the test loads `battus-philenor` through the
species repository (real `TestEntitySource` JSON load), calls
`placedInOptional().get()`, then `CladeTraversal.findTrait(...,
MetabolyTrait.class, InsectClades::traitsFor)`, and asserts the
expected `Holometabolous` metaboly.

---

## Test plan

Single new test in the repository-test or core test surface:

- `battusPhilenorResolvesItsCladePlacementThroughLoadedJson` —
  loads the species via the repository, walks the clade chain,
  confirms `MetabolyTrait(Holometabolous)`.

This is functionally similar to
`InsectFamilyTest.papilionidaeFamilyResolvesItsCladePlacement` from
Phase 3 but exercises the **JSON load path** rather than an inline
record. That distinction matters: Phase 4's promise is that the
resolution works against real catalog data, not just fabricated
fixtures.

No new tests on the new `papilionidae` family record specifically —
its placement is exercised by the same end-to-end test (the species
walks the chain that begins at the clade `placedIn` points to;
the family's own placement is identical, and the chain converges
on the same trait).

---

## Steps

- [ ] Add `papilionidae` entry to `insect-families.json` with the four
  standard fields (name/order/family/description/commonNames) plus
  `"placedIn": "papilionidae"`.
- [ ] Add `TestInsectsIdentifiers.InsectFamily.Papilionidae` constant.
- [ ] Add `"placedIn": "papilionidae"` to the battus-philenor entry
  in `insect-species.json`.
- [ ] Add the end-to-end JSON-load resolution test.
- [ ] User runs `mvn verify`. Expect green.
- [ ] Commit.
- [ ] Roll work-tracker forward — Phase 4 ✅, Phase 5 next.

---

## Out of scope

- **Other family/species placements.** Hesperiidae (Lepidoptera but
  not Papilionidae) could be placed in the `lepidoptera` clade
  trivially; deferred until Phase 5 needs it.
- **Other Holometabola families** (Halictidae, Chrysopidae, Tachinidae,
  etc.). The current Clade tree has no permits for those orders'
  containing branches below Holometabola, so they'd have to skip
  intermediate clades — a design decision worth deferring.
- **Resolver wiring.** Phase 5.
- **Removing the inline life-stage fields on species records.**
  Phase 5.

---

## Risks

- **End-to-end test reaches across modules.** The test needs the
  species repository (in `insects-repository-test`) plus the clade
  traversal (in `kernels/clades`) and the insects trait declarations
  (in `insects-api`). Standard test wiring should accommodate this;
  if not, the fallback is loading the JSON directly through
  `TestDataHelper` rather than going through the full repository.
- **Family record growth.** Adding a new InsectFamily entry triggers
  the repository contract tests that exercise family-rank queries.
  The new entry must have a valid four-level description and pass
  the entity's invariants. Worst case is a typo in a required field;
  caught by `mvn verify`.
