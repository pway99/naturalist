ok # Clades Kernel — Phase 1 Slice Plan

> Promoted from [`clades-kernel.md`](../../clades-kernel.md) Phase 1 on 2026-05-12.
> Revised the same day after design review converged on a sealed-type
> vocabulary kernel rather than entity-with-repository scaffolding.

**Goal.** Stand up the `clades` kernel as a small, curated vocabulary of
the evolutionary tree of life — `Clade` as a `sealed interface` with one
stateless `record` permit per recognised clade, plus a `Trait` marker
interface and a parent-chain traversal helper. The kernel ships with the
Phase 1 slice **Eukaryota → Animalia → Arthropoda → Insecta → Holometabola
→ Lepidoptera → Papilionidae** as compile-time permits; future phases
extend by adding new permits, not by editing data.

---

## Design decision — sealed vocabulary, not entity registry

The original sketch proposed `Clade` as a `NamedEntity` with `CladeName`
identifier, `CladeRepository`, `CladeQuery`, `CladeTestEntitySource`, and a
JSON seed. That collapsed under three observations:

1. **Cardinality is small.** The full project upper bound is on the order of
   100–150 clades — Phase 1 needs 7. The Phase 1 plan explicitly puts
   importing a taxonomic backbone (Catalogue of Life / ChecklistBank)
   out of scope. The catalog is curated and deliberately added.
2. **The kernel-vs-domain pattern in the codebase is clear.** `Bioregion` is
   a sealed interface in `kernels/biogeography/` with record permits.
   `LinnaeanFamily` is an interface contract in `kernels/taxonomy/` and
   per-domain records implement it. The codebase has no precedent for a
   kernel-level `NamedEntity` with repository scaffolding, and we should
   not invent one for a vocabulary this small.
3. **Single tree identity must be preserved.** Per-domain Clade entities
   would create parallel trees with duplicate Eukaryota nodes and forced
   trait-declaration duplication. Sealed records sidestep this via
   **value identity** — `new Holometabola().equals(new Holometabola())` is
   `true` because records derive `equals`/`hashCode` from their components,
   and stateless permits have no components. Every domain that constructs
   `new Holometabola()` reaches the same logical node by value equality;
   no singleton instances, no registry, no startup wiring.

The sealed design also resolves a layering concern: trait *declarations*
("Holometabola is holometabolous") are domain knowledge and belong in the
consuming domain (insects-api), not in the kernel. The kernel-provided
`CladeTraversal#findTrait` takes a `Function<Clade, Set<Trait>>` from the
caller, so the kernel stays trait-agnostic.

---

## Architecture

Single kernel module — no repository module, no core module, no test
fixtures module.

```
kernels/clades/
└── src/main/java/com/naturalist/clades/
    ├── Clade.java          — sealed interface; @JsonValue slug; @JsonCreator of(slug)
    ├── Eukaryota.java      — permit (parent: null)
    ├── Animalia.java       — permit (parent: Eukaryota)
    ├── Arthropoda.java     — permit
    ├── Insecta.java        — permit
    ├── Holometabola.java   — permit
    ├── Lepidoptera.java    — permit
    ├── Papilionidae.java   — permit
    ├── Trait.java          — marker interface for domain-declared traits
    └── CladeTraversal.java — static ancestry() + findTrait(start, type, traitsFor)
```

Each permit is a stateless `record FooClade() implements Clade` with hardcoded
`slug()`, `displayName()`, `parent()`, and a private static `Description`
constant built from four Java TextBlocks (preschool → university). Pattern
matches `biogeography.SacramentoValley` but uses TextBlocks for the
multi-paragraph description text, which keeps each permit file readable.

Stateless records derive `equals`/`hashCode` from their (zero) components, so
`new Holometabola().equals(new Holometabola())` is `true` and the two
instances hash identically. Consumers freely use clade instances as map keys,
in sets, in equality checks, and as `switch` selectors. **This is value
identity, not singleton identity** — there is no shared instance, just shared
value semantics — so no consumer needs to know about a registry.

`parent()` returns `Optional<Clade>` rather than `@Nullable Clade`. The root
(`Eukaryota.parent()`) returns `Optional.empty()`; every other permit returns
`Optional.of(new ParentClade())`. The `Optional` return keeps traversal
call sites NPE-free without a per-site null check.

---

## What's intentionally absent

