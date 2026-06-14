# Paraphyly Fixtures Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Load the paraphyly teaching-exemplar fixtures into the insects catalog, add the clade permits they reference, extend `traitsFor` for Blattodea metaboly, and write the placement-chain monotonicity acceptance test.

**Architecture:** Nine new `Clade` sealed-interface permits in `kernels/clades` form the evolutionary tree nodes these fixtures reference. One existing permit (`Papilionidae`) gets a parent-chain update. The `InsectClades.traitsFor()` switch gains a `Blattodea` arm for hemimetabolous resolution. Catalog JSON is appended (not overwritten) to the existing `insects-repository-test` resource files. A new `ParaphylyPlacementMonotonicityTest` asserts DAG-descendant relationships across the three fixture lineages plus the Battus philenor control.

**Tech Stack:** Java 21 sealed records, Jackson slug serialization, JUnit 5, AssertJ, `NaturalistDatabaseExtension`

**Source spec:** `docs/notes/clade-assignment-investigation/paraphyly-fixtures-spec.md`

---

## Resolved Gates (confirmed via codebase investigation)

| Gate | Resolution |
|------|-----------|
| G1 — `placedIn` wire form | **Slug string.** `Clade` uses `@JsonValue` on `slug()` + `@JsonCreator` factory `of(String)`. Fixture data loads as-is. |
| G2 — functional-role discriminator | **Confirmed.** `parentRank` accepts `ORDER`/`FAMILY`/`GENUS`/`SPECIES`/`SUBSPECIES`. |
| G3 — no SUPERFAMILY rank | **Confirmed.** Only order/family/genus/species exist. Apoidea is clade-only. |
| G4 — empty `commonNames` | **Confirmed.** Empty `[]` is valid JSON for the array field. |

## Design Decisions

1. **Troidini parent chain:** `Troidini → Papilionidae → Papilionoidea → Lepidoptera → Holometabola`. Update `Papilionidae.parent()` from `Lepidoptera` to `Papilionoidea`.

2. **Intermediate ancestors:** No Diptera-level or Hymenoptera-level clade. Chain directly:
   - `Drosophilinae → Holometabola` (trait resolves at Holometabola)
   - `Apoidea → Holometabola` (trait resolves at Holometabola)
   - `Blattodea → Insecta` (trait declared directly on Blattodea via `traitsFor`)

3. **Orders already in catalog:** `diptera`, `hymenoptera`, `blattodea` exist. Only `blattodea` needs `placedIn` added. Others keep their existing `"holometabola"` value.

4. **Control update:** `battus-philenor` species `placedIn` changes from `"papilionidae"` to `"troidini"`.

## New Clade Parent Chains (complete)

```
Eukaryota
└── Animalia
    └── Arthropoda
        └── Insecta
            ├── Hemiptera                          (existing)
            ├── Holometabola                       (existing)
            │   ├── Lepidoptera                    (existing)
            │   │   └── Papilionoidea              ← NEW
            │   │       └── Papilionidae           (existing, re-parented)
            │   │           └── Troidini           ← NEW
            │   ├── Drosophilinae                  ← NEW
            │   │   ├── Sophophora                 ← NEW
            │   │   └── DrosophilaSensuStricto     ← NEW
            │   └── Apoidea                        ← NEW
            │       └── Anthophila                 ← NEW
            └── Blattodea                          ← NEW (hemimetabolous trait bearer)
                └── Termitoidae                    ← NEW
```

---

### Task 1: Add new clade permits to the kernel

**Files:**
- Create: `kernels/clades/src/main/java/com/naturalist/clades/Papilionoidea.java`
- Create: `kernels/clades/src/main/java/com/naturalist/clades/Troidini.java`
- Create: `kernels/clades/src/main/java/com/naturalist/clades/Drosophilinae.java`
- Create: `kernels/clades/src/main/java/com/naturalist/clades/Sophophora.java`
- Create: `kernels/clades/src/main/java/com/naturalist/clades/DrosophilaSensuStricto.java`
- Create: `kernels/clades/src/main/java/com/naturalist/clades/Blattodea.java`
- Create: `kernels/clades/src/main/java/com/naturalist/clades/Termitoidae.java`
- Create: `kernels/clades/src/main/java/com/naturalist/clades/Apoidea.java`
- Create: `kernels/clades/src/main/java/com/naturalist/clades/Anthophila.java`
- Modify: `kernels/clades/src/main/java/com/naturalist/clades/Papilionidae.java:67`
- Modify: `kernels/clades/src/main/java/com/naturalist/clades/Clade.java:37-77`
- Modify: `kernels/clades/src/test/java/com/naturalist/clades/CladeTest.java`

- [ ] **Step 1: Create `Papilionoidea.java`**

```java
package com.naturalist.clades;

import com.naturalist.fieldnotes.Description;

import java.util.Optional;

public record Papilionoidea() implements Clade {

    private static final Description DESCRIPTION = new Description(
            """
            The butterfly superfamily — swallowtails, whites, blues, and \
            brush-foots all belong here.""",
            """
            Papilionoidea is the superfamily of true butterflies. It \
            contains the swallowtails (Papilionidae), whites and sulphurs \
            (Pieridae), brush-footed butterflies (Nymphalidae), blues and \
            hairstreaks (Lycaenidae), and skippers (Hesperiidae). All are \
            day-flying Lepidoptera with clubbed antennae.""",
            """
            Superfamily Papilionoidea — the true butterflies, a clade \
            within Lepidoptera distinguished from moths by clubbed \
            antennae, lack of a frenulum wing-coupling mechanism, and \
            diurnal habits. Contains approximately 18,000 species across \
            seven families.""",
            """
            Papilionoidea — monophyletic clade within Obtectomera \
            (Lepidoptera). Sister to Hedyloidea (moth-butterflies). \
            The clade unites all traditional butterfly families and is \
            strongly supported by molecular phylogenetics. Used here as \
            the intermediate node between Lepidoptera and family-level \
            clades like Papilionidae.""");

    @Override
    public String slug() {
        return "papilionoidea";
    }

    @Override
    public String displayName() {
        return "Papilionoidea";
    }

    @Override
    public Description description() {
        return DESCRIPTION;
    }

    @Override
    public Optional<Clade> parent() {
        return Optional.of(new Lepidoptera());
    }
}
```

- [ ] **Step 2: Create `Troidini.java`**

