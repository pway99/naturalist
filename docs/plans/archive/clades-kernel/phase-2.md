# Clades Kernel — Phase 2 Slice Plan

> Promoted from [`clades-kernel.md`](../../clades-kernel.md) Phase 2 on 2026-05-12.

**Goal.** Define `Metaboly` (the three developmental patterns insects can
follow), wrap it in a `MetabolyTrait` implementing the kernel's `Trait`
marker, and write `InsectClades.traitsFor(Clade)` — a pure pattern-matching
switch — that declares `MetabolyTrait(Holometabolous)` on the
`Holometabola` clade. End-to-end test: traversal from `Papilionidae`
returns holometabolous; traversal from `Insecta` (one node *above* the
declaration) returns empty.

This phase proves the **trait-declaration / traversal contract** end-to-end
before any consumer (Phase 5) depends on it.

---

## Open question (settled) — module placement for `Metaboly`

The sketch left three options on the table: a new `kernels/developmental-biology/`
kernel, folding into `kernels/field-notes/`, or keeping `Metaboly` in
`domains/insects/insects-api/`.

**Decision: `domains/insects/insects-api/com/naturalist/insects/lifestage/`.**

Reasoning:

- The categories `Ametabolous` / `Hemimetabolous` / `Holometabolous` are
  Hexapoda-specific developmental vocabulary. Arachnids use
  *anamorphic*/*epimorphic* growth, crustaceans use *nauplius*/*zoea*/
  *megalopa* sequences, vertebrates have entirely different concepts.
  There is no cross-kingdom vocabulary to abstract — yet.
- `kernels/CLAUDE.md` hard rule: "Kingdom-specific concerns belong in the
  domain module, not here." Metaboly is exactly that.
- Folding into `field-notes` is wrong — that kernel is the Durrell
  description vocabulary, not a general life-history concept bucket.
- The `lifestage` subpackage already exists in `insects-api` and houses
  `EggStage` / `LarvaStage` / `PupaStage` / `AdultStage` / `LifeStage`
  records. `Metaboly` fits there as the type that *enumerates* which
  of those stages a given developmental pattern uses.
- If arachnids or myriapods later need shared developmental concepts,
  factor out then. Today: YAGNI.

`MetabolyTrait`, `InsectClades`, and the test go to insects-api as well —
no module boundary crossed within this phase.

---

## Open question (settled) — sealed interface vs. enum for `Metaboly`

**Decision: sealed interface with stateless record permits.** Same shape
as `Clade` in the kernel. Reasons:

- Pattern matching at call sites:
  ```java
  switch (metaboly) {
      case Holometabolous _ -> ...;
      case Hemimetabolous _ -> ...;
      case Ametabolous   _ -> ...;
  }
  ```
- Extensibility: if a future permit ever needs state (e.g.
  `Hemimetabolous(int instarCount)`), the shape supports it without
  refactor. An enum would force a rewrite.
- Consistency with `Clade` — same author can read both without context
  switching between "sealed permits" and "enum with constructor args."

The enum form (`Metaboly.HOLOMETABOLOUS` with a `stages()` accessor)
would be more concise but locks the permits to stateless. Not worth
the saving.

---

## Architecture

Everything lives in `domains/insects/insects-api/`. No new modules, no
test fixture changes, no JSON edits.

```
domains/insects/insects-api/src/main/java/com/naturalist/insects/
├── InsectClades.java                       — static utility; traitsFor(Clade) switch (NEW)
└── lifestage/
    ├── Metaboly.java                       — sealed interface + List<LifeStageKind> stages() (NEW)
    ├── Ametabolous.java                    — permit: [EGG, JUVENILE, ADULT] (NEW)
    ├── Hemimetabolous.java                 — permit: [EGG, NYMPH, ADULT] (NEW)
    ├── Holometabolous.java                 — permit: [EGG, LARVA, PUPA, ADULT] (NEW)
    └── MetabolyTrait.java                  — record(Metaboly) implements Trait (NEW)
```

Plus one additive enum change in `domains/identifiers/`:

```
domains/identifiers/src/main/java/com/naturalist/insects/
└── LifeStageKind.java                      — add NYMPH ("nymph"), JUVENILE ("juvenile")
```

No existing call site references either new enum value. The change is
purely additive; `LifeStageName` and `fromSlug` continue to work for
EGG / LARVA / PUPA / ADULT and now accept "nymph" / "juvenile" too.

