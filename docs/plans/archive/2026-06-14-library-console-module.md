# Library Console Module — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extract `CladesController` and `ConceptsController` from the management-console app into a new `library-console` domain contribution module, following the established `*-console` pattern (reference: `insects-console`).

**Architecture:** The library domain becomes the umbrella for reference-browsing UI — both concepts (library-api entities) and clades (kernel). The new `library-console` module contributes controllers, JTE templates, a `LibraryDataConfiguration`, and a `LibraryLinker` for catalog cross-references. The management-console app replaces its direct library-api/core/test-context dependencies with a single `library-console` dependency.

**Tech Stack:** Java 21, Spring MVC, JTE templates, Maven module system

---

## File Map

### New files (library-console module)

| File | Responsibility |
|------|---------------|
| `domains/library/library-console/pom.xml` | Module descriptor — depends on catalog, clades, library-api, spring-web, spring-context, library-test-context (TEMPORARY) |
| `.../library/console/clade/CladesController.java` | Moved from management-console, repackaged to `com.naturalist.library.console.clade` |
| `.../library/console/concept/ConceptsController.java` | Moved from management-console, repackaged to `com.naturalist.library.console.concept` |
| `.../library/console/LibraryDataConfiguration.java` | Spring `@Configuration` — exposes `ConceptTestEntitySource` and `CitationTestEntitySource` as beans |
| `.../library/console/catalog/LibraryLinker.java` | `@DomainService` `EntityRefLinker` — maps `ConceptName` to `/concepts/{slug}` |
| `src/main/jte/.jteroot` | Empty marker file |
| `src/main/jte/clades/tree.jte` | Moved from management-console, import updated |
| `src/main/jte/clades/detail.jte` | Moved from management-console, import updated |
| `src/main/jte/clades/node.jte` | Moved from management-console, import updated |
| `src/main/jte/concepts/list.jte` | Moved from management-console (unchanged — already imports from `com.naturalist.library`) |
| `src/main/jte/concepts/detail.jte` | Moved from management-console (unchanged) |

### Modified files

| File | Change |
|------|--------|
| `domains/library/pom.xml` | Add `<module>library-console</module>` |
| `pom.xml` (root) | Add `library-console` to `<dependencyManagement>` in LIBRARY section |
| `apps/management-console/pom.xml` | Add `library-console` dep; remove `library-api`, `library-core`, `library-test-context` |

### Deleted files

| File | Reason |
|------|--------|
| `apps/management-console/.../console/clade/CladesController.java` | Moved to library-console |
| `apps/management-console/.../console/concept/ConceptsController.java` | Moved to library-console |
| `apps/management-console/src/main/jte/clades/tree.jte` | Moved to library-console |
| `apps/management-console/src/main/jte/clades/detail.jte` | Moved to library-console |
| `apps/management-console/src/main/jte/clades/node.jte` | Moved to library-console |
| `apps/management-console/src/main/jte/concepts/list.jte` | Moved to library-console |
| `apps/management-console/src/main/jte/concepts/detail.jte` | Moved to library-console |

---

### Task 1: Create library-console module skeleton

**Files:**
- Create: `domains/library/library-console/pom.xml`
- Modify: `domains/library/pom.xml`
- Modify: `pom.xml` (root dependencyManagement)

- [ ] **Step 1: Create `domains/library/library-console/pom.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xmlns="http://maven.apache.org/POM/4.0.0"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.naturalist</groupId>
        <artifactId>library</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>library-console</artifactId>

    <name>library :: console</name>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring-boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <dependencies>
        <dependency>
            <groupId>com.naturalist</groupId>
            <artifactId>catalog</artifactId>
        </dependency>
        <dependency>
            <groupId>com.naturalist</groupId>
            <artifactId>clades</artifactId>
        </dependency>
        <dependency>
            <groupId>com.naturalist</groupId>
            <artifactId>library-api</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-context</artifactId>
        </dependency>

        <!-- TEMPORARY -->
        <dependency>
            <groupId>com.naturalist</groupId>
            <artifactId>library-test-context</artifactId>
        </dependency>

        <!-- TEST -->
        <dependency>
            <groupId>gg.jte</groupId>
            <artifactId>jte</artifactId>
            <version>${jte.version}</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <parameters>true</parameters>
                </configuration>
            </plugin>
        </plugins>
    </build>

</project>
```