```java
package com.naturalist.clades;

import com.naturalist.fieldnotes.Description;

import java.util.Optional;

public record Troidini() implements Clade {

    private static final Description DESCRIPTION = new Description(
            """
            The pipevine swallowtails — butterflies whose caterpillars \
            eat pipevine plants and store their poison.""",
            """
            Troidini is a tribe of swallowtail butterflies whose \
            caterpillars specialise on Aristolochiaceae (pipevines). \
            They sequester toxic aristolochic acids from their host \
            plants, making all life stages unpalatable to predators. \
            Battus philenor (Pipevine Swallowtail) is the best-known \
            North American member.""",
            """
            Tribe Troidini (Papilionidae: Papilioninae) — a clade of \
            pharmacophagous swallowtails that sequester aristolochic \
            acids from larval host plants in Aristolochiaceae. The \
            chemical defence drives Müllerian and Batesian mimicry \
            complexes across multiple families.""",
            """
            Tribe Troidini — approximately 135 species in genera \
            including Battus, Parides, Atrophaneura, and Trogonoptera. \
            Monophyletic within Papilioninae; defined by larval \
            aristolochic-acid sequestration and associated aposematic \
            coloration. The tribe is the control fixture for the \
            placement-chain monotonicity test: a clean monophyletic \
            case where per-rank inheritance works trivially.""");

    @Override
    public String slug() {
        return "troidini";
    }

    @Override
    public String displayName() {
        return "Troidini";
    }

    @Override
    public Description description() {
        return DESCRIPTION;
    }

    @Override
    public Optional<Clade> parent() {
        return Optional.of(new Papilionidae());
    }
}
```

- [ ] **Step 3: Create `Drosophilinae.java`**

```java
package com.naturalist.clades;

import com.naturalist.fieldnotes.Description;

import java.util.Optional;

public record Drosophilinae() implements Clade {

    private static final Description DESCRIPTION = new Description(
            """
            A big group of tiny flies that includes all the fruit flies \
            scientists study.""",
            """
            Drosophilinae is a subfamily of small flies that includes \
            the famous fruit flies. It contains the paraphyletic genus \
            Drosophila and all the lineages nested within it.""",
            """
            Subfamily Drosophilinae (Drosophilidae) — the largest \
            subfamily of pomace flies, containing the model organism \
            genus Drosophila and its embedded lineages (Zaprionus, \
            Scaptomyza, Hawaiian drosophilids).""",
            """
            Drosophilinae — the containing crown clade for the \
            paraphyletic genus Drosophila. Because Drosophila is not \
            monophyletic (multiple genera are nested within it), the \
            genus cannot be placed at a 'genus clade'. Instead, the \
            genus-level placement uses Drosophilinae — the smallest \
            clade known to contain the entire genus — while species \
            within it receive finer placements (Sophophora, Drosophila \
            sensu stricto). This is the decisive paraphyly fixture.""");

    @Override
    public String slug() {
        return "drosophilinae";
    }

    @Override
    public String displayName() {
        return "Drosophilinae";
    }

    @Override
    public Description description() {
        return DESCRIPTION;
    }

    @Override
    public Optional<Clade> parent() {
        return Optional.of(new Holometabola());
    }
}
```

- [ ] **Step 4: Create `Sophophora.java`**

```java
package com.naturalist.clades;

import com.naturalist.fieldnotes.Description;

import java.util.Optional;

public record Sophophora() implements Clade {

    private static final Description DESCRIPTION = new Description(
            """
            The group of fruit flies that includes the one scientists \
            study most — the common fruit fly near your bananas.""",
            """
            Sophophora is a subgenus of Drosophila that contains the \
            common fruit fly (D. melanogaster) — the most studied \
            insect in the world. It is only distantly related to the \
            fly the genus is named after.""",
            """
            Subgenus Sophophora — the clade within Drosophilinae \
            containing D. melanogaster. Despite being the canonical \
            'Drosophila' in lab parlance, melanogaster is far from \
            the type species D. funebris in subgenus Drosophila sensu \
            stricto.""",
            """
            Sophophora — a monophyletic subgenus within the \
            paraphyletic genus Drosophila. Contains D. melanogaster, \
            D. simulans, D. yakuba, and relatives. The placement of \
            melanogaster here (not in the same sub-clade as the type \
            species funebris) is the crux of the nomenclatural case: \
            a genus split would require renaming melanogaster unless \
            the type species were changed (ICZN Case 3407, not \
            adopted).""");

    @Override
    public String slug() {
        return "sophophora";
    }

    @Override
    public String displayName() {
        return "Sophophora";
    }

    @Override
    public Description description() {
        return DESCRIPTION;
    }

    @Override
    public Optional<Clade> parent() {
        return Optional.of(new Drosophilinae());
    }
}
```

- [ ] **Step 5: Create `DrosophilaSensuStricto.java`**

```java
package com.naturalist.clades;

import com.naturalist.fieldnotes.Description;

import java.util.Optional;

public record DrosophilaSensuStricto() implements Clade {

    private static final Description DESCRIPTION = new Description(
            """
            The group of fruit flies that the genus name really \
            belongs to — including the dark-winged fly D. funebris.""",
            """
            Drosophila sensu stricto is the subgenus containing the \
            type species D. funebris — the fly that gives the whole \
            genus Drosophila its name, even though it is less famous \
            than the fruit fly scientists study.""",
            """
            Subgenus Drosophila sensu stricto — the clade containing \
            the type species D. funebris. Because this subgenus anchors \
            the genus name, a future split of the paraphyletic genus \
            would leave 'Drosophila' with this group, potentially \
            forcing melanogaster into a new genus.""",
            """
            Drosophila sensu stricto — the subgenus containing the \
            type species D. funebris (Fabricius 1787). The type species \
            fixes the name Drosophila to this clade under ICZN rules. \
            Sibling to Sophophora within Drosophilinae; the two sub- \
            clades illustrate genus-level paraphyly: same Linnaean \
            genus, different evolutionary sub-clades, neither \
            derivable from the other.""");

    @Override
    public String slug() {
        return "drosophila-sensu-stricto";
    }

    @Override
    public String displayName() {
        return "Drosophila sensu stricto";
    }

    @Override
    public Description description() {
        return DESCRIPTION;
    }

    @Override
    public Optional<Clade> parent() {
        return Optional.of(new Drosophilinae());
    }
}
```

- [ ] **Step 6: Create `Blattodea.java`**