---

## Dependency-graph changes

`insects-api` already depends on `framework`, `identifiers`, `field-notes`,
`taxonomy`. Phase 2 adds **one new dependency**: `insects-api → clades`.
Required because `MetabolyTrait implements Trait` (kernel-provided) and
`InsectClades.traitsFor` switches over `Clade` permits (kernel-provided).

Editing `domains/insects/insects-api/pom.xml` and a one-liner in the
root `pom.xml`'s `<dependencyManagement>` is already covered — `clades`
is registered there as of `b8f0025`.

---

## Code shapes (reference)

### `LifeStageKind.java` — enum extension

```java
public enum LifeStageKind {
    EGG("egg"),
    LARVA("larva"),
    NYMPH("nymph"),
    PUPA("pupa"),
    JUVENILE("juvenile"),
    ADULT("adult");
    // existing slug accessor + fromSlug unchanged
}
```

### `Metaboly.java`

```java
package com.naturalist.insects.lifestage;

import com.naturalist.insects.LifeStageKind;

import java.util.List;

/**
 * The developmental pattern a hexapod lineage follows: which life stages
 * exist, in what order. Declared once on the originating clade
 * (e.g. {@code Holometabola}) via {@link MetabolyTrait}; descendants
 * resolve it through {@code CladeTraversal#findTrait}.
 *
 * <p>Categories are Hexapoda-specific. Other kingdoms have analogous
 * but distinct developmental concepts and would model them separately.
 */
public sealed interface Metaboly
        permits Ametabolous, Hemimetabolous, Holometabolous {

    /** The life-stage sequence for this developmental pattern, in order. */
    List<LifeStageKind> stages();
}
```

### Permits

```java
// Ametabolous.java
public record Ametabolous() implements Metaboly {
    @Override public List<LifeStageKind> stages() {
        return List.of(LifeStageKind.EGG, LifeStageKind.JUVENILE, LifeStageKind.ADULT);
    }
}

// Hemimetabolous.java
public record Hemimetabolous() implements Metaboly {
    @Override public List<LifeStageKind> stages() {
        return List.of(LifeStageKind.EGG, LifeStageKind.NYMPH, LifeStageKind.ADULT);
    }
}

// Holometabolous.java
public record Holometabolous() implements Metaboly {
    @Override public List<LifeStageKind> stages() {
        return List.of(LifeStageKind.EGG, LifeStageKind.LARVA, LifeStageKind.PUPA, LifeStageKind.ADULT);
    }
}
```

### `MetabolyTrait.java`

```java
package com.naturalist.insects.lifestage;

import com.naturalist.clades.Trait;

/**
 * Trait wrapper attaching a {@link Metaboly} declaration to a clade.
 * Declared by the insects domain on Hexapoda sub-clades that originated
 * a particular developmental pattern (e.g. Holometabola declares
 * {@code new MetabolyTrait(new Holometabolous())}).
 */
public record MetabolyTrait(Metaboly metaboly) implements Trait {
}
```

### `InsectClades.java`

```java
package com.naturalist.insects;

import com.naturalist.clades.Clade;
import com.naturalist.clades.Holometabola;
import com.naturalist.clades.Trait;
import com.naturalist.insects.lifestage.Holometabolous;
import com.naturalist.insects.lifestage.MetabolyTrait;

import java.util.Set;

/**
 * Insect-domain trait declarations on clade-kernel nodes. The
 * declarations are pure values — adding a clade-trait mapping is a
 * code change to the {@code switch} below, reviewed in the same PR
 * that introduces the trait.
 *
 * <p>Consumers pass {@link #traitsFor} as the third argument to
 * {@link com.naturalist.clades.CladeTraversal#findTrait}.
 */
public final class InsectClades {

    private InsectClades() {
    }

    public static Set<Trait> traitsFor(Clade clade) {
        return switch (clade) {
            case Holometabola _ -> Set.of(new MetabolyTrait(new Holometabolous()));
            // Future:
            //   case Hemiptera _ -> Set.of(new MetabolyTrait(new Hemimetabolous()));
            //   case Odonata _   -> Set.of(new MetabolyTrait(new Hemimetabolous()));
            //   case Zygentoma _ -> Set.of(new MetabolyTrait(new Ametabolous()));
            default -> Set.of();
        };
    }
}
```

---

## Test plan

Two test classes, both in `insects-api/src/test/`.