| Feature | Why not |
| --- | --- |
| `CladeName` `EntityName` subclass | Sealed types reference by record instance or by `Clade.of(slug)`. A separate name type adds no safety; the compiler already knows every permit. |
| `CladeRepository` / `CladeQuery` | No data layer needed. Lookups are static; the parent chain is in code. |
| `CladeTestEntitySource` / JSON seed | Same — there is no "data" to load. The seven permits are the data. |
| `CladeCollection` (`BehavioralCollection`) | No query returns a collection of clades through a repository port. If a future consumer needs one it can be added then. |
| Trait declarations in the kernel | Domain-owned — insects-api will declare Holometabola → MetabolyTrait when Phase 2 lands. |

---

## File map

### `kernels/clades/`

| File | Responsibility |
| --- | --- |
| `pom.xml` | Module pom; parent `kernels`; depends on `framework`, `field-notes`. No `identifiers` dep — sealed types do not use `EntityName`. |
| `src/main/java/com/naturalist/clades/Clade.java` | Sealed interface listing all permits; `@JsonValue slug()`; `@JsonCreator of(String)` switch resolving slug → permit. |
| `src/main/java/com/naturalist/clades/Eukaryota.java` … `Papilionidae.java` | Seven stateless records, each carrying hardcoded slug / displayName / parent / TextBlock-based Description. |
| `src/main/java/com/naturalist/clades/Trait.java` | Marker interface, no methods. |
| `src/main/java/com/naturalist/clades/CladeTraversal.java` | `static ancestry(Clade)` returns the chain from start to root inclusive; `static <T extends Trait> Optional<T> findTrait(Clade start, Class<T> type, Function<Clade, Set<Trait>> traitsFor)` walks the parent chain looking for the nearest declared trait. |
| `src/test/java/com/naturalist/clades/CladeTest.java` | Slug uniqueness, parent-chain integrity, `Clade.of(slug)` roundtrip, unknown-slug rejection, JSON `@JsonValue`/`@JsonCreator` roundtrip via `ObjectMapper`, record equality. |
| `src/test/java/com/naturalist/clades/CladeTraversalTest.java` | `ancestry()` from `Papilionidae` returns all 7 in order; `findTrait` returns the nearest declared, returns empty when none declared, prefers nearer over farther when both ancestors declare. Uses test-local `SampleTrait` / `OtherTrait` record permits of `Trait`. |

### Top-level configuration

| File | Change |
| --- | --- |
| `kernels/pom.xml` | Add `<module>clades</module>` to `<modules>` (alphabetical, after `catalog-inmem`). |
| `pom.xml` | Add `<dependency>clades</dependency>` to root `<dependencyManagement>` under `<!-- KERNELS -->` (alphabetical). |
| `kernels/CLAUDE.md` | New `### clades` section describing the sealed-type vocabulary and the rationale for it; trait-ownership note; DAG line update. |
| `CLAUDE.md` (root) | Clades in kernel layout + DAG line `clades → framework, field-notes`. |

---

## Single PR

The original plan had this as four PRs (modules / seed / repository /
query). The sealed design collapses to **one PR** — there is no
repository, no seed JSON, no query adapter, no contract test interface.
Phase 1 lands as a single ~600-line change.

### Steps

- [ ] Create `kernels/clades/pom.xml` and the source directories.
- [ ] Write `Clade.java` — sealed interface, `@JsonValue slug()`, `@JsonCreator of(String)` switch.
- [ ] Write `Trait.java` — marker interface.
- [ ] Write `CladeTraversal.java` — `ancestry()` and `findTrait()`.
- [ ] Write the 7 permit records — each stateless `record FooClade() implements Clade` with hardcoded slug / displayName / parent / TextBlock-based Description.
- [ ] Write `CladeTest.java` — slug/displayName/description integrity across all permits, parent chain, `of` factory, JSON roundtrip.
- [ ] Write `CladeTraversalTest.java` — ancestry walks from `Papilionidae` to `Eukaryota`; `findTrait` returns nearest, returns empty when no match, prefers nearer when two ancestors declare.
- [ ] Register `<module>clades</module>` in `kernels/pom.xml`.
- [ ] Add `clades` dependency-management entry in root `pom.xml`.
- [ ] Update `kernels/CLAUDE.md` with the new `### clades` section, trait-ownership note, DAG line, and qualified "no entity definitions" hard rule.
- [ ] Update root `CLAUDE.md` with `clades/` in kernel layout and the DAG line.
- [ ] User runs `mvn verify`. Expect green.
- [ ] Commit.

---

## Permit-record shape (reference)