```java
package com.naturalist.clades;

import com.naturalist.fieldnotes.Description;

import java.util.Optional;

public record Blattodea() implements Clade {

    private static final Description DESCRIPTION = new Description(
            """
            Cockroaches and termites together — termites are really \
            just cockroaches that learned to live in big families.""",
            """
            Blattodea is the clade containing all cockroaches and all \
            termites. Termites used to have their own order (Isoptera) \
            but scientists discovered they are actually a branch of \
            the cockroach family tree — so 'cockroaches without \
            termites' is not a real group.""",
            """
            Clade Blattodea — cockroaches including termites. The \
            traditional 'Isoptera' (termites) is a derived clade \
            nested within Blattodea, sister to Cryptocercus. This \
            makes 'cockroaches excluding termites' a paraphyletic \
            grade. Hemimetabolous development.""",
            """
            Blattodea — hemimetabolous order-level clade within \
            Polyneoptera/Dictyoptera. Monophyletic only with termites \
            included (Inward et al. 2007; Lo et al. 2000). The \
            embedding of eusocial termites within a solitary-ancestral \
            cockroach lineage is the textbook example of a former \
            order collapsing into a clade within another. Used here \
            as the trait-bearing node for hemimetabolous development \
            in this lineage — the insect-domain traitsFor switch \
            declares Blattodea → Hemimetabolous.""");

    @Override
    public String slug() {
        return "blattodea";
    }

    @Override
    public String displayName() {
        return "Blattodea";
    }

    @Override
    public Description description() {
        return DESCRIPTION;
    }

    @Override
    public Optional<Clade> parent() {
        return Optional.of(new Insecta());
    }
}
```

- [ ] **Step 7: Create `Termitoidae.java`**

```java
package com.naturalist.clades;

import com.naturalist.fieldnotes.Description;

import java.util.Optional;

public record Termitoidae() implements Clade {

    private static final Description DESCRIPTION = new Description(
            """
            Termites — tiny pale insects that live together in huge \
            families and eat wood. They are really a kind of cockroach.""",
            """
            Termitoidae is the termite clade — all the termites form \
            one branch inside the cockroach tree. They evolved to live \
            in enormous colonies with queens, workers, and soldiers, \
            and they are champion wood-decomposers.""",
            """
            Termitoidae (= Isoptera sensu lato) — the termite clade \
            nested within Blattodea, sister to the wood-roach \
            Cryptocercus. Eusociality is a derived trait of this clade, \
            below the ordinal level — a sub-ordinal trait that resolves \
            on a clade, not on the order.""",
            """
            Termitoidae — approximately 3,000 species of eusocial \
            cockroaches. Former order Isoptera, now a clade within \
            Blattodea. Eusociality evolved once in this lineage, \
            facilitated by subsocial behaviour and gut symbionts \
            inherited from the cryptocercid ancestor. The termite \
            fixture proves trait decoupling: metaboly (hemimetabolous) \
            is stable across the Blattodea clade boundary, while \
            eusociality is a derived trait restricted to this sub- \
            ordinal node.""");

    @Override
    public String slug() {
        return "termitoidae";
    }

    @Override
    public String displayName() {
        return "Termitoidae";
    }

    @Override
    public Description description() {
        return DESCRIPTION;
    }

    @Override
    public Optional<Clade> parent() {
        return Optional.of(new Blattodea());
    }
}
```

- [ ] **Step 8: Create `Apoidea.java`**

```java
package com.naturalist.clades;

import com.naturalist.fieldnotes.Description;

import java.util.Optional;

public record Apoidea() implements Clade {

    private static final Description DESCRIPTION = new Description(
            """
            The apoid wasps and bees together — bees are really a \
            kind of wasp that switched to eating pollen.""",
            """
            Apoidea is the group that includes both the hunting wasps \
            (like beewolves) and all the bees. Bees evolved from \
            within these wasps when some switched from hunting insects \
            to collecting pollen and nectar from flowers.""",
            """
            Apoidea — the apoid wasps and bees. A clade within \
            Hymenoptera that corresponds to no single Linnaean rank \
            above family. The 'wasp' lineages of Apoidea are \
            paraphyletic with respect to the bees (Anthophila), which \
            arose from within them.""",
            """
            Apoidea (EOL:676) — superfamily-level clade within \
            Aculeata (Hymenoptera). Contains Crabronidae sensu lato \
            (paraphyletic apoid wasps) plus Anthophila (bees). The \
            paraphyly of 'wasps' with respect to bees means a \
            beewolf's smallest honest clade is Apoidea itself — \
            broader than its family rank. This fixture proves \
            placedIn values that map to no Linnaean rank.""");

    @Override
    public String slug() {
        return "apoidea";
    }

    @Override
    public String displayName() {
        return "Apoidea";
    }

    @Override
    public Description description() {
        return DESCRIPTION;
    }

    @Override
    public Optional<Clade> parent() {
        return Optional.of(new Holometabola());
    }
}
```

- [ ] **Step 9: Create `Anthophila.java`**

```java
package com.naturalist.clades;

import com.naturalist.fieldnotes.Description;

import java.util.Optional;

public record Anthophila() implements Clade {

    private static final Description DESCRIPTION = new Description(
            """
            All the bees — from tiny sweat bees to big bumble bees \
            and honey bees. They all collect pollen to feed their \
            babies.""",
            """
            Anthophila is the clade of all bees. Bees evolved from \
            hunting wasps and switched to collecting pollen and nectar \
            instead of prey. There is no single Linnaean rank for \
            'all bees' — the clade spans several families (Apidae, \
            Halictidae, Andrenidae, and more).""",
            """
            Anthophila — the bee clade, nested within Apoidea. \
            Corresponds to no Linnaean rank: it spans multiple \
            families (Apidae, Halictidae, Andrenidae, Megachilidae, \
            Colletidae, etc.). A pollinator functional role attaches \
            to this clade, not to any rank.""",
            """
            Anthophila — monophyletic clade of approximately 20,000 \
            species of pollen-collecting Hymenoptera, nested within \
            the apoid wasps. The strongest 'clade ≠ rank' case in \
            this fixture suite: a placedIn value that maps to no \
            Linnaean rank at all. Ties the POLLINATOR functional \
            guild to a clade rather than a rank, reinforcing why \
            InsectFunctionalRole is cross-rank (parentName: \
            InsectRankName) rather than a species field.""");

    @Override
    public String slug() {
        return "anthophila";
    }

    @Override
    public String displayName() {
        return "Anthophila";
    }

    @Override
    public Description description() {
        return DESCRIPTION;
    }

    @Override
    public Optional<Clade> parent() {
        return Optional.of(new Apoidea());
    }
}
```

- [ ] **Step 10: Update `Papilionidae.parent()` to chain through `Papilionoidea`**

In `kernels/clades/src/main/java/com/naturalist/clades/Papilionidae.java`, change:

```java
    @Override
    public Optional<Clade> parent() {
        return Optional.of(new Lepidoptera());
    }
```

