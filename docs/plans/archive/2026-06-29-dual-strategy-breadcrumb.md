# Dual-Strategy Clade Breadcrumb Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Render a two-row phylogenetic/Linnaean breadcrumb on clade pages, moving clade assembly logic from the console into the library domain.

**Architecture:** `library-api` gains three new types (`CladeStep`, `CladeView`, `CladeQuery`) that expose clade ancestry with aligned Linnaean ranks. `library-core` owns the curated clade→rank correspondence (`CladeRanks`) and the assembly factory (`CladeViewFactory`). The console controller becomes thin — it calls the query and renders the result. Per-rank concept pages (`/concepts/kingdom`, etc.) provide link targets for the rank row.

**Tech Stack:** Java 21 records, JTE templates, CSS Grid, Observer framework, `clades` and `taxonomy` kernels.

## Global Constraints

- `clades` kernel is read-only — no changes to sealed permits, `CladeCatalog`, or `CladeTraversal`.
- `taxonomy` kernel is read-only — `LinealRank` enum is used as-is.
- `library-api` may depend on `clades` and `taxonomy` — these are reasonable kernel deps for the domain that teaches the tree of life.
- `CladeQuery` does **not** extend `EntityQuery` — no repository, no persisted entity.
- `CladeViewFactory` is package-private, concrete, no interface (ADR-020/ADR-010).
- No new modules. All code lands in existing `library-api`, `library-core`, `library-console`, `library-test-context`.
- Rank concept entries cover `KINGDOM` through `SPECIES` (7 entries). `SUBSPECIES` is excluded because no current clade maps to it — add when first needed.

---

### Task 1: library-api read surface — CladeStep, CladeView, CladeQuery

**Files:**
- Modify: `domains/library/library-api/pom.xml`
- Create: `domains/library/library-api/src/main/java/com/naturalist/library/CladeStep.java`
- Create: `domains/library/library-api/src/main/java/com/naturalist/library/CladeView.java`
- Create: `domains/library/library-api/src/main/java/com/naturalist/library/CladeQuery.java`
- Test: `domains/library/library-api/src/test/java/com/naturalist/library/CladeStepTest.java`
- Test: `domains/library/library-api/src/test/java/com/naturalist/library/CladeViewTest.java`

**Interfaces:**
- Consumes: `LinealRank` from `kernels/taxonomy`, `Observable`/`ValueObject`/`ReadModel` from `kernels/framework`
- Produces: `CladeStep` (ValueObject), `CladeView` (ReadModel), `CladeQuery` (public query interface with nested `CladeTreeNode`). Task 2 implements `CladeQuery`; Task 4 consumes all three from templates.

- [ ] **Step 1: Add `clades` and `taxonomy` dependencies to library-api pom.xml**

In `domains/library/library-api/pom.xml`, add two dependencies after the existing `catalog` dependency (alphabetical order):

```xml
        <dependency>
            <groupId>com.naturalist</groupId>
            <artifactId>clades</artifactId>
        </dependency>
```

and after `framework`:

```xml
        <dependency>
            <groupId>com.naturalist</groupId>
            <artifactId>taxonomy</artifactId>
        </dependency>
```

The full `<dependencies>` section becomes (in order): `authority`, `catalog`, `clades`, `field-notes`, `framework`, `identifiers`, `taxonomy`, then test deps.

- [ ] **Step 2: Write the CladeStep invariant test**

Create `domains/library/library-api/src/test/java/com/naturalist/library/CladeStepTest.java`:

```java
package com.naturalist.library;

import com.naturalist.observability.InvariantObservation;
import com.naturalist.observability.Observer;
import com.naturalist.taxonomy.LinealRank;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class CladeStepTest {

    private static final Observer observer = Observer.forClass(CladeStepTest.class);

    @Test
    void validStepWithRank() {
        var mo = observer.forMethod("validStepWithRank");
        var step = new CladeStep("animalia", "Animalia", Optional.of(LinealRank.KINGDOM));

        InvariantObservation result = mo.observable(step, "step");

        assertThat(result.violations()).isEmpty();
    }

    @Test
    void validStepWithoutRank() {
        var mo = observer.forMethod("validStepWithoutRank");
        var step = new CladeStep("holometabola", "Holometabola", Optional.empty());

        InvariantObservation result = mo.observable(step, "step");

        assertThat(result.violations()).isEmpty();
    }

    @Test
    void nullComponentsProduceExpectedViolations() {
        var mo = observer.forMethod("nullComponentsProduceExpectedViolations");
        var step = new CladeStep(null, null, null);

        InvariantObservation result = mo.observable(step, "step");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .containsExactlyInAnyOrder(
                        ".step.cladeSlug",
                        ".step.displayName",
                        ".step.rank");
    }
}
```

- [ ] **Step 3: Run CladeStepTest to verify it fails**

Run: `mvn test -pl domains/library/library-api -Dtest=CladeStepTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: FAIL — `CladeStep` does not exist yet.

- [ ] **Step 4: Implement CladeStep**

Create `domains/library/library-api/src/main/java/com/naturalist/library/CladeStep.java`:

```java
package com.naturalist.library;

import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.Constraints;
import com.naturalist.taxonomy.LinealRank;

import java.util.Optional;
import java.util.function.Consumer;

public record CladeStep(
        String cladeSlug,
        String displayName,
        Optional<LinealRank> rank
) implements ValueObject {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .notBlank(cladeSlug, "cladeSlug")
                .notBlank(displayName, "displayName")
                .notNull(rank, "rank");
    }
}
```

- [ ] **Step 5: Run CladeStepTest to verify it passes**

Run: `mvn test -pl domains/library/library-api -Dtest=CladeStepTest`
Expected: PASS — all three tests green.

- [ ] **Step 6: Write the CladeView invariant test**

Create `domains/library/library-api/src/test/java/com/naturalist/library/CladeViewTest.java`:

```java
package com.naturalist.library;