- [ ] **Step 2: Add module to `domains/library/pom.xml`**

Add `<module>library-console</module>` after `library-test-context`:

```xml
    <modules>
        <module>library-api</module>
        <module>library-core</module>
        <module>library-repository-test</module>
        <module>library-test-context</module>
        <module>library-console</module>
    </modules>
```

- [ ] **Step 3: Add to root `pom.xml` dependencyManagement**

Insert after the `library-test-context` entry (line 307) in the LIBRARY section:

```xml
            <dependency>
                <groupId>com.naturalist</groupId>
                <artifactId>library-console</artifactId>
                <version>${project.version}</version>
            </dependency>
```

- [ ] **Step 4: Create source directory**

```bash
mkdir -p domains/library/library-console/src/main/java/com/naturalist/library/console/clade
mkdir -p domains/library/library-console/src/main/java/com/naturalist/library/console/concept
mkdir -p domains/library/library-console/src/main/java/com/naturalist/library/console/catalog
mkdir -p domains/library/library-console/src/main/jte/clades
mkdir -p domains/library/library-console/src/main/jte/concepts
```

- [ ] **Step 5: Create JTE root marker**

Create empty file: `domains/library/library-console/src/main/jte/.jteroot`

---

### Task 2: Move controllers to library-console

**Files:**
- Create: `domains/library/library-console/src/main/java/com/naturalist/library/console/clade/CladesController.java`
- Create: `domains/library/library-console/src/main/java/com/naturalist/library/console/concept/ConceptsController.java`
- Delete: `apps/management-console/src/main/java/com/naturalist/console/clade/CladesController.java`
- Delete: `apps/management-console/src/main/java/com/naturalist/console/concept/ConceptsController.java`

- [ ] **Step 1: Create `CladesController.java` in library-console**

Only change from original: package declaration `com.naturalist.console.clade` → `com.naturalist.library.console.clade`.

```java
package com.naturalist.library.console.clade;

import com.naturalist.clades.Clade;
import com.naturalist.clades.CladeCatalog;
import com.naturalist.clades.CladeTraversal;
import com.naturalist.clades.Eukaryota;
import com.naturalist.fieldnotes.render.DescriptionRenderer;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Controller
public class CladesController {

    private final DescriptionRenderer descriptionRenderer = new DescriptionRenderer(List.of());

    @GetMapping("/clades")
    String tree(Model model) {
        model.addAttribute("root", toNode(new Eukaryota()));
        return "clades/tree";
    }

    @GetMapping("/clades/{slug}")
    String detail(@PathVariable String slug, Model model) {
        Clade clade;
        try {
            clade = Clade.of(slug);
        } catch (IllegalArgumentException e) {
            return "redirect:/clades";
        }
        var description = clade.description();
        model.addAttribute("displayName", clade.displayName());
        model.addAttribute("slug", clade.slug());
        model.addAttribute("ancestry", ancestryLinks(clade));
        model.addAttribute("children", childLinks(clade));
        model.addAttribute("descriptionPreschool", descriptionRenderer.render(description.preschool()));
        model.addAttribute("descriptionElementary", descriptionRenderer.render(description.elementary()));
        model.addAttribute("descriptionSecondary", descriptionRenderer.render(description.secondary()));
        model.addAttribute("descriptionUniversity", descriptionRenderer.render(description.university()));
        return "clades/detail";
    }

    private CladeNode toNode(Clade clade) {
        List<CladeNode> children = CladeCatalog.childrenOf(clade).stream()
                .map(this::toNode)
                .toList();
        return new CladeNode(clade.slug(), clade.displayName(), children);
    }

    /** Ancestors from root down to the clade's parent (the clade itself excluded). */
    private List<CladeLink> ancestryLinks(Clade clade) {
        List<Clade> chain = new ArrayList<>(CladeTraversal.ancestry(clade));
        chain.removeFirst();              // drop the clade itself
        Collections.reverse(chain);       // root → parent order
        return chain.stream().map(c -> new CladeLink(c.slug(), c.displayName())).toList();
    }

    private List<CladeLink> childLinks(Clade clade) {
        return CladeCatalog.childrenOf(clade).stream()
                .map(c -> new CladeLink(c.slug(), c.displayName()))
                .toList();
    }

    public record CladeNode(String slug, String displayName, List<CladeNode> children) {
    }

    public record CladeLink(String slug, String displayName) {
    }
}
```