### `MetabolyTest.java`

Smoke tests for the value type:

- `Holometabolous().stages()` returns `[EGG, LARVA, PUPA, ADULT]` in order.
- `Hemimetabolous().stages()` returns `[EGG, NYMPH, ADULT]`.
- `Ametabolous().stages()` returns `[EGG, JUVENILE, ADULT]`.
- `new Holometabolous().equals(new Holometabolous())` is true (value equality).

### `InsectCladesTest.java`

End-to-end traversal:

- `findTrait(new Papilionidae(), MetabolyTrait.class, InsectClades::traitsFor)`
  returns present, with `metaboly()` equal to `new Holometabolous()`.
- `findTrait(new Lepidoptera(), MetabolyTrait.class, InsectClades::traitsFor)`
  same — traversal walks up to Holometabola.
- `findTrait(new Holometabola(), MetabolyTrait.class, InsectClades::traitsFor)`
  same — declaration at the start node.
- `findTrait(new Insecta(), MetabolyTrait.class, InsectClades::traitsFor)`
  returns empty — declaration sits below Insecta in the tree.
- `findTrait(new Eukaryota(), MetabolyTrait.class, InsectClades::traitsFor)`
  returns empty — far above the declaration.

This is the verification contract from the sketch's Phase 2 description.

---

## Steps

- [ ] Extend `LifeStageKind` with `NYMPH` and `JUVENILE`.
- [ ] Add `clades` dependency to `domains/insects/insects-api/pom.xml`.
- [ ] Write `Metaboly.java` — sealed interface declaring `stages()`.
- [ ] Write `Ametabolous.java`, `Hemimetabolous.java`, `Holometabolous.java` permits.
- [ ] Write `MetabolyTrait.java`.
- [ ] Write `InsectClades.java` — `traitsFor(Clade)` switch with `Holometabola → MetabolyTrait(new Holometabolous())`.
- [ ] Write `MetabolyTest.java` — three permits' `stages()` and equality.
- [ ] Write `InsectCladesTest.java` — five traversal cases.
- [ ] User runs `mvn verify`. Expect green.
- [ ] Commit.
- [ ] Roll work-tracker forward — Phase 2 ✅, Phase 3 next.

---

## Out of scope

- Adding `placedIn: Clade` to `InsectFamily` / `InsectGenus` / `InsectSpecies` (Phase 3).
- New stage records (`NymphStage`, `JuvenileStage`) mirroring `EggStage` etc. — the enum value exists for `Metaboly.stages()` to reference; concrete stage subclass records can wait for an actual hemimetabolous or ametabolous species being catalogued.
- Filling in `placedIn` data anywhere (Phase 4).
- Routing existing life-stage queries through the new resolver (Phase 5).
- Trait declarations for any clade other than Holometabola — Phase 1 didn't seed Hemiptera, Odonata, or Zygentoma permits, so there's nothing to attach those declarations to yet. They go in when those clade permits are added (probably alongside the first non-holometabolous species catalogued).

---

## Risk

- **`LifeStageKind` enum extension.** Adding values is normally safe, but switch statements over `LifeStageKind` would become non-exhaustive. Quick audit: no production `switch` on `LifeStageKind` exists today (`grep` confirms only enum-array iteration via `values()` and `fromSlug`). If a future contributor adds a switch, the new permits will surface as a `default` miss — acceptable.
- **`Metaboly` sealed permits in insects-api.** Once published as a sealed type, adding a new permit (e.g. for a non-insect developmental pattern) requires editing the kernel-ish source file. That's the same property as `Clade` and is intentional — a controlled vocabulary, not free expansion.
- **`InsectClades.traitsFor` switch needs `default`.** Sealed `Clade` has 7 permits; declaring trait info on only one (`Holometabola`) means the switch must use `default -> Set.of()`. Future contributors should not "fix" this to be exhaustive — most clades carry no insect-domain traits, and forcing per-permit declarations would clutter the file without benefit. Documented in the source.

---

## Done-when

- `mvn verify` green at repo root.
- Calling `CladeTraversal.findTrait(new Papilionidae(), MetabolyTrait.class, InsectClades::traitsFor)` returns present with `metaboly() == new Holometabolous()`.
- Calling the same starting at `new Insecta()` returns empty.
- Work-tracker rolls to "Phase 3 — add `placedIn: Clade` to taxon entities" as the next slice.