import com.naturalist.observability.InvariantObservation;
import com.naturalist.observability.Observer;
import com.naturalist.taxonomy.LinealRank;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class CladeViewTest {

    private static final Observer observer = Observer.forClass(CladeViewTest.class);

    @Test
    void validViewPassesAllInvariants() {
        var mo = observer.forMethod("validViewPassesAllInvariants");
        var view = new CladeView(
                new CladeStep("lepidoptera", "Lepidoptera", Optional.of(LinealRank.ORDER)),
                List.of(
                        new CladeStep("eukaryota", "Eukaryota", Optional.empty()),
                        new CladeStep("animalia", "Animalia", Optional.of(LinealRank.KINGDOM))),
                List.of(
                        new CladeStep("papilionoidea", "Papilionoidea", Optional.empty())));

        InvariantObservation result = mo.observable(view, "view");

        assertThat(result.violations()).isEmpty();
    }

    @Test
    void nullComponentsProduceExpectedViolations() {
        var mo = observer.forMethod("nullComponentsProduceExpectedViolations");
        var view = new CladeView(null, null, null);

        InvariantObservation result = mo.observable(view, "view");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .containsExactlyInAnyOrder(
                        ".view.subject",
                        ".view.ancestry",
                        ".view.children");
    }
}
```

- [ ] **Step 7: Implement CladeView**

Create `domains/library/library-api/src/main/java/com/naturalist/library/CladeView.java`:

```java
package com.naturalist.library;

import com.naturalist.ddd.ReadModel;
import com.naturalist.observability.Constraints;

import java.util.List;
import java.util.function.Consumer;

public record CladeView(
        CladeStep subject,
        List<CladeStep> ancestry,
        List<CladeStep> children
) implements ReadModel {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .valueObject(subject, "subject")
                .notNull(ancestry, "ancestry")
                .notNull(children, "children");
    }
}
```

- [ ] **Step 8: Run CladeViewTest to verify it passes**

Run: `mvn test -pl domains/library/library-api -Dtest=CladeViewTest`
Expected: PASS.

- [ ] **Step 9: Create CladeQuery interface**

Create `domains/library/library-api/src/main/java/com/naturalist/library/CladeQuery.java`:

```java
package com.naturalist.library;

import com.naturalist.taxonomy.LinealRank;

import java.util.List;
import java.util.Optional;

public interface CladeQuery {

    Optional<CladeView> getBySlug(String slug);

    CladeTreeNode tree();

    record CladeTreeNode(
            String slug,
            String displayName,
            Optional<LinealRank> rank,
            List<CladeTreeNode> children
    ) {}
}
```

- [ ] **Step 10: Run all library-api tests to verify nothing is broken**

Run: `mvn test -pl domains/library/library-api`
Expected: PASS — all existing + new tests green.

- [ ] **Step 11: Commit**

```bash
git add domains/library/library-api/pom.xml \
       domains/library/library-api/src/main/java/com/naturalist/library/CladeStep.java \
       domains/library/library-api/src/main/java/com/naturalist/library/CladeView.java \
       domains/library/library-api/src/main/java/com/naturalist/library/CladeQuery.java \
       domains/library/library-api/src/test/java/com/naturalist/library/CladeStepTest.java \
       domains/library/library-api/src/test/java/com/naturalist/library/CladeViewTest.java
git commit -m "feat(library): add CladeStep, CladeView, CladeQuery read surface for dual-strategy breadcrumb"
```

---

### Task 2: library-core logic — CladeRanks, CladeViewFactory, CladeQueryImpl

**Files:**
- Create: `domains/library/library-core/src/main/java/com/naturalist/library/CladeRanks.java`
- Create: `domains/library/library-core/src/main/java/com/naturalist/library/CladeViewFactory.java`
- Create: `domains/library/library-core/src/main/java/com/naturalist/library/CladeQueryImpl.java`
- Test: `domains/library/library-core/src/test/java/com/naturalist/library/CladeViewFactoryTest.java`

**Interfaces:**
- Consumes: `CladeStep`, `CladeView`, `CladeQuery` (from Task 1); `Clade`, `CladeCatalog`, `CladeTraversal` from `kernels/clades`; `LinealRank` from `kernels/taxonomy`
- Produces: `CladeQueryImpl` (package-private `@DomainService`, implements `CladeQuery`). Task 4 wires it via `LibraryTestContext` and calls it from `CladesController`.

- [ ] **Step 1: Write the CladeViewFactory test — ancestry order**

Create `domains/library/library-core/src/test/java/com/naturalist/library/CladeViewFactoryTest.java`:

```java
package com.naturalist.library;