```java
package com.naturalist.clades;

import com.naturalist.fieldnotes.Description;

import java.util.Optional;

public record Holometabola() implements Clade {

    private static final Description DESCRIPTION = new Description(
            """
            Some insects start out looking very different from their \
            grown-up shape. A caterpillar turns into a butterfly; a grub \
            turns into a beetle. Insects that do this big change are \
            called holometabolous.""",
            """
            Holometabola is the part of the insect tree whose members go \
            through complete metamorphosis: egg → larva → pupa → adult …""",
            """
            Superorder Holometabola (sometimes called Endopterygota) is \
            the clade of insects that undergo complete metamorphosis …""",
            """
            Holometabola — strongly supported monophyletic clade …""");

    @Override public String slug() { return "holometabola"; }
    @Override public String displayName() { return "Holometabola"; }
    @Override public Description description() { return DESCRIPTION; }
    @Override public Optional<Clade> parent() { return Optional.of(new Insecta()); }
}
```

The trailing `\` on each line of a TextBlock continues the string without
inserting a newline — so the rendered paragraphs flow naturally rather
than carrying the source-code line wraps.

---

## How later phases plug in

- **Phase 2 — define `Metaboly` and attach to Holometabola.** Introduces
  `Metaboly` as a sealed value type (developmental biology vocabulary) and
  `MetabolyTrait` as a `Trait` subtype. The trait declaration lives in
  insects-api (or wherever insects-domain trait knowledge ends up) as a
  pure pattern-matching function — **not** a Map registry:

  ```java
  // in insects-api or insects-core, owned by the insects domain
  public final class InsectTraitDeclarations {
      public static Set<Trait> traitsFor(Clade c) {
          return switch (c) {
              case Holometabola _ -> Set.of(new MetabolyTrait(HOLOMETABOLOUS));
              // future: Hemiptera _ -> hemimetabolous, etc.
              default             -> Set.of();
          };
      }
  }
  ```

  Resolving life stages for a species:

  ```java
  CladeTraversal.findTrait(
      species.placedIn(),
      MetabolyTrait.class,
      InsectTraitDeclarations::traitsFor);
  ```

  Plants-api will expose its own equivalent function; nothing is shared
  except the `Clade` value identities themselves. No registry, no startup
  wiring, no mutable surface.

- **Phase 3 — add `placedIn: Clade` to taxon entities.** Each Linnaean
  rank record gains an `Optional<Clade> placedIn()` accessor (or
  `@Nullable Clade` field per the existing record-component convention —
  to be settled in the Phase 3 slice plan). **References are
  value-level — a `Clade` record instance — not a `Class<? extends Clade>`
  type-level reference.** Value-level matches how `Bioregion` is used
  elsewhere in the codebase, lets call sites pattern-match
  (`switch (species.placedIn().orElse(null)) { … }`), and roundtrips
  through Jackson via the kernel's `@JsonValue slug()` + `@JsonCreator
  of(String)` without further wiring.

- **Phase 4 — place insects into the clade DAG.** Just code: `new
  LinnaeanFamily(…).withPlacedIn(new Papilionidae())`. No JSON edits, no
  data loading.

- **Phase 5 — route life-stage queries through the clade resolver.** The
  resolver is `CladeTraversal.findTrait` against the consuming domain's
  switch-based trait function (see Phase 2 example).

- **Phase 6 — extend to plants.** Adding plant clades is adding permits
  to the sealed `Clade` interface (Plantae, Archaeplastida, Magnoliids,
  Aristolochiaceae, etc.) and `case` arms to `Clade.of`. Plants-api adds
  its own switch-based trait function. The permits list lengthens; the
  shape stays identical.

---

## Risks

- **Sealed list grows.** When the permit count reaches ~30–50, the
  permits clause in `Clade.java` and the switch in `Clade.of` start
  feeling long. That's still fine. If it ever crosses ~150 (project upper
  bound estimate), revisit whether code-as-data has hit its limit. Until
  then the constraint is a feature: every addition is a reviewed PR.
- **TextBlock description content drifts.** Description text is in code;
  edits require a Java compile cycle. Acceptable — the descriptions are
  curated content, not configuration that changes frequently.
- **JSON discriminator on `Set<Trait>` (Phase 2).** Trait types are
  polymorphic; deserialising a `Set<Trait>` from JSON will require
  Jackson polymorphic configuration when Phase 2 wires Metaboly into
  insects records. Not a Phase 1 concern.

---

## Done-when

- `mvn verify` is green at repo root.
- A consumer can write `CladeTraversal.ancestry(new Papilionidae())` and
  get the chain back to `new Eukaryota()`.
- `CladeTraversal.findTrait(new Papilionidae(), SomeTrait.class, ...)`
  works against a caller-supplied declaration map.
- `kernels/CLAUDE.md` documents the clades section and the sealed-type
  rationale.
- Work-tracker rolls forward to "Phase 2 — define `Metaboly` and attach
  to Holometabola" as the next slice.