to:

```java
    @Override
    public Optional<Clade> parent() {
        return Optional.of(new Papilionoidea());
    }
```

- [ ] **Step 11: Update `Clade` sealed interface permits list**

In `kernels/clades/src/main/java/com/naturalist/clades/Clade.java`, change the permits list from:

```java
public sealed interface Clade
        permits Eukaryota,
        Animalia,
        Arthropoda,
        Insecta,
        Hemiptera,
        Holometabola,
        Lepidoptera,
        Papilionidae {
```

to:

```java
public sealed interface Clade
        permits Eukaryota,
        Animalia,
        Anthophila,
        Apoidea,
        Arthropoda,
        Blattodea,
        DrosophilaSensuStricto,
        Drosophilinae,
        Holometabola,
        Hemiptera,
        Insecta,
        Lepidoptera,
        Papilionidae,
        Papilionoidea,
        Sophophora,
        Termitoidae,
        Troidini {
```

- [ ] **Step 12: Update `Clade.of()` factory switch**

In the same file, update the `@JsonCreator` factory to add the new slugs:

```java
    @JsonCreator
    static Clade of(String slug) {
        return switch (slug) {
            case "anthophila" -> new Anthophila();
            case "animalia" -> new Animalia();
            case "apoidea" -> new Apoidea();
            case "arthropoda" -> new Arthropoda();
            case "blattodea" -> new Blattodea();
            case "drosophila-sensu-stricto" -> new DrosophilaSensuStricto();
            case "drosophilinae" -> new Drosophilinae();
            case "eukaryota" -> new Eukaryota();
            case "hemiptera" -> new Hemiptera();
            case "holometabola" -> new Holometabola();
            case "insecta" -> new Insecta();
            case "lepidoptera" -> new Lepidoptera();
            case "papilionidae" -> new Papilionidae();
            case "papilionoidea" -> new Papilionoidea();
            case "sophophora" -> new Sophophora();
            case "termitoidae" -> new Termitoidae();
            case "troidini" -> new Troidini();
            default -> throw new IllegalArgumentException("Unknown clade: " + slug);
        };
    }
```

- [ ] **Step 13: Update `CladeTest.java`**

The `ALL` list and parent-chain test need updating to include the new permits.

Update the `ALL` list to include all 17 permits:

```java
    private static final List<Clade> ALL = List.of(
            new Anthophila(),
            new Animalia(),
            new Apoidea(),
            new Arthropoda(),
            new Blattodea(),
            new DrosophilaSensuStricto(),
            new Drosophilinae(),
            new Eukaryota(),
            new Hemiptera(),
            new Holometabola(),
            new Insecta(),
            new Lepidoptera(),
            new Papilionidae(),
            new Papilionoidea(),
            new Sophophora(),
            new Termitoidae(),
            new Troidini());
```

Update the `onlyEukaryotaHasEmptyParent` stream to include all non-root permits (replace the inline Stream.of with filtering ALL):

```java
    @Test
    void onlyEukaryotaHasEmptyParent() {
        assertThat(new Eukaryota().parent()).isEmpty();
        ALL.stream()
                .filter(c -> !(c instanceof Eukaryota))
                .forEach(c -> assertThat(c.parent()).as(c.slug() + ".parent").isPresent());
    }
```

Update the parent-chain test to reflect `Papilionidae → Papilionoidea`:

```java
    @Test
    void parentChainResolvesFromTroidiniToEukaryota() {
        assertThat(new Troidini().parent()).contains(new Papilionidae());
        assertThat(new Papilionidae().parent()).contains(new Papilionoidea());
        assertThat(new Papilionoidea().parent()).contains(new Lepidoptera());
        assertThat(new Lepidoptera().parent()).contains(new Holometabola());
        assertThat(new Holometabola().parent()).contains(new Insecta());
        assertThat(new Insecta().parent()).contains(new Arthropoda());
        assertThat(new Arthropoda().parent()).contains(new Animalia());
        assertThat(new Animalia().parent()).contains(new Eukaryota());
        assertThat(new Eukaryota().parent()).isEmpty();
    }
```

- [ ] **Step 14: Commit**

```bash
git add kernels/clades/src/main/java/com/naturalist/clades/ kernels/clades/src/test/java/com/naturalist/clades/
git commit -m "feat(clades): add 9 paraphyly-fixture permits + re-parent Papilionidae

Add Papilionoidea, Troidini, Drosophilinae, Sophophora,
DrosophilaSensuStricto, Blattodea, Termitoidae, Apoidea, Anthophila.

Re-parent Papilionidae through Papilionoidea (was direct to Lepidoptera).
Update sealed permits list and Clade.of() factory.

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 2: Extend `InsectClades.traitsFor()` for Blattodea

**Files:**
- Modify: `domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectClades.java:33-43`

- [ ] **Step 1: Add Blattodea import and switch arm**

Add import:
```java
import com.naturalist.clades.Blattodea;
```

Update the `traitsFor` method:

```java
    public static Set<Trait> traitsFor(Clade clade) {
        return switch (clade) {
            case Holometabola _ -> Set.of(new MetabolyTrait(new Holometabolous()));
            case Hemiptera _ -> Set.of(new MetabolyTrait(new Hemimetabolous()));
            case Blattodea _ -> Set.of(new MetabolyTrait(new Hemimetabolous()));
            default -> Set.of();
        };
    }
```

- [ ] **Step 2: Commit**

```bash
git add domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectClades.java
git commit -m "feat(insects): map Blattodea → Hemimetabolous in traitsFor

Blattodea is hemimetabolous but is neither Hemiptera nor descended from it.
Without this mapping, roach/termite fixtures resolve no metaboly.

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 3: Merge catalog data into insects-repository-test resources

**Files:**
- Modify: `domains/insects/insects-repository-test/src/main/resources/insects/insect-orders.json`
- Modify: `domains/insects/insects-repository-test/src/main/resources/insects/insect-families.json`
- Modify: `domains/insects/insects-repository-test/src/main/resources/insects/insect-genera.json`
- Modify: `domains/insects/insects-repository-test/src/main/resources/insects/insect-species.json`
- Modify: `domains/insects/insects-repository-test/src/main/resources/insects/insect-functional-roles.json`

**Source data:** `docs/notes/clade-assignment-investigation/insect-*.json` and `insect-functional-roles.json`

- [ ] **Step 1: Update `blattodea` order — add `placedIn`**

In `insect-orders.json`, the existing `blattodea` entry (last in the array) is missing `placedIn`. Add it after `commonNames`:

```json
    "placedIn": "blattodea"
```