import com.naturalist.taxonomy.LinealRank;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class CladeViewFactoryTest {

    private final CladeViewFactory factory = new CladeViewFactory();

    @Test
    void ancestryIsRootToParentOrder() {
        Optional<CladeView> result = factory.buildBySlug("lepidoptera");

        assertThat(result).isPresent();
        CladeView view = result.get();
        assertThat(view.ancestry().stream().map(CladeStep::cladeSlug).toList())
                .containsExactly(
                        "eukaryota", "animalia", "arthropoda",
                        "insecta", "holometabola");
    }

    @Test
    void subjectIsExcludedFromAncestry() {
        Optional<CladeView> result = factory.buildBySlug("lepidoptera");

        assertThat(result).isPresent();
        assertThat(result.get().ancestry().stream().map(CladeStep::cladeSlug))
                .doesNotContain("lepidoptera");
    }

    @Test
    void subjectCarriesCorrectSlugAndDisplayName() {
        Optional<CladeView> result = factory.buildBySlug("lepidoptera");

        assertThat(result).isPresent();
        assertThat(result.get().subject().cladeSlug()).isEqualTo("lepidoptera");
        assertThat(result.get().subject().displayName()).isEqualTo("Lepidoptera");
    }

    @Test
    void rankedCladesCarryTheirLinealRank() {
        Optional<CladeView> result = factory.buildBySlug("lepidoptera");

        assertThat(result).isPresent();
        CladeView view = result.get();

        assertThat(stepBySlug(view, "animalia").rank())
                .isEqualTo(Optional.of(LinealRank.KINGDOM));
        assertThat(stepBySlug(view, "arthropoda").rank())
                .isEqualTo(Optional.of(LinealRank.PHYLUM));
        assertThat(stepBySlug(view, "insecta").rank())
                .isEqualTo(Optional.of(LinealRank.CLASS));
        assertThat(view.subject().rank())
                .isEqualTo(Optional.of(LinealRank.ORDER));
    }

    @Test
    void ranklessCladesYieldEmptyRank() {
        Optional<CladeView> result = factory.buildBySlug("lepidoptera");

        assertThat(result).isPresent();
        CladeView view = result.get();

        assertThat(stepBySlug(view, "eukaryota").rank()).isEmpty();
        assertThat(stepBySlug(view, "holometabola").rank()).isEmpty();
    }

    @Test
    void papilionoideaIsRankless() {
        Optional<CladeView> result = factory.buildBySlug("papilionoidea");

        assertThat(result).isPresent();
        assertThat(result.get().subject().rank()).isEmpty();
    }

    @Test
    void childrenAreDirectDescendantsSortedByName() {
        Optional<CladeView> result = factory.buildBySlug("insecta");

        assertThat(result).isPresent();
        assertThat(result.get().children().stream().map(CladeStep::cladeSlug).toList())
                .containsExactly("blattodea", "hemiptera", "holometabola");
    }

    @Test
    void rootCladeHasEmptyAncestry() {
        Optional<CladeView> result = factory.buildBySlug("eukaryota");

        assertThat(result).isPresent();
        assertThat(result.get().ancestry()).isEmpty();
    }

    @Test
    void unknownSlugReturnsEmpty() {
        Optional<CladeView> result = factory.buildBySlug("unobtainium");

        assertThat(result).isEmpty();
    }

    @Test
    void treeRootIsEukaryota() {
        CladeQuery.CladeTreeNode tree = factory.buildTree();

        assertThat(tree.slug()).isEqualTo("eukaryota");
        assertThat(tree.rank()).isEmpty();
    }

    @Test
    void treeContainsNestedChildren() {
        CladeQuery.CladeTreeNode tree = factory.buildTree();

        // Eukaryota → Animalia → Arthropoda → Insecta
        assertThat(tree.children()).hasSize(1);
        CladeQuery.CladeTreeNode animalia = tree.children().getFirst();
        assertThat(animalia.slug()).isEqualTo("animalia");
        assertThat(animalia.rank()).isEqualTo(Optional.of(LinealRank.KINGDOM));
    }

    private CladeStep stepBySlug(CladeView view, String slug) {
        return view.ancestry().stream()
                .filter(s -> s.cladeSlug().equals(slug))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no step with slug: " + slug));
    }
}
```

- [ ] **Step 2: Run CladeViewFactoryTest to verify it fails**

Run: `mvn test -pl domains/library/library-core -Dtest=CladeViewFactoryTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: FAIL — `CladeViewFactory` does not exist.

- [ ] **Step 3: Implement CladeRanks**

Create `domains/library/library-core/src/main/java/com/naturalist/library/CladeRanks.java`:

```java
package com.naturalist.library;

import com.naturalist.clades.Clade;
import com.naturalist.taxonomy.LinealRank;

import java.util.Map;
import java.util.Optional;

/**
 * Curated clade → Linnaean rank correspondence. Ranked clades appear here;
 * rank-less clades (Eukaryota, Holometabola, Papilionoidea, etc.) are simply
 * absent and map to {@code Optional.empty()}.
 */
final class CladeRanks {

    private static final Map<String, LinealRank> RANK_BY_SLUG = Map.ofEntries(
            Map.entry("animalia", LinealRank.KINGDOM),
            Map.entry("arthropoda", LinealRank.PHYLUM),
            Map.entry("insecta", LinealRank.CLASS),
            Map.entry("blattodea", LinealRank.ORDER),
            Map.entry("hemiptera", LinealRank.ORDER),
            Map.entry("lepidoptera", LinealRank.ORDER),
            Map.entry("papilionidae", LinealRank.FAMILY),
            Map.entry("termitoidae", LinealRank.FAMILY)
    );

    private CladeRanks() {}

    static Optional<LinealRank> rankFor(Clade clade) {
        return Optional.ofNullable(RANK_BY_SLUG.get(clade.slug()));
    }
}
```

- [ ] **Step 4: Implement CladeViewFactory**

Create `domains/library/library-core/src/main/java/com/naturalist/library/CladeViewFactory.java`:

```java
package com.naturalist.library;

import com.naturalist.clades.Clade;
import com.naturalist.clades.CladeCatalog;
import com.naturalist.clades.CladeTraversal;
import com.naturalist.clades.Eukaryota;
import com.naturalist.observability.Level;
import com.naturalist.observability.Observer;

import java.util.List;
import java.util.Optional;

class CladeViewFactory {

    private static final Observer observer = Observer.forClass(CladeViewFactory.class);

    Optional<CladeView> buildBySlug(String slug) {
        observer.arguments("buildBySlug", i -> i.notBlank(slug, "slug")).throwWhenInvalid();
        Clade clade;
        try {
            clade = Clade.of(slug);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }

        List<Clade> chain = CladeTraversal.ancestry(clade); // [clade, parent, ..., root]
        List<CladeStep> ancestry = chain.reversed().stream()
                .filter(c -> !c.equals(clade))
                .map(CladeViewFactory::toStep)
                .toList();

        CladeStep subject = toStep(clade);

        List<CladeStep> children = CladeCatalog.childrenOf(clade).stream()
                .map(CladeViewFactory::toStep)
                .toList();

        CladeView view = new CladeView(subject, ancestry, children);
        observer.observable(view, "cladeView").observe(Level.WARN);
        return Optional.of(view);
    }

    CladeQuery.CladeTreeNode buildTree() {
        return buildTreeNode(new Eukaryota());
    }

    private CladeQuery.CladeTreeNode buildTreeNode(Clade clade) {
        List<CladeQuery.CladeTreeNode> children = CladeCatalog.childrenOf(clade).stream()
                .map(this::buildTreeNode)
                .toList();
        return new CladeQuery.CladeTreeNode(
                clade.slug(), clade.displayName(), CladeRanks.rankFor(clade), children);
    }

    private static CladeStep toStep(Clade clade) {
        return new CladeStep(clade.slug(), clade.displayName(), CladeRanks.rankFor(clade));
    }
}
```

- [ ] **Step 5: Implement CladeQueryImpl**

Create `domains/library/library-core/src/main/java/com/naturalist/library/CladeQueryImpl.java`:

```java
package com.naturalist.library;

import com.naturalist.infrastructure.DomainService;
import com.naturalist.observability.Observer;

import java.util.Optional;

@DomainService
class CladeQueryImpl implements CladeQuery {

    private static final Observer observer = Observer.forClass(CladeQueryImpl.class);
    private final CladeViewFactory factory;

    CladeQueryImpl() {
        this.factory = new CladeViewFactory();
    }

    @Override
    public Optional<CladeView> getBySlug(String slug) {
        observer.arguments("getBySlug", i -> i.notBlank(slug, "slug")).throwWhenInvalid();
        return factory.buildBySlug(slug);
    }

    @Override
    public CladeTreeNode tree() {
        return factory.buildTree();
    }
}
```

- [ ] **Step 6: Run CladeViewFactoryTest to verify all tests pass**

Run: `mvn test -pl domains/library/library-core -Dtest=CladeViewFactoryTest`
Expected: PASS — all 11 tests green.

- [ ] **Step 7: Run all library-core tests**

Run: `mvn test -pl domains/library/library-core`
Expected: PASS — existing tests unaffected.

- [ ] **Step 8: Commit**

```bash
git add domains/library/library-core/src/main/java/com/naturalist/library/CladeRanks.java \
       domains/library/library-core/src/main/java/com/naturalist/library/CladeViewFactory.java \
       domains/library/library-core/src/main/java/com/naturalist/library/CladeQueryImpl.java \
       domains/library/library-core/src/test/java/com/naturalist/library/CladeViewFactoryTest.java
git commit -m "feat(library): add CladeRanks, CladeViewFactory, CladeQueryImpl for clade-rank assembly"
```

---

### Task 3: Rank concept content — per-rank teaching pages

**Files:**
- Modify: `domains/library/library-repository-test/src/main/resources/library/concepts.json`

**Interfaces:**
- Consumes: existing `Concept` record (`name`, `title`, `description` with four Durrell levels)
- Produces: 7 new concept entries. Task 4's JTE templates link to `/concepts/{rankSlug}` using these slugs: `kingdom`, `phylum`, `class`, `order`, `family`, `genus`, `species`.

- [ ] **Step 1: Add rank concept entries to concepts.json**

Open `domains/library/library-repository-test/src/main/resources/library/concepts.json`. Append these 7 entries before the closing `]` bracket (after the existing `taxonomy` entry):