- [ ] **Step 2: Create `ConceptsController.java` in library-console**

Only change from original: package declaration `com.naturalist.console.concept` → `com.naturalist.library.console.concept`.

```java
package com.naturalist.library.console.concept;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.PageRequest;
import com.naturalist.fieldnotes.render.DescriptionRenderer;
import com.naturalist.library.Concept;
import com.naturalist.library.ConceptName;
import com.naturalist.library.ConceptQuery;
import com.naturalist.library.LibraryTestContext;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Comparator;
import java.util.List;

@Controller
class ConceptsController {

    private final ConceptQuery conceptQuery;
    private final DescriptionRenderer descriptionRenderer = new DescriptionRenderer(List.of());

    ConceptsController() {
        // TODO:: This will eventually be a spring managed bean
        this.conceptQuery = LibraryTestContext.create(NaturalistDatabase.create()).conceptQuery();
    }

    @GetMapping("/concepts")
    String list(Model model) {
        List<Concept> concepts = conceptQuery
                .findPage(PageRequest.first(PageRequest.MAX_PAGE_SIZE))
                .content().stream()
                .sorted(Comparator.comparing(Concept::title))
                .toList();
        model.addAttribute("concepts", concepts);
        return "concepts/list";
    }

    @GetMapping("/concepts/{slug}")
    String detail(@PathVariable String slug, Model model) {
        var concept = conceptQuery.getByName(ConceptName.of(slug));
        if (concept.isEmpty()) {
            return "redirect:/concepts";
        }
        Concept c = concept.get();
        var description = c.description();
        model.addAttribute("concept", c);
        model.addAttribute("descriptionPreschool", descriptionRenderer.render(description.preschool()));
        model.addAttribute("descriptionElementary", descriptionRenderer.render(description.elementary()));
        model.addAttribute("descriptionSecondary", descriptionRenderer.render(description.secondary()));
        model.addAttribute("descriptionUniversity", descriptionRenderer.render(description.university()));
        return "concepts/detail";
    }
}
```

- [ ] **Step 3: Delete original controllers**

```bash
rm apps/management-console/src/main/java/com/naturalist/console/clade/CladesController.java
rmdir apps/management-console/src/main/java/com/naturalist/console/clade
rm apps/management-console/src/main/java/com/naturalist/console/concept/ConceptsController.java
rmdir apps/management-console/src/main/java/com/naturalist/console/concept
```

---

### Task 3: Move JTE templates to library-console

**Files:**
- Create: `domains/library/library-console/src/main/jte/clades/tree.jte`
- Create: `domains/library/library-console/src/main/jte/clades/detail.jte`
- Create: `domains/library/library-console/src/main/jte/clades/node.jte`
- Create: `domains/library/library-console/src/main/jte/concepts/list.jte`
- Create: `domains/library/library-console/src/main/jte/concepts/detail.jte`
- Delete: `apps/management-console/src/main/jte/clades/` (3 files)
- Delete: `apps/management-console/src/main/jte/concepts/` (2 files)

- [ ] **Step 1: Create clades templates with updated import**