The existing description, commonNames, etc. remain unchanged. Do NOT replace with the fixture's shorter descriptions — the existing Oak Vista descriptions are richer.

- [ ] **Step 2: Append four families + update `apidae` in `insect-families.json`**

**Append** these four records to the end of the JSON array (before the closing `]`). Copy from `docs/notes/clade-assignment-investigation/insect-families.json` verbatim:

- `drosophilidae` (orderName: diptera, placedIn: null)
- `blattidae` (orderName: blattodea, placedIn: null)
- `rhinotermitidae` (orderName: blattodea, placedIn: null)
- `crabronidae` (orderName: hymenoptera, placedIn: "apoidea")

**Update existing `apidae`** (confirmed at line 236 in the file): it already exists with rich Oak Vista descriptions but NO `placedIn` field. Add `"placedIn": "anthophila"` after the `commonNames` array. Do NOT replace the existing description or commonNames.

- [ ] **Step 3: Append five genera to `insect-genera.json`**

Append from `docs/notes/clade-assignment-investigation/insect-genera.json`:

- `drosophila` (familyName: drosophilidae, placedIn: "drosophilinae")
- `periplaneta` (familyName: blattidae, placedIn: null)
- `reticulitermes` (familyName: rhinotermitidae, placedIn: null)
- `apis` (familyName: apidae, placedIn: null)
- `philanthus` (familyName: crabronidae, placedIn: null)

- [ ] **Step 4: Append six species to `insect-species.json`**

Append from `docs/notes/clade-assignment-investigation/insect-species.json`:

- `drosophila-melanogaster` (genusName: drosophila, placedIn: "sophophora")
- `drosophila-funebris` (genusName: drosophila, placedIn: "drosophila-sensu-stricto")
- `periplaneta-americana` (genusName: periplaneta, placedIn: "blattodea")
- `reticulitermes-hesperus` (genusName: reticulitermes, placedIn: "termitoidae")
- `apis-mellifera` (genusName: apis, placedIn: null)
- `philanthus-gibbosus` (genusName: philanthus, placedIn: null)

- [ ] **Step 5: Append functional role for apis-mellifera**

Append to `insect-functional-roles.json` from `docs/notes/clade-assignment-investigation/insect-functional-roles.json`:

```json
  {
    "name": "019ec455-d43e-71de-9dab-deb3b09ad1de",
    "parentRank": "SPECIES",
    "parentName": "apis-mellifera",
    "guilds": [
      "POLLINATOR"
    ],
    "beneficial": true
  }
```

- [ ] **Step 6: Update `battus-philenor` species — change `placedIn` to `"troidini"`**

In `insect-species.json`, find the `battus-philenor` entry and change:
```json
    "placedIn": "papilionidae",
```
to:
```json
    "placedIn": "troidini",
```

- [ ] **Step 7: Commit**

```bash
git add domains/insects/insects-repository-test/src/main/resources/insects/
git commit -m "feat(insects): merge paraphyly fixture catalog data

Append 4-5 families, 5 genera, 6 species, 1 functional role.
Add placedIn to blattodea order. Update battus-philenor placedIn
from papilionidae to troidini (finer control placement).

Fixture lineages: Drosophila (genus paraphyly), Blattodea/termites
(order absorption), bees-in-wasps (rank-less clade).

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 4: Add test identifiers for new fixture entities

**Files:**
- Modify: `domains/identifiers-test/src/main/java/com/naturalist/insects/TestInsectsIdentifiers.java`

- [ ] **Step 1: Add `Blattodea` order identifier**

In the `InsectOrder` class, add:

```java
        public static class Blattodea {
            public static final InsectOrderName name = InsectOrderName.of("blattodea");
        }
```

- [ ] **Step 2: Add new family identifiers**

In the `InsectFamily` class, add (alphabetical placement):

```java
        public static class Blattidae {
            public static final InsectFamilyName name = InsectFamilyName.of("blattidae");
        }

        public static class Crabronidae {
            public static final InsectFamilyName name = InsectFamilyName.of("crabronidae");
        }

        public static class Drosophilidae {
            public static final InsectFamilyName name = InsectFamilyName.of("drosophilidae");
        }

        public static class Rhinotermitidae {
            public static final InsectFamilyName name = InsectFamilyName.of("rhinotermitidae");
        }
```

- [ ] **Step 3: Add new genus identifiers**

In the `InsectGenus` class, add:

```java
        public static class Apis {
            public static final InsectGenusName name = InsectGenusName.of("apis");
        }

        public static class Drosophila {
            public static final InsectGenusName name = InsectGenusName.of("drosophila");
        }

        public static class Periplaneta {
            public static final InsectGenusName name = InsectGenusName.of("periplaneta");
        }

        public static class Philanthus {
            public static final InsectGenusName name = InsectGenusName.of("philanthus");
        }

        public static class Reticulitermes {
            public static final InsectGenusName name = InsectGenusName.of("reticulitermes");
        }
```

- [ ] **Step 4: Add new species identifiers**

In the `InsectSpecies` class, add:

```java
        public static class ApisMellifera {
            public static final InsectSpeciesName name = InsectSpeciesName.of("apis-mellifera");

            public static class FunctionalRole {
                public static final InsectFunctionalRoleId name = InsectFunctionalRoleId.of(
                        UUID.fromString("019ec455-d43e-71de-9dab-deb3b09ad1de"));
            }
        }

        public static class DrosophilaFunebris {
            public static final InsectSpeciesName name = InsectSpeciesName.of("drosophila-funebris");
        }

        public static class DrosophilaMelanogaster {
            public static final InsectSpeciesName name = InsectSpeciesName.of("drosophila-melanogaster");
        }

        public static class PeriplanetaAmericana {
            public static final InsectSpeciesName name = InsectSpeciesName.of("periplaneta-americana");
        }

        public static class PhilanthusGibbosus {
            public static final InsectSpeciesName name = InsectSpeciesName.of("philanthus-gibbosus");
        }

        public static class ReticulitermesHesperus {
            public static final InsectSpeciesName name = InsectSpeciesName.of("reticulitermes-hesperus");
        }
```

- [ ] **Step 5: Commit**

```bash
git add domains/identifiers-test/src/main/java/com/naturalist/insects/TestInsectsIdentifiers.java
git commit -m "feat(identifiers): add test identifiers for paraphyly fixture entities

Orders: Blattodea. Families: Blattidae, Crabronidae, Drosophilidae,
Rhinotermitidae. Genera: Apis, Drosophila, Periplaneta, Philanthus,
Reticulitermes. Species: six fixture species + functional role id.

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 5: Write placement-chain monotonicity acceptance test