```json
  ,
  {
    "name": "kingdom",
    "title": "What is a kingdom?",
    "description": {
      "preschool": "A kingdom is one of the biggest groups of living things. Animals are in one kingdom, plants are in another. It is like having two giant toy bins — one for all the stuffed animals and one for all the building bricks.",
      "elementary": "The kingdom is one of the broadest ranks in taxonomy — just below domain. All animals belong to one kingdom (Animalia), all plants to another (Plantae), all fungi to a third (Fungi). Each kingdom contains thousands of smaller groups nested inside it, each one more specific than the last.",
      "secondary": "A kingdom is a high-level taxonomic rank traditionally used to divide all life into a small number of broad groups: Animalia, Plantae, Fungi, Protista, and the prokaryotic kingdoms. The boundaries have shifted repeatedly as molecular data replaced morphological criteria — for example, fungi were long placed with plants before ribosomal RNA analysis showed they are more closely related to animals. Today, domain (Bacteria, Archaea, Eukarya) sits above kingdom, and many lineages formerly lumped in Protista have been redistributed.",
      "university": "Kingdom is the rank immediately below domain in the Linnaean hierarchy. Its circumscription has been revised from Linnaeus's original two kingdoms (Animalia, Plantae) through Whittaker's five-kingdom system (adding Fungi, Protista, Monera) to the current three-domain framework (Woese 1990), where kingdom-level divisions within Eukarya remain debated — molecular phylogenetics consistently recovers polyphyletic 'Protista,' and no consensus number of eukaryotic kingdoms has stabilised. For metazoan taxonomy the kingdom Animalia is uncontroversial: it is monophyletic, diagnosed by multicellularity with collagen-based extracellular matrix, and governed by the ICZN. Its utility is as a filing coordinate — 'kingdom: Animalia' tells you which code of nomenclature applies and roughly what cell biology to expect, but carries no finer phylogenetic signal than that."
    }
  },
  {
    "name": "phylum",
    "title": "What is a phylum?",
    "description": {
      "preschool": "A phylum is a really big group inside a kingdom. All the animals with a backbone are in one phylum, and all the insects, crabs, and spiders are in another. It is like splitting the big animal bin into a few smaller bins by how their bodies are built.",
      "elementary": "Inside the animal kingdom, animals are sorted into phyla based on their basic body plan. Arthropoda is the phylum for animals with jointed legs and a hard outer shell — that includes insects, spiders, and crabs. Chordata is the phylum for animals with a nerve cord running down their back, which includes fish, birds, and mammals. There are about 35 animal phyla.",
      "secondary": "A phylum is the rank below kingdom that groups organisms by fundamental body plan. Arthropoda, the phylum containing insects, is defined by a segmented body, an exoskeleton of chitin, and paired jointed appendages. Most animal phyla appeared during the Cambrian explosion, roughly 540 million years ago, and the body-plan differences that separate phyla are deeper than those separating classes or orders within a phylum.",
      "university": "Phylum is the highest rank below kingdom, grouping organisms that share a fundamental body plan — a suite of developmental and morphological characters conserved across the clade. In zoology the rank is governed by the ICZN but carries no mandatory name suffix. Most metazoan phyla are recovered as monophyletic by molecular phylogenetics, though internal relationships (e.g. Ecdysozoa vs. Articulata for the placement of arthropods relative to annelids) have been substantially revised by 18S/28S rRNA and phylogenomic data. Arthropoda — the phylum relevant to entomology — is diagnosed by tagmosis, a sclerotized cuticle, and biramous-to-uniramous appendage transformation series."
    }
  },
  {
    "name": "class",
    "title": "What is a class?",
    "description": {
      "preschool": "A class is a group inside a phylum. Insects are a class — all the bugs with six legs and three body parts belong together in the class called Insecta.",
      "elementary": "Inside the arthropod phylum, animals are divided into classes. Insecta is the class for six-legged arthropods — beetles, butterflies, ants, and flies all belong here. Arachnida is the class for eight-legged arthropods like spiders and scorpions. Each class shares a body layout that sets it apart from its cousins.",
      "secondary": "A class is the rank below phylum and above order. Insecta, the class of six-legged arthropods, is diagnosed by a body divided into head, thorax, and abdomen, three pairs of thoracic legs, and (usually) wings. Insecta is by far the most species-rich class of animals, with over a million described species. Classification within Insecta — particularly the relationships among the winged orders — has been reshaped by molecular phylogenetics.",
      "university": "Class is a principal Linnaean rank between phylum and order. Insecta (= Hexapoda sensu stricto in many treatments) is diagnosed by tagmosis into head, thorax, and abdomen, three pairs of thoracic legs, ectognathous mouthparts, and (primitively) two pairs of wings. The rank has no ICZN-mandated suffix. Internal classification follows the Pterygota–Palaeoptera–Neoptera topology, with Holometabola as the major derived clade. Class boundaries are stable for Insecta but contested elsewhere — the number of classes within Arthropoda (Myriapoda monophyletic vs. paraphyletic, Hexapoda nested within Crustacea as Pancrustacea) continues to shift with phylogenomic data."
    }
  },
  {
    "name": "order",
    "title": "What is an order?",
    "description": {
      "preschool": "An order is a group of animals that are alike in a special way. Butterflies and moths are in the order Lepidoptera — they all have tiny scales on their wings that make the colours and patterns you see.",
      "elementary": "An order groups together families that share a big, defining feature. Lepidoptera is the order of butterflies and moths — all the insects whose wings are covered in tiny overlapping scales. Coleoptera is the order of beetles — the ones with hard wing covers. Hymenoptera is the order of bees, wasps, and ants. Each order is a branch of the insect tree of life.",
      "secondary": "An order is the rank below class and above family, grouping families that share a major suite of derived characters. Lepidoptera (butterflies and moths) is defined by scale-covered wings and a coiled proboscis. Orders are often the most practical level of insect identification — a naturalist learns to recognise ordinal characters (wing venation, mouthpart type, metamorphosis pattern) before refining to family or genus.",
      "university": "Order is a principal rank between class and family. Insect orders are diagnosed by wing venation ground plan, mouthpart morphology, and metamorphosis type (holo- vs. hemimetabolous). Ordinal limits in Insecta are mostly stable — Lepidoptera, Coleoptera, Hymenoptera, Diptera are uncontroversially monophyletic — but some traditional orders have been revised: Isoptera was sunk into Blattodea when termites were shown to be social cockroaches, and Homoptera was dissolved when its constituent suborders proved non-monophyletic. The ICZN does not regulate names above family group, so ordinal names carry no mandatory suffix and no type genus."
    }
  },
  {
    "name": "family",
    "title": "What is a family?",
    "description": {
      "preschool": "A family is a group of animals that are close relatives. The swallowtail butterflies all belong to one family because they share the same kind of tail-shaped wings and bright warning colours.",
      "elementary": "A family is a group of genera that are closely related. Papilionidae is the swallowtail family — all the big, boldly coloured butterflies with tail-like extensions on their hindwings. Family names for animals always end in '-idae.' Inside a family, there are genera, and inside each genus, species.",
      "secondary": "A family is the rank below order and above genus. In zoology, family names are governed by the ICZN and must end in '-idae,' anchored to a type genus. Papilionidae, the swallowtail family, is diagnosed by the forked osmeterium (a defensive gland unique to its larvae) and characteristic wing venation. Families are often the level at which field guides are organised — learning to recognise family-level characters is a key step in naturalist training.",
      "university": "Family is a rank in the family group (superfamily, family, subfamily, tribe, subtribe) governed by the ICZN, carrying the mandatory suffix -idae and anchored to a type genus whose valid name fixes the family name. A family is circumscribed by a set of synapomorphies diagnostic at that rank — for Papilionidae these include the larval osmeterium, a single anal vein in the forewing, and the absence of a humeral vein in the hindwing. Family-level stability varies: some insect families (Papilionidae, Scarabaeidae) have been recognised since the early 19th century, while others are regularly split or merged as molecular data revise generic relationships."
    }
  },
  {
    "name": "genus",
    "title": "What is a genus?",
    "description": {
      "preschool": "A genus is a small group of animals that are very close relatives — almost like brothers and sisters. The genus Battus holds a few kinds of swallowtail butterfly that look and act very much alike.",
      "elementary": "A genus is a group of species that are each other's closest relatives. The genus name is the first word in a species' two-part scientific name. Battus is a genus of swallowtail butterflies — Battus philenor (the pipevine swallowtail) and Battus polydamas (the gold rim swallowtail) are two species in that genus. They share enough features that scientists group them together.",
      "secondary": "A genus is the rank below family and above species, and it supplies the first word of the binomial name. A genus should be monophyletic — a clade — and its member species should share a set of diagnostic characters that distinguish them from species in sibling genera. In practice, generic limits are among the most frequently revised ranks in taxonomy, because what 'counts' as genus-level divergence is partly a judgment call. Splitting a genus changes every species' binomial, which is why genus revisions are both scientifically important and nomenclaturally disruptive.",
      "university": "Genus is the rank that supplies the first element of the binomial and is the operative unit of Linnaean nomenclature: the specific epithet has no standing without it. Under the ICZN a genus is anchored to a type species, which fixes the name's application when the genus is split or merged. Generic monophyly is expected but not enforced by the code — paraphyletic genera persist where no formal revision has been published. The genus Drosophila illustrates the tension: molecular phylogenetics shows it to be massively paraphyletic, and a strict split would reassign D. melanogaster to Sophophora, disrupting one of biology's most familiar binomials. Priority and typification rules arbitrate such cases, but the community's tolerance for nomenclatural upheaval often delays formal action."
    }
  },
  {
    "name": "species",
    "title": "What is a species?",
    "description": {
      "preschool": "A species is one exact kind of animal. Every pipevine swallowtail butterfly is the same species — they all look alike, live in similar places, and their babies grow up to be pipevine swallowtails too.",
      "elementary": "A species is the most specific rank — one particular kind of living thing. The pipevine swallowtail, Battus philenor, is one species. Its scientific name has two parts: Battus (the genus, its group of close relatives) and philenor (which picks out this one kind). Members of the same species can breed with each other and produce healthy offspring.",
      "secondary": "A species is the fundamental unit of biodiversity — a population or group of populations of organisms that share a recent common ancestor and are reproductively or genetically cohesive. In practice, entomologists diagnose species using morphological characters (wing pattern, genitalic structure), DNA barcoding (the mitochondrial COI gene), and ecological or behavioural differences. Cryptic species — populations that look identical but are genetically distinct — are increasingly being discovered through molecular work, especially in insects.",
      "university": "Species is the base rank of the Linnaean hierarchy and the only rank with an operational (if contested) biological definition. The biological species concept (Mayr 1942) defines species by reproductive isolation; the phylogenetic species concept defines them as the smallest diagnosable monophyletic cluster; the unified species concept (de Queiroz 2007) treats speciation as a process and species as independently evolving metapopulation lineages, with various criteria (reproductive isolation, diagnosability, ecological distinctness) serving as lines of evidence rather than definitions. In entomology, integrative taxonomy combining morphology, molecular barcoding (typically COI), ecology, and behaviour is standard practice for species delimitation. The ICZN governs species-group names — each anchored to a type specimen — and arbitrates priority, homonymy, and synonymy when species are recombined across genera."
    }
  }
```