`tree.jte` — import changes from `com.naturalist.console.clade` to `com.naturalist.library.console.clade`:

```jte
@import com.naturalist.library.console.clade.CladesController.CladeNode
@param CladeNode root

@template.layout.page(title = "Tree of Life", content = @`
    <h1>Tree of Life</h1>
    <p>Every clade in the catalog, from Eukaryota down.
       <a href="/concepts/clade">What is a clade?</a></p>
    <ul class="clade-tree">
        @template.clades.node(node = root)
    </ul>
`)
```

`detail.jte` — same import update:

```jte
@import com.naturalist.library.console.clade.CladesController.CladeLink
@import java.util.List

@param String displayName
@param String slug
@param List<CladeLink> ancestry = List.of()
@param List<CladeLink> children = List.of()
@param String descriptionPreschool = ""
@param String descriptionElementary = ""
@param String descriptionSecondary = ""
@param String descriptionUniversity = ""

@template.layout.page(title = displayName, content = @`
    <nav class="breadcrumb-trail" aria-label="Clade ancestry">
        <span class="breadcrumb-segment"><a href="/clades">Tree of Life</a></span>
        @for(var ancestor : ancestry)
            <span class="breadcrumb-sep" aria-hidden="true">›</span>
            <span class="breadcrumb-segment"><a href="/clades/${ancestor.slug()}">${ancestor.displayName()}</a></span>
        @endfor
        <span class="breadcrumb-sep" aria-hidden="true">›</span>
        <span class="breadcrumb-segment breadcrumb-current"><strong>${displayName}</strong></span>
    </nav>

    <h1>${displayName}</h1>

    <p class="clade-concept-links">
        <a href="/concepts/clade">What is a clade?</a> ·
        <a href="/concepts/clade-taxonomy-relation">Clades vs. taxonomy</a>
    </p>

    @template.components.description(
        preschool = descriptionPreschool,
        elementary = descriptionElementary,
        secondary = descriptionSecondary,
        university = descriptionUniversity)

    @if(!children.isEmpty())
        <section>
            <h2>Direct descendants</h2>
            <ul class="clade-children">
                @for(var child : children)
                    <li><a href="/clades/${child.slug()}">${child.displayName()}</a></li>
                @endfor
            </ul>
        </section>
    @endif
`)
```

`node.jte` — same import update:

```jte
@import com.naturalist.library.console.clade.CladesController.CladeNode
@param CladeNode node

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

- [ ] **Step 2: Create concepts templates (unchanged)**

`list.jte` — no import changes needed (already imports from `com.naturalist.library`):

```jte
@import com.naturalist.library.Concept
@import java.util.List
@param List<Concept> concepts

@template.layout.page(title = "Concepts", content = @`
    <h1>Concepts</h1>
    <p>Teaching notes on how the catalog of life is organised.</p>
    <ul class="concept-list">
        @for(var concept : concepts)
            <li><a href="/concepts/${concept.name().value()}">${concept.title()}</a></li>
        @endfor
    </ul>
    <p><a href="/clades">Browse the tree of life →</a></p>
`)
```

`detail.jte` — no import changes needed:

```jte
@import com.naturalist.library.Concept

@param Concept concept
@param String descriptionPreschool = ""
@param String descriptionElementary = ""
@param String descriptionSecondary = ""
@param String descriptionUniversity = ""