**Files:**
- Create: `domains/insects/insects-repository-test/src/test/java/com/naturalist/insects/ParaphylyPlacementMonotonicityTest.java`
- Modify: `domains/insects/insects-repository-test/src/test/java/com/naturalist/insects/CladePlacementResolutionTest.java`

This test asserts the spec's §6 monotonicity relations: `child.placedIn` is descendant-or-equal of `parent.placedIn` in the clade DAG whenever both are non-null.

- [ ] **Step 1: Create `ParaphylyPlacementMonotonicityTest.java`**

```java
package com.naturalist.insects;

import com.naturalist.clades.Anthophila;
import com.naturalist.clades.Apoidea;
import com.naturalist.clades.Blattodea;
import com.naturalist.clades.Clade;
import com.naturalist.clades.CladeTraversal;
import com.naturalist.clades.DrosophilaSensuStricto;
import com.naturalist.clades.Drosophilinae;
import com.naturalist.clades.Holometabola;
import com.naturalist.clades.Sophophora;
import com.naturalist.clades.Termitoidae;
import com.naturalist.clades.Troidini;
import com.naturalist.data.NaturalistDatabaseExtension;
import com.naturalist.insects.lifestage.Hemimetabolous;
import com.naturalist.insects.lifestage.Holometabolous;
import com.naturalist.insects.lifestage.MetabolyTrait;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Acceptance suite for placement-chain monotonicity across the paraphyly
 * fixtures. Asserts that {@code child.placedIn} is a descendant-or-equal
 * of {@code parent.placedIn} in the clade DAG whenever both are non-null.
 * <p>
 * Three fixture lineages exercise non-trivial paths:
 * <ul>
 *   <li>2.1 Drosophila — genus-level paraphyly (decisive case)</li>
 *   <li>2.2 Blattodea/termites — order/family absorption</li>
 *   <li>2.3 Bees within apoid wasps — rank-less clade</li>
 * </ul>
 * Plus the Battus philenor control (clean monophyletic nesting).
 *
 * @see <a href="docs/notes/clade-assignment-investigation/paraphyly-fixtures-spec.md">Spec §6</a>
 */
class ParaphylyPlacementMonotonicityTest {

    @RegisterExtension
    NaturalistDatabaseExtension db = NaturalistDatabaseExtension.create();

    // -- Utility: DAG-descendant check --

    /**
     * Returns true if {@code candidate} is a descendant-or-equal of
     * {@code ancestor} in the clade parent-chain DAG.
     */
    private static boolean isDescendantOrEqual(Clade candidate, Clade ancestor) {
        return CladeTraversal.ancestry(candidate).contains(ancestor);
    }

    // ===== 2.0 Control — Battus philenor (monophyletic) =====

    @Test
    void control_battusPhilenorPlacedAtTroidini() {
        InsectSpecies species = loadSpecies(
                TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name);

        assertThat(species.placedIn()).isEqualTo(new Troidini());
    }

    @Test
    void control_troidiniIsDescendantOfHolometabola() {
        assertThat(isDescendantOrEqual(new Troidini(), new Holometabola())).isTrue();
    }

    @Test
    void control_battusPhilenorResolvesHolometabolous() {
        InsectSpecies species = loadSpecies(
                TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name);

        Optional<MetabolyTrait> trait = CladeTraversal.findTrait(
                species.placedIn(), MetabolyTrait.class, InsectClades::traitsFor);

        assertThat(trait).contains(new MetabolyTrait(new Holometabolous()));
    }

    // ===== 2.1 Drosophila — genus paraphyly (decisive case) =====

    @Test
    void drosophila_genusPlacedAtDrosophilinae() {
        InsectGenus genus = loadGenus(
                TestInsectsIdentifiers.InsectGenus.Drosophila.name);

        assertThat(genus.placedIn()).isEqualTo(new Drosophilinae());
    }

    @Test
    void drosophila_melanogasterPlacedAtSophophora() {
        InsectSpecies species = loadSpecies(
                TestInsectsIdentifiers.InsectSpecies.DrosophilaMelanogaster.name);

        assertThat(species.placedIn()).isEqualTo(new Sophophora());
    }

    @Test
    void drosophila_funebrisPlacedAtDrosophilaSensuStricto() {
        InsectSpecies species = loadSpecies(
                TestInsectsIdentifiers.InsectSpecies.DrosophilaFunebris.name);

        assertThat(species.placedIn()).isEqualTo(new DrosophilaSensuStricto());
    }

    @Test
    void drosophila_monotonicity_sophohoraDescendantOfDrosophilinae() {
        assertThat(isDescendantOrEqual(new Sophophora(), new Drosophilinae())).isTrue();
    }

    @Test
    void drosophila_monotonicity_drosophilaSensuStrictoDescendantOfDrosophilinae() {
        assertThat(isDescendantOrEqual(new DrosophilaSensuStricto(), new Drosophilinae()))
                .isTrue();
    }

    @Test
    void drosophila_melanogasterResolvesHolometabolous() {
        InsectSpecies species = loadSpecies(
                TestInsectsIdentifiers.InsectSpecies.DrosophilaMelanogaster.name);

        Optional<MetabolyTrait> trait = CladeTraversal.findTrait(
                species.placedIn(), MetabolyTrait.class, InsectClades::traitsFor);

        assertThat(trait).contains(new MetabolyTrait(new Holometabolous()));
    }

    @Test
    void drosophila_funebrisResolvesHolometabolous() {
        InsectSpecies species = loadSpecies(
                TestInsectsIdentifiers.InsectSpecies.DrosophilaFunebris.name);

        Optional<MetabolyTrait> trait = CladeTraversal.findTrait(
                species.placedIn(), MetabolyTrait.class, InsectClades::traitsFor);

        assertThat(trait).contains(new MetabolyTrait(new Holometabolous()));
    }

    // ===== 2.2 Blattodea/termites — order absorption =====

    @Test
    void blattodea_orderPlacedAtBlattodea() {
        InsectOrder order = loadOrder(
                TestInsectsIdentifiers.InsectOrder.Blattodea.name);

        assertThat(order.placedIn()).isEqualTo(new Blattodea());
    }

    @Test
    void blattodea_periplanetaAmericanaPlacedAtBlattodea() {
        InsectSpecies species = loadSpecies(
                TestInsectsIdentifiers.InsectSpecies.PeriplanetaAmericana.name);

        assertThat(species.placedIn()).isEqualTo(new Blattodea());
    }

    @Test
    void blattodea_reticulitermesHesperusPlacedAtTermitoidae() {
        InsectSpecies species = loadSpecies(
                TestInsectsIdentifiers.InsectSpecies.ReticulitermesHesperus.name);

        assertThat(species.placedIn()).isEqualTo(new Termitoidae());
    }

    @Test
    void blattodea_monotonicity_termitoidaeDescendantOfBlattodea() {
        assertThat(isDescendantOrEqual(new Termitoidae(), new Blattodea())).isTrue();
    }

    @Test
    void blattodea_periplanetaAmericanaResolvesHemimetabolous() {
        InsectSpecies species = loadSpecies(
                TestInsectsIdentifiers.InsectSpecies.PeriplanetaAmericana.name);

        Optional<MetabolyTrait> trait = CladeTraversal.findTrait(
                species.placedIn(), MetabolyTrait.class, InsectClades::traitsFor);

        assertThat(trait).contains(new MetabolyTrait(new Hemimetabolous()));
    }

    @Test
    void blattodea_reticulitermesHesperusResolvesHemimetabolous() {
        InsectSpecies species = loadSpecies(
                TestInsectsIdentifiers.InsectSpecies.ReticulitermesHesperus.name);

        Optional<MetabolyTrait> trait = CladeTraversal.findTrait(
                species.placedIn(), MetabolyTrait.class, InsectClades::traitsFor);

        assertThat(trait).contains(new MetabolyTrait(new Hemimetabolous()));
    }

    // ===== 2.3 Bees within apoid wasps — rank-less clade =====

    @Test
    void bees_apidaeFamilyPlacedAtAnthophila() {
        InsectFamily family = loadFamily(
                TestInsectsIdentifiers.InsectFamily.Apidae.name);

        assertThat(family.placedIn()).isEqualTo(new Anthophila());
    }

    @Test
    void bees_crabronidaeFamilyPlacedAtApoidea() {
        InsectFamily family = loadFamily(
                TestInsectsIdentifiers.InsectFamily.Crabronidae.name);

        assertThat(family.placedIn()).isEqualTo(new Apoidea());
    }

    @Test
    void bees_monotonicity_anthophilaDescendantOfApoidea() {
        assertThat(isDescendantOrEqual(new Anthophila(), new Apoidea())).isTrue();
    }

    @Test
    void bees_apidaeResolvesHolometabolous() {
        InsectFamily family = loadFamily(
                TestInsectsIdentifiers.InsectFamily.Apidae.name);

        Optional<MetabolyTrait> trait = CladeTraversal.findTrait(
                family.placedIn(), MetabolyTrait.class, InsectClades::traitsFor);

        assertThat(trait).contains(new MetabolyTrait(new Holometabolous()));
    }

    @Test
    void bees_crabronidaeResolvesHolometabolous() {
        InsectFamily family = loadFamily(
                TestInsectsIdentifiers.InsectFamily.Crabronidae.name);

        Optional<MetabolyTrait> trait = CladeTraversal.findTrait(
                family.placedIn(), MetabolyTrait.class, InsectClades::traitsFor);

        assertThat(trait).contains(new MetabolyTrait(new Holometabolous()));
    }

    // -- Helpers --

    private InsectOrder loadOrder(InsectOrderName name) {
        return db.getNamed(InsectOrderTestEntitySource.class)
                .getByName(name).orElseThrow();
    }

    private InsectFamily loadFamily(InsectFamilyName name) {
        return db.getNamed(InsectFamilyTestEntitySource.class)
                .getByName(name).orElseThrow();
    }

    private InsectGenus loadGenus(InsectGenusName name) {
        return db.getNamed(InsectGenusTestEntitySource.class)
                .getByName(name).orElseThrow();
    }

    private InsectSpecies loadSpecies(InsectSpeciesName name) {
        return db.getNamed(InsectSpeciesTestEntitySource.class)
                .getByName(name).orElseThrow();
    }
}
```