- [ ] **Step 2: Run existing concept tests to confirm data loads correctly**

Run: `mvn test -pl domains/library/library-core -Dtest=ConceptQueryImplTest`
Expected: PASS — existing tests still work with the expanded dataset.

- [ ] **Step 3: Commit**

```bash
git add domains/library/library-repository-test/src/main/resources/library/concepts.json
git commit -m "content(library): add per-rank concept teaching pages (kingdom through species)"
```

---

### Task 4: Controller rewrite, templates, CSS — wiring the dual-strategy breadcrumb

**Files:**
- Modify: `domains/library/library-test-context/src/main/java/com/naturalist/library/LibraryTestContext.java`
- Modify: `domains/library/library-console/src/main/java/com/naturalist/library/console/clade/CladesController.java`
- Modify: `domains/library/library-console/src/main/jte/clades/detail.jte`
- Modify: `domains/library/library-console/src/main/jte/clades/tree.jte`
- Modify: `domains/library/library-console/src/main/jte/clades/node.jte`
- Modify: `apps/management-console/src/main/resources/static/css/naturalist.css`

**Interfaces:**
- Consumes: `CladeQuery`, `CladeView`, `CladeStep`, `CladeQuery.CladeTreeNode` (from Tasks 1–2); `LinealRank` (from taxonomy kernel); `LibraryTestContext` wired `CladeQuery`; rank concept slugs from Task 3.
- Produces: rendered dual-row breadcrumb on `/clades` and `/clades/{slug}`.

- [ ] **Step 1: Wire CladeQuery into LibraryTestContext**

In `domains/library/library-test-context/src/main/java/com/naturalist/library/LibraryTestContext.java`, add the `CladeQuery` field and accessor:

Add field alongside existing ones:

```java
    private final CladeQuery cladeQuery;
```

Add to the constructor body after the `citationAssociationQuery` line:

```java
        this.cladeQuery = new CladeQueryImpl();
```

Add accessor method after `citationAssociationQuery()`:

```java
    public CladeQuery cladeQuery() {
        return cladeQuery;
    }
```

- [ ] **Step 2: Rewrite CladesController**

Replace the entire contents of `domains/library/library-console/src/main/java/com/naturalist/library/console/clade/CladesController.java`:

```java
package com.naturalist.library.console.clade;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.fieldnotes.render.DescriptionRenderer;
import com.naturalist.library.CladeQuery;
import com.naturalist.library.CladeView;
import com.naturalist.library.LibraryTestContext;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Controller
public class CladesController {

    private final CladeQuery cladeQuery;
    private final DescriptionRenderer descriptionRenderer = new DescriptionRenderer(List.of());

    CladesController() {
        // TODO:: This will eventually be a spring managed bean
        this.cladeQuery = LibraryTestContext.create(NaturalistDatabase.create()).cladeQuery();
    }

    @GetMapping("/clades")
    String tree(Model model) {
        model.addAttribute("root", cladeQuery.tree());
        var eukaryota = cladeQuery.getBySlug("eukaryota");
        eukaryota.ifPresent(view -> model.addAttribute("rootView", view));
        return "clades/tree";
    }

    @GetMapping("/clades/{slug}")
    String detail(@PathVariable String slug, Model model) {
        var view = cladeQuery.getBySlug(slug);
        if (view.isEmpty()) {
            return "redirect:/clades";
        }
        CladeView v = view.get();
        var description = findCladeDescription(slug);
        model.addAttribute("view", v);
        model.addAttribute("descriptionPreschool", description[0]);
        model.addAttribute("descriptionElementary", description[1]);
        model.addAttribute("descriptionSecondary", description[2]);
        model.addAttribute("descriptionUniversity", description[3]);
        return "clades/detail";
    }

    /**
     * Resolves the four-level description for a clade by its slug.
     * Clade descriptions live on the Clade sealed permits in the clades kernel,
     * accessed indirectly through the kernel's static API.
     */
    private String[] findCladeDescription(String slug) {
        try {
            var clade = com.naturalist.clades.Clade.of(slug);
            var description = clade.description();
            return new String[]{
                    descriptionRenderer.render(description.preschool()),
                    descriptionRenderer.render(description.elementary()),
                    descriptionRenderer.render(description.secondary()),
                    descriptionRenderer.render(description.university())
            };
        } catch (IllegalArgumentException e) {
            return new String[]{"", "", "", ""};
        }
    }
}
```

**Note:** The controller still needs `Clade.of(slug)` for description rendering because `CladeView` deliberately does not carry the full description text (it carries navigation data, not page content). This is a single remaining reference to the `clades` kernel — acceptable because the description is rendering data, not assembly logic. The `clades` dependency stays in `library-console/pom.xml`.

- [ ] **Step 3: Rewrite detail.jte for dual-row breadcrumb**

Replace the entire contents of `domains/library/library-console/src/main/jte/clades/detail.jte`:

```jte
@import com.naturalist.library.CladeStep
@import com.naturalist.library.CladeView

@param CladeView view
@param String descriptionPreschool = ""
@param String descriptionElementary = ""
@param String descriptionSecondary = ""
@param String descriptionUniversity = ""

@template.layout.page(title = view.subject().displayName(), content = @`
    <nav class="dual-breadcrumb" aria-label="Clade navigation">
        <%-- Row labels --%>
        <span class="dual-breadcrumb-clade"><a href="/concepts/clade">Phylogenetic lineage</a></span>
        <span class="dual-breadcrumb-rank"><a href="/concepts/taxonomic-rank">Linnaean rank ladder</a></span>

        @for(var step : view.ancestry())
            <%-- Separator column --%>
            <span class="dual-breadcrumb-clade dual-breadcrumb-sep" aria-hidden="true">›</span>
            <span class="dual-breadcrumb-rank dual-breadcrumb-spacer"></span>

            <%-- Data column --%>
            <span class="dual-breadcrumb-clade"><a href="/clades/${step.cladeSlug()}">${step.displayName()}</a></span>
            @if(step.rank().isPresent())
                !{var r = step.rank().get().name();}
                <span class="dual-breadcrumb-rank"><a href="/concepts/${r.toLowerCase()}">${r.substring(0, 1)}${r.substring(1).toLowerCase()}</a></span>
            @else
                <span class="dual-breadcrumb-rank dual-breadcrumb-gap" aria-label="no rank">—</span>
            @endif
        @endfor

        <%-- Subject separator --%>
        <span class="dual-breadcrumb-clade dual-breadcrumb-sep" aria-hidden="true">›</span>
        <span class="dual-breadcrumb-rank dual-breadcrumb-spacer"></span>

        <%-- Subject column --%>
        <span class="dual-breadcrumb-clade dual-breadcrumb-current"><strong>${view.subject().displayName()}</strong></span>
        @if(view.subject().rank().isPresent())
            !{var sr = view.subject().rank().get().name();}
            <span class="dual-breadcrumb-rank"><a href="/concepts/${sr.toLowerCase()}">${sr.substring(0, 1)}${sr.substring(1).toLowerCase()}</a></span>
        @else
            <span class="dual-breadcrumb-rank dual-breadcrumb-gap" aria-label="no rank">—</span>
        @endif
    </nav>

    <h1>${view.subject().displayName()}</h1>

    <p class="clade-concept-links">
        <a href="/concepts/clade">What is a clade?</a> ·
        <a href="/concepts/clade-taxonomy-relation">Clades vs. taxonomy</a>
    </p>

    @template.components.description(
        preschool = descriptionPreschool,
        elementary = descriptionElementary,
        secondary = descriptionSecondary,
        university = descriptionUniversity)

    @if(!view.children().isEmpty())
        <section>
            <h2>Direct descendants</h2>
            <ul class="clade-children">
                @for(var child : view.children())
                    <li><a href="/clades/${child.cladeSlug()}">${child.displayName()}</a></li>
                @endfor
            </ul>
        </section>
    @endif
`)
```

- [ ] **Step 4: Rewrite tree.jte**