@template.layout.page(title = concept.title(), content = @`
    <nav class="breadcrumb-trail" aria-label="Breadcrumb">
        <span class="breadcrumb-segment"><a href="/concepts">Concepts</a></span>
        <span class="breadcrumb-sep" aria-hidden="true">›</span>
        <span class="breadcrumb-segment breadcrumb-current"><strong>${concept.title()}</strong></span>
    </nav>
    <h1>${concept.title()}</h1>
    @template.components.description(
        preschool = descriptionPreschool,
        elementary = descriptionElementary,
        secondary = descriptionSecondary,
        university = descriptionUniversity)
    <p><a href="/clades">Browse the tree of life →</a></p>
`)
```

- [ ] **Step 3: Delete original templates**

```bash
rm -r apps/management-console/src/main/jte/clades
rm -r apps/management-console/src/main/jte/concepts
```

---

### Task 4: Create LibraryDataConfiguration and LibraryLinker

**Files:**
- Create: `domains/library/library-console/src/main/java/com/naturalist/library/console/LibraryDataConfiguration.java`
- Create: `domains/library/library-console/src/main/java/com/naturalist/library/console/catalog/LibraryLinker.java`

- [ ] **Step 1: Create `LibraryDataConfiguration.java`**

```java
package com.naturalist.library.console;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.library.CitationTestEntitySource;
import com.naturalist.library.ConceptTestEntitySource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LibraryDataConfiguration {

    @Bean
    ConceptTestEntitySource conceptSource(NaturalistDatabase database) {
        return new ConceptTestEntitySource(database);
    }

    @Bean
    CitationTestEntitySource citationSource(NaturalistDatabase database) {
        return new CitationTestEntitySource(database);
    }
}
```

- [ ] **Step 2: Create `LibraryLinker.java`**

```java
package com.naturalist.library.console.catalog;

import com.naturalist.catalog.EntityRef;
import com.naturalist.catalog.EntityRefLinker;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.library.ConceptName;

/**
 * Library-domain {@link EntityRefLinker}: maps library-owned
 * {@code EntityName} types to detail-page URLs. The single place to look
 * when adding a new library entity or moving an existing one.
 */
@DomainService
public class LibraryLinker implements EntityRefLinker {

    @Override
    public String linkFor(EntityRef ref) {
        return switch (ref.name()) {
            case ConceptName n -> "/concepts/" + n.value();
            default -> null;
        };
    }
}
```

---

### Task 5: Update management-console pom.xml

**Files:**
- Modify: `apps/management-console/pom.xml`

- [ ] **Step 1: Replace library deps with library-console**

In `apps/management-console/pom.xml`, replace the "DOMAIN MODULES (no console module)" section (lines 76-88):

```xml
        <!-- DOMAIN MODULES (no console module) -->
        <dependency>
            <groupId>com.naturalist</groupId>
            <artifactId>library-api</artifactId>
        </dependency>
        <dependency>
            <groupId>com.naturalist</groupId>
            <artifactId>library-core</artifactId>
        </dependency>
        <dependency>
            <groupId>com.naturalist</groupId>
            <artifactId>library-test-context</artifactId>
        </dependency>
```

with a new entry in the DOMAIN CONSOLE MODULES section:

```xml
        <!-- DOMAIN CONSOLE MODULES -->
        <dependency>
            <groupId>com.naturalist</groupId>
            <artifactId>chemistry-console</artifactId>
        </dependency>
        <dependency>
            <groupId>com.naturalist</groupId>
            <artifactId>insects-console</artifactId>
        </dependency>
        <dependency>
            <groupId>com.naturalist</groupId>
            <artifactId>library-console</artifactId>
        </dependency>
        <dependency>
            <groupId>com.naturalist</groupId>
            <artifactId>plants-console</artifactId>
        </dependency>
```

- [ ] **Step 2: Commit**

```bash
git add domains/library/library-console/ domains/library/pom.xml pom.xml apps/management-console/
git commit -m "refactor(console): extract library-console module from management-console

Move CladesController, ConceptsController, and their JTE templates into
domains/library/library-console following the established *-console pattern.
Add LibraryDataConfiguration and LibraryLinker for catalog integration."
```

---

### Task 6: Verify build

- [ ] **Step 1: Run the build**

```bash
mvn verify
```

Expected: clean build. Watch for:
- JTE template compilation errors (import path changes in clades templates)
- Missing dependency errors (clades kernel not resolved)
- Spring component scan issues (new package `com.naturalist.library.console` must be picked up by `@ComponentScan(basePackages = "com.naturalist")`)