- [ ] **Step 2: Update existing `CladePlacementResolutionTest` — fix battus-philenor assertion**

In `CladePlacementResolutionTest.java`, the test `battusPhilenorPlacementResolvesToHolometabolyTraitViaPapilionidae` at line 43 asserts:

```java
        assertThat(battusPhilenor.placedIn()).isEqualTo(new Papilionidae());
```

Change to:

```java
        assertThat(battusPhilenor.placedIn()).isEqualTo(new Troidini());
```

Also add the `Troidini` import:
```java
import com.naturalist.clades.Troidini;
```

And rename the test method to reflect the updated assertion:
```java
    @Test
    void battusPhilenorPlacementResolvesToHolometabolyTraitViaTroidini() {
```

- [ ] **Step 3: Verify the test compiles and all assertions are coherent**

Run `mvn verify` from the repository root. Expected: all tests pass including the new monotonicity test and the updated battus-philenor assertion.

- [ ] **Step 4: Commit**

```bash
git add domains/insects/insects-repository-test/src/test/java/com/naturalist/insects/ParaphylyPlacementMonotonicityTest.java
git add domains/insects/insects-repository-test/src/test/java/com/naturalist/insects/CladePlacementResolutionTest.java
git commit -m "test(insects): add paraphyly placement-chain monotonicity acceptance suite

ParaphylyPlacementMonotonicityTest asserts DAG-descendant monotonicity
across three fixture lineages (Drosophila genus paraphyly, Blattodea
order absorption, bees-in-apoid-wasps) plus the Battus philenor control.

Update existing CladePlacementResolutionTest: battus-philenor now
resolves via Troidini (finer placement) rather than Papilionidae.

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 6: Load EOL citations into the library domain

**Files:**
- Modify: `domains/library/library-repository-test/src/main/resources/library/citations.json`
- Modify: `domains/identifiers-test/src/main/java/com/naturalist/library/TestLibraryIdentifiers.java`

**Source data:** `docs/notes/clade-assignment-investigation/eol-citations.csl.json`

Transform CSL-JSON records to the library domain's `OnlineSource` wire format and append. Skip `apis-mellifera` (already exists as `eol-apis-mellifera-1045608`). The `philanthus-gibbosus` species page is unresolved — use genus page (EOL:104130) as fallback per spec §6.5.

- [ ] **Step 1: Append 11 citation records to `citations.json`**

Append to the existing array (before closing `]`):

```json
  {
    "kind": "ONLINE_SOURCE",
    "name": "eol-drosophila-melanogaster-733739",
    "authorityReference": {
      "source": { "id": "eol", "displayName": "Encyclopedia of Life" },
      "url": "https://eol.org/pages/733739"
    },
    "title": "Drosophila melanogaster Meigen 1830 (Common Fruit Fly)",
    "author": "EOL Curators",
    "year": 2026,
    "lastModified": null
  },
  {
    "kind": "ONLINE_SOURCE",
    "name": "eol-drosophila-funebris-733824",
    "authorityReference": {
      "source": { "id": "eol", "displayName": "Encyclopedia of Life" },
      "url": "https://eol.org/pages/733824"
    },
    "title": "Drosophila funebris (Fabricius 1787)",
    "author": "EOL Curators",
    "year": 2026,
    "lastModified": null
  },
  {
    "kind": "ONLINE_SOURCE",
    "name": "eol-periplaneta-americana-1076920",
    "authorityReference": {
      "source": { "id": "eol", "displayName": "Encyclopedia of Life" },
      "url": "https://eol.org/pages/1076920"
    },
    "title": "Periplaneta americana (Linnaeus 1758) (American Cockroach)",
    "author": "EOL Curators",
    "year": 2026,
    "lastModified": null
  },
  {
    "kind": "ONLINE_SOURCE",
    "name": "eol-reticulitermes-hesperus-469438",
    "authorityReference": {
      "source": { "id": "eol", "displayName": "Encyclopedia of Life" },
      "url": "https://eol.org/pages/469438"
    },
    "title": "Reticulitermes hesperus Banks 1920 (Western Subterranean Termite)",
    "author": "EOL Curators",
    "year": 2026,
    "lastModified": null
  },
  {
    "kind": "ONLINE_SOURCE",
    "name": "eol-philanthus-gibbosus-104130",
    "authorityReference": {
      "source": { "id": "eol", "displayName": "Encyclopedia of Life" },
      "url": "https://eol.org/pages/104130"
    },
    "title": "Philanthus gibbosus (Fabricius 1775) (Hump-backed Beewolf)",
    "author": null,
    "year": 2026,
    "lastModified": null
  },
  {
    "kind": "ONLINE_SOURCE",
    "name": "eol-periplaneta-31515",
    "authorityReference": {
      "source": { "id": "eol", "displayName": "Encyclopedia of Life" },
      "url": "https://eol.org/pages/31515"
    },
    "title": "Periplaneta Burmeister 1838 (genus)",
    "author": "EOL Curators",
    "year": 2026,
    "lastModified": null
  },
  {
    "kind": "ONLINE_SOURCE",
    "name": "eol-philanthus-104130",
    "authorityReference": {
      "source": { "id": "eol", "displayName": "Encyclopedia of Life" },
      "url": "https://eol.org/pages/104130"
    },
    "title": "Philanthus Fabricius 1790 (Beewolves, genus)",
    "author": "EOL Curators",
    "year": 2026,
    "lastModified": null
  },
  {
    "kind": "ONLINE_SOURCE",
    "name": "eol-apoidea-676",
    "authorityReference": {
      "source": { "id": "eol", "displayName": "Encyclopedia of Life" },
      "url": "https://eol.org/pages/676"
    },
    "title": "Apoidea (Apoid Wasps)",
    "author": "EOL Curators",
    "year": 2026,
    "lastModified": null
  },
  {
    "kind": "ONLINE_SOURCE",
    "name": "eol-holometabola-3016961",
    "authorityReference": {
      "source": { "id": "eol", "displayName": "Encyclopedia of Life" },
      "url": "https://eol.org/pages/3016961"
    },
    "title": "Endopterygota / Holometabola (Endopterygotes)",
    "author": "EOL Curators",
    "year": 2026,
    "lastModified": null
  },
  {
    "kind": "ONLINE_SOURCE",
    "name": "eol-hymenoptera-648",
    "authorityReference": {
      "source": { "id": "eol", "displayName": "Encyclopedia of Life" },
      "url": "https://eol.org/pages/648"
    },
    "title": "Hymenoptera (Wasps, Bees, And Ants)",
    "author": "EOL Curators",
    "year": 2026,
    "lastModified": null
  },
  {
    "kind": "ONLINE_SOURCE",
    "name": "eol-crabronidae-7493",
    "authorityReference": {
      "source": { "id": "eol", "displayName": "Encyclopedia of Life" },
      "url": "https://eol.org/pages/7493"
    },
    "title": "Crabronidae (Crabronid Wasps)",
    "author": "EOL Curators",
    "year": 2026,
    "lastModified": null
  }