Replace the entire contents of `domains/library/library-console/src/main/jte/clades/tree.jte`:

```jte
@import com.naturalist.library.CladeQuery.CladeTreeNode
@import com.naturalist.library.CladeView

@param CladeTreeNode root
@param CladeView rootView = null

@template.layout.page(title = "Tree of Life", content = @`
    @if(rootView != null)
        <nav class="dual-breadcrumb" aria-label="Clade navigation">
            <span class="dual-breadcrumb-clade"><a href="/concepts/clade">Phylogenetic lineage</a></span>
            <span class="dual-breadcrumb-rank"><a href="/concepts/taxonomic-rank">Linnaean rank ladder</a></span>

            <span class="dual-breadcrumb-clade dual-breadcrumb-sep" aria-hidden="true">›</span>
            <span class="dual-breadcrumb-rank dual-breadcrumb-spacer"></span>

            <span class="dual-breadcrumb-clade dual-breadcrumb-current"><strong>${rootView.subject().displayName()}</strong></span>
            <span class="dual-breadcrumb-rank dual-breadcrumb-gap" aria-label="no rank">—</span>
        </nav>
    @endif

    <h1>Tree of Life</h1>
    <p>Every clade in the catalog, from Eukaryota down.
       <a href="/concepts/clade">What is a clade?</a></p>
    <ul class="clade-tree">
        @template.clades.node(node = root)
    </ul>
`)
```

- [ ] **Step 5: Rewrite node.jte**

Replace the entire contents of `domains/library/library-console/src/main/jte/clades/node.jte`:

```jte
@import com.naturalist.library.CladeQuery.CladeTreeNode
@param CladeTreeNode node

<li>
    <a href="/clades/${node.slug()}">${node.displayName()}</a>
    @if(!node.children().isEmpty())
        <ul>
            @for(var child : node.children())
                @template.clades.node(node = child)
            @endfor
        </ul>
    @endif
</li>
```

- [ ] **Step 6: Add dual-breadcrumb CSS**

In `apps/management-console/src/main/resources/static/css/naturalist.css`, add the following block **before** the existing `/* Taxonomic breadcrumb` comment (line 1107):

```css
/* Dual-strategy breadcrumb — two-row CSS Grid. Items are emitted in
   strict (clade-row, rank-row) pairs, and grid-auto-flow: column places
   each pair in the same column. Row 1 = phylogenetic lineage (clade names),
   row 2 = Linnaean rank ladder (rank names or em-dash gaps). */
.dual-breadcrumb {
    display: grid;
    grid-template-rows: auto auto;
    grid-auto-flow: column;
    grid-auto-columns: auto;
    column-gap: 0.5rem;
    row-gap: 0.15rem;
    align-items: baseline;
    font-size: 0.9rem;
    font-style: italic;
    color: var(--sepia-ink-soft);
    padding: 0.4rem 0;
    margin: 0 0 0.75rem;
    line-height: 1.3;
    overflow-x: auto;
}

.dual-breadcrumb-clade {
    grid-row: 1;
    white-space: nowrap;
}

.dual-breadcrumb-rank {
    grid-row: 2;
    white-space: nowrap;
    font-size: 0.75rem;
    font-style: normal;
    font-weight: 600;
    letter-spacing: 0.05em;
    color: rgba(58, 42, 24, 0.55);
}

.dual-breadcrumb-sep {
    color: rgba(58, 42, 24, 0.35);
}

.dual-breadcrumb-spacer {
    /* Invisible placeholder keeping rank row aligned under separators. */
}

.dual-breadcrumb-gap {
    color: rgba(58, 42, 24, 0.3);
    font-style: italic;
}

.dual-breadcrumb a {
    color: var(--sepia-ink-soft);
    text-decoration: none;
    border-bottom: 1px dotted var(--sepia-rule);
}

.dual-breadcrumb a:hover {
    color: var(--pico-primary);
    border-bottom-color: var(--pico-primary);
}

.dual-breadcrumb-rank a {
    color: rgba(58, 42, 24, 0.55);
}

.dual-breadcrumb-rank a:hover {
    color: var(--pico-primary);
}

.dual-breadcrumb-current strong {
    color: var(--sepia-ink);
    font-weight: 700;
    font-style: normal;
}

```

- [ ] **Step 7: Verify the app compiles and loads**

Run: `mvn compile -pl domains/library/library-console,domains/library/library-test-context`
Expected: compiles without errors.

- [ ] **Step 8: Manual smoke test**

Start the management console and verify:

1. Navigate to `/clades/lepidoptera` — confirm both rows render:
   - Clade row: Phylogenetic lineage › Eukaryota › Animalia › Arthropoda › Insecta › Holometabola › **Lepidoptera**
   - Rank row: Linnaean rank ladder · — · Kingdom · Phylum · Class · — · Order
2. Confirm Holometabola shows "—" in the rank row (rank-less clade).
3. Click "Kingdom" in the rank row → arrives at `/concepts/kingdom`.
4. Click "Animalia" in the clade row → arrives at `/clades/animalia`.
5. Navigate to `/clades` — tree page renders with Eukaryota dual-breadcrumb.
6. Navigate to `/clades/eukaryota` — ancestry is empty, rank row shows "—".

- [ ] **Step 9: Commit**

```bash
git add domains/library/library-test-context/src/main/java/com/naturalist/library/LibraryTestContext.java \
       domains/library/library-console/src/main/java/com/naturalist/library/console/clade/CladesController.java \
       domains/library/library-console/src/main/jte/clades/detail.jte \
       domains/library/library-console/src/main/jte/clades/tree.jte \
       domains/library/library-console/src/main/jte/clades/node.jte \
       apps/management-console/src/main/resources/static/css/naturalist.css
git commit -m "feat(library): dual-strategy breadcrumb on clade pages, thin CladesController"
```