```

**Note on `eol-philanthus-gibbosus-104130`:** The species page was unresolvable via EOL search. This record uses the genus page (EOL:104130) as a fallback reference per spec §6.5. The name encodes the genus page id to be honest about what was confirmed. Resolve the true species page via the EOL API (`/api/search`) if ingestion tooling is added later.

- [ ] **Step 2: Add test identifiers for fixture citations**

In `TestLibraryIdentifiers.java`, add to the `Citations` class:

```java
        public static final CitationName EolFruitFly =
                CitationName.of("eol-drosophila-melanogaster-733739");

        public static final CitationName EolFunebris =
                CitationName.of("eol-drosophila-funebris-733824");

        public static final CitationName EolAmericanCockroach =
                CitationName.of("eol-periplaneta-americana-1076920");

        public static final CitationName EolWesternTermite =
                CitationName.of("eol-reticulitermes-hesperus-469438");

        public static final CitationName EolBeewolf =
                CitationName.of("eol-philanthus-gibbosus-104130");
```

- [ ] **Step 3: Commit**

```bash
git add domains/library/library-repository-test/src/main/resources/library/citations.json
git add domains/identifiers-test/src/main/java/com/naturalist/library/TestLibraryIdentifiers.java
git commit -m "feat(library): load EOL citations for paraphyly fixtures

11 OnlineSource records transformed from CSL-JSON: 5 species,
2 genera, 4 clade/order pages. Apis mellifera skipped (exists).
Philanthus gibbosus uses genus page as fallback (species page
unresolved via EOL search).

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Out of Scope (deferred)

- **Family-rank dispute notes** (§6.6 gate 4/5): Rhinotermitidae vs Heterotermitidae, Crabronidae vs Philanthidae — captured in university-level Durrell descriptions already present in the fixture data. No code change needed; the disputes are the teaching material.
- **`InsectLifeStages` integration for new fixtures:** The existing resolver already chains through `CladeTraversal.findTrait` → `MetabolyTrait` → stage list. No new code needed — the new clade permits and `traitsFor` extension make it work automatically.
- **Cross-domain citation linkage:** InsectSpecies does not yet carry a `CitationName` provenance field. When that field lands, link these fixture species to their corresponding citation by slug. The citations exist in the library catalog and are query-ready; the soft-FK wiring is a separate effort.
