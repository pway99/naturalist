# Soil Console (facts viewer) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:subagent-driven-development (recommended)
> or superpowers:executing-plans. Steps use checkbox (`- [ ]`) syntax.

**Goal:** Surface the assembled `SoilProfile` in the management console — a read-only viewer to
browse the Oak Vista profiles and see each dated analysis's nutrient panel + physical
characteristics, validating the Slice 1 model against a real Spring/JTE consumer.

**Architecture:** Two new Maven modules under `domains/soil`: `soil-test-context` (a public
`SoilTestContext` wiring seam, promoted from the existing `SoilsTestContextInternal`) and
`soil-console` (a Spring MVC `@Controller` + JTE templates). The `apps/management-console` app
auto-discovers both via `scanBasePackages="com.naturalist"` + its JTE source-tree scan — the only
app-side edits are one Maven dependency and one nav `<li>`. Facts only; no status/color (crop slice).

**Tech Stack:** Java 25, Spring Web MVC, JTE (`gg.jte`) templates resolved from source dirs, JUnit 5
+ AssertJ, Maven multi-module. Design: `docs/plans/2026-08-09-soil-console-design.md`.

## Global Constraints

- **Discovery is automatic.** `ConsoleApplication` uses `@SpringBootApplication(scanBasePackages =
  "com.naturalist")`; `JteConfiguration` scans every `domains/*/src/main/jte`. No `@Import`, no
  `.jteroot`, no `JteConfiguration` edit. New module + templates + one app dependency = wired.
- **Never add `spring-security-web` (or import `CsrfToken`) to `soil-console`.** Reverted in review
  before. `page.jte` is compiled by every console module's template tests against a classpath
  lacking Spring Security / `jakarta.servlet-api`. This viewer has **no forms**, so it needs no CSRF
  reads at all — keep templates form-free.
- **Templates call the shared layout as a content block:** `@template.layout.page(title = ...,
  content = @`...`)` — not inheritance.
- `maven-compiler-plugin` with `<parameters>true</parameters>` in the console pom (so Spring binds
  `@PathVariable String name` without an explicit `name=`).
- New-module scaffolding (`domains/CLAUDE.md`): module dir + pom, `<module>` in
  `domains/soil/pom.xml`, **root** `pom.xml` `dependencyManagement` entry with `${project.version}`.
- Build/verify is the user's `mvn verify` from repo root; this session uses IDE per-file checks. No
  commit until the user says so.
- Soil query surface (verify against `soil-api`): `SoilProfileInfoQuery.findPage(PageRequest) →
  Page<SoilProfileInfo>`; `SoilProfileQuery.getBySoilProfileName(SoilProfileName) →
  Optional<SoilProfile>`. `SoilProfile.info()/labAnalyses()/latestLabAnalysis()/soilProfileName()`.
  `SoilProfileInfo.name()/zoneName()/subZoneName()/isSubZoneScoped()`. `LabAnalysis.info()/
  nutrients()/physicalCharacteristics()`. `LabAnalysisInfo.crop()/sampleDate()/labId()/labSampleId()`.
  `NutrientPanel.primary()/secondary()/micro()`; group accessors per nutrient; `NutrientReading.
  value()/unit()`; `MeasurementUnit.symbol()`. `SoilPhysicalCharacteristics.pH()/ecDsPerMeter()/
  cecMeqPer100g()/limestonePct()/saturationPct()/cationBaseSaturation()`; each NamedValue `.value()`;
  `CationBaseSaturation.calciumPct()/magnesiumPct()/potassiumPct()/sodiumPct()/hydrogenPct()`.

---

### Task 1: `soil-test-context` module (the wiring seam)

**Files:**
- Create: `domains/soil/soil-test-context/pom.xml`
- Create: `domains/soil/soil-test-context/src/main/java/com/naturalist/soil/SoilTestContext.java`
- Create: `domains/soil/soil-test-context/src/test/java/com/naturalist/soil/SoilTestContextTest.java`
- Modify: `domains/soil/pom.xml` (add `<module>soil-test-context</module>`)
- Modify: `pom.xml` (root — add `soil-test-context` to the SOIL `dependencyManagement`, alphabetical
  after `soil-repository-test`, version `${project.version}`)

**Interfaces produced:** `SoilTestContext.create(NaturalistDatabase) : SoilTestContext`, with
`soilProfileQuery() : SoilProfileQuery` and `soilProfileInfoQuery() : SoilProfileInfoQuery`.

- [ ] **Step 1 — pom.** `domains/soil/soil-test-context/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.naturalist</groupId>
        <artifactId>soil</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>soil-test-context</artifactId>

    <dependencies>
        <dependency>
            <groupId>com.naturalist</groupId>
            <artifactId>soil-api</artifactId>
        </dependency>
        <dependency>
            <groupId>com.naturalist</groupId>
            <artifactId>soil-core</artifactId>
        </dependency>
        <dependency>
            <groupId>com.naturalist</groupId>
            <artifactId>soil-repository-test</artifactId>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 2 — register the module + root dep-mgmt.** Add `<module>soil-test-context</module>` to
  `domains/soil/pom.xml`'s `<modules>`, and this block to the root `pom.xml` SOIL section (after the
  `soil-repository-test` entry):

```xml
            <dependency>
                <groupId>com.naturalist</groupId>
                <artifactId>soil-test-context</artifactId>
                <version>${project.version}</version>
            </dependency>
```

- [ ] **Step 3 — `SoilTestContext`.** Promote `SoilsTestContextInternal` to a public module context
  (keep `soilProfileInfoQuery` as a field with an accessor). `.../soil/SoilTestContext.java`:

```java
package com.naturalist.soil;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.soil.observation.LabAnalysisInfoQueries;
import com.naturalist.soil.observation.LabAnalysisInfoQuery;
import com.naturalist.soil.observation.NutrientReadingQueries;
import com.naturalist.soil.observation.NutrientReadingQuery;
import com.naturalist.soil.observation.SoilPhysicalCharacteristicsQueries;
import com.naturalist.soil.observation.SoilPhysicalCharacteristicsQuery;

/**
 * Pre-wired, in-memory read surface for the soil bounded context: assembles the entity queries and
 * the {@code SoilProfileFactory}, exposing the {@link SoilProfileQuery} (assembled profile) and the
 * {@link SoilProfileInfoQuery} (profile enumeration). Lives in package {@code com.naturalist.soil}
 * for split-package access to soil-core's package-private impls. Goes away when Spring DI replaces
 * the manual composition.
 */
public class SoilTestContext {

    private final SoilProfileInfoQuery soilProfileInfoQuery;
    private final SoilProfileQuery soilProfileQuery;

    private SoilTestContext(NaturalistDatabase db) {
        this.soilProfileInfoQuery =
                new SoilProfileInfoQueryImpl(new SoilProfileInfoEntityRepositoryMock(db));
        LabAnalysisInfoQuery labAnalysisInfoQuery = LabAnalysisInfoQueries.create(db);
        NutrientReadingQuery nutrientReadingQuery = NutrientReadingQueries.create(db);
        SoilPhysicalCharacteristicsQuery physicalCharacteristicsQuery =
                SoilPhysicalCharacteristicsQueries.create(db);
        SoilProfileFactory factory = new SoilProfileFactory(
                soilProfileInfoQuery, labAnalysisInfoQuery, nutrientReadingQuery, physicalCharacteristicsQuery);
        this.soilProfileQuery = new SoilProfileQueryImpl(factory);
    }

    public static SoilTestContext create(NaturalistDatabase db) {
        return new SoilTestContext(db);
    }

    public SoilProfileQuery soilProfileQuery() {
        return soilProfileQuery;
    }

    public SoilProfileInfoQuery soilProfileInfoQuery() {
        return soilProfileInfoQuery;
    }
}
```

- [ ] **Step 4 — smoke test.** `.../soil/SoilTestContextTest.java`:

```java
package com.naturalist.soil;

import com.naturalist.data.NaturalistDatabase;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SoilTestContextTest {

    @Test
    void assemblesBox1WithItsAnalysis() {
        SoilTestContext context = SoilTestContext.create(NaturalistDatabase.create());
        var profile = context.soilProfileQuery().getBySoilProfileName(SoilProfileName.of("box1"));

        assertThat(profile).isPresent();
        assertThat(profile.get().labAnalyses()).isNotEmpty();
        assertThat(context.soilProfileInfoQuery()
                .findPage(com.naturalist.data.PageRequest.console(0)).content())
                .isNotEmpty();
    }
}
```

- [ ] **Step 5 — verify + commit.** `mvn -q -pl domains/soil/soil-test-context -am verify`; expect
  the smoke test green. Commit `feat(soil): soil-test-context wiring seam`.

### Task 2: `soil-console` module + JTE templates + template tests

**Files:**
- Create: `domains/soil/soil-console/pom.xml`
- Create: `domains/soil/soil-console/src/main/jte/soil/readingRow.jte`
- Create: `domains/soil/soil-console/src/main/jte/soil/list.jte`
- Create: `domains/soil/soil-console/src/main/jte/soil/profile.jte`
- Create: `domains/soil/soil-console/src/test/java/com/naturalist/soil/console/TestTemplateEngine.java`
- Create: `domains/soil/soil-console/src/test/java/com/naturalist/soil/console/SoilConsoleTemplateTest.java`
- Modify: `domains/soil/pom.xml` (add `<module>soil-console</module>`)
- Modify: `pom.xml` (root — add `soil-console` to the SOIL `dependencyManagement`, after `soil-api`)

**Interfaces consumed:** `SoilTestContext` (Task 1); the soil query/read-model surface (Global
Constraints).

- [ ] **Step 1 — pom.** `domains/soil/soil-console/pom.xml` (minimal vs insects — soil has no
  authority/catalog/library deps):

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xmlns="http://maven.apache.org/POM/4.0.0"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.naturalist</groupId>
        <artifactId>soil</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>soil-console</artifactId>
    <name>soil :: console</name>

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
            <artifactId>soil-api</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-context</artifactId>
        </dependency>
        <dependency>
            <groupId>jakarta.servlet</groupId>
            <artifactId>jakarta.servlet-api</artifactId>
            <scope>provided</scope>
        </dependency>

        <!-- TEMPORARY: read data via the in-memory test context until the rdbms adapter lands -->
        <dependency>
            <groupId>com.naturalist</groupId>
            <artifactId>soil-test-context</artifactId>
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

Register the module in `domains/soil/pom.xml` `<modules>` and add the root dep-mgmt block (SOIL
section, after `soil-api`):

```xml
            <dependency>
                <groupId>com.naturalist</groupId>
                <artifactId>soil-console</artifactId>
                <version>${project.version}</version>
            </dependency>
```

- [ ] **Step 2 — reading-row component.** `.../src/main/jte/soil/readingRow.jte`:

```jte
@import com.naturalist.soil.observation.NutrientReading
@param String label
@param NutrientReading reading
<tr><td>${label}</td><td>${reading.value().toString()}</td><td>${reading.unit().symbol()}</td></tr>
```

- [ ] **Step 3 — list template.** `.../src/main/jte/soil/list.jte`:

```jte
@import com.naturalist.soil.SoilProfile
@import java.util.List

@param List<SoilProfile> profiles

@template.layout.page(title = "Soil Profiles", content = @`
    <h1>Soil Profiles</h1>
    <p>Managed soil units at Oak Vista, with their most recent lab analysis.</p>

    <div class="entity-grid">
        @for(var profile : profiles)
            !{var info = profile.info();}
            <article>
                <header>
                    <a href="/soil/profiles/${info.name().value()}">
                        <strong>${info.name().value()}</strong>
                    </a>
                </header>
                <dl class="taxonomy">
                    <dt>zone</dt>
                    <dd>${info.zoneName().value()}@if(info.isSubZoneScoped()) · ${info.subZoneName().value()}@endif</dd>
                    @if(profile.latestLabAnalysis().isPresent())
                        !{var latest = profile.latestLabAnalysis().get();}
                        <dt>crop</dt>
                        <dd>${latest.info().crop().value()}</dd>
                        <dt>latest test</dt>
                        <dd>${latest.info().sampleDate().toString()}</dd>
                    @endif
                </dl>
            </article>
        @endfor
    </div>
`)
```

- [ ] **Step 4 — profile detail template.** `.../src/main/jte/soil/profile.jte`:

```jte
@import com.naturalist.soil.SoilProfile

@param SoilProfile profile

!{var info = profile.info();}

@template.layout.page(title = info.name().value(), content = @`
    <h1>Soil Profile — ${info.name().value()}</h1>
    <dl class="taxonomy">
        <dt>zone</dt>
        <dd>${info.zoneName().value()}@if(info.isSubZoneScoped()) · ${info.subZoneName().value()}@endif</dd>
    </dl>

    @if(profile.labAnalyses().isEmpty())
        <p><em>No lab analyses on record for this profile.</em></p>
    @else
        @for(var analysis : profile.labAnalyses())
            !{var a = analysis.info();}
            !{var panel = analysis.nutrients();}
            !{var chars = analysis.physicalCharacteristics();}
            !{var bs = chars.cationBaseSaturation();}
            <section class="analysis">
                <h2>${a.sampleDate().toString()} — ${a.crop().value()}</h2>
                <p class="analysis-provenance">${a.labId()} · sample ${a.labSampleId()}</p>

                <h3>Primary nutrients</h3>
                <table>
                    <thead><tr><th>Nutrient</th><th>Value</th><th>Unit</th></tr></thead>
                    <tbody>
                        @template.soil.readingRow(label = "Nitrate-N", reading = panel.primary().nitrateN())
                        @template.soil.readingRow(label = "Phosphorus (P₂O₅)", reading = panel.primary().phosphorusP2O5())
                        @template.soil.readingRow(label = "Potassium — exchangeable", reading = panel.primary().potassiumExch())
                        @template.soil.readingRow(label = "Potassium — soluble", reading = panel.primary().potassiumSoluble())
                    </tbody>
                </table>

                <h3>Secondary nutrients</h3>
                <table>
                    <thead><tr><th>Nutrient</th><th>Value</th><th>Unit</th></tr></thead>
                    <tbody>
                        @template.soil.readingRow(label = "Calcium — exchangeable", reading = panel.secondary().calciumExch())
                        @template.soil.readingRow(label = "Calcium — soluble", reading = panel.secondary().calciumSoluble())
                        @template.soil.readingRow(label = "Magnesium — exchangeable", reading = panel.secondary().magnesiumExch())
                        @template.soil.readingRow(label = "Magnesium — soluble", reading = panel.secondary().magnesiumSoluble())
                        @template.soil.readingRow(label = "Sodium — exchangeable", reading = panel.secondary().sodiumExch())
                        @template.soil.readingRow(label = "Sodium — soluble", reading = panel.secondary().sodiumSoluble())
                        @template.soil.readingRow(label = "Sulfate", reading = panel.secondary().sulfate())
                    </tbody>
                </table>

                <h3>Micronutrients</h3>
                <table>
                    <thead><tr><th>Nutrient</th><th>Value</th><th>Unit</th></tr></thead>
                    <tbody>
                        @template.soil.readingRow(label = "Zinc", reading = panel.micro().zinc())
                        @template.soil.readingRow(label = "Manganese", reading = panel.micro().manganese())
                        @template.soil.readingRow(label = "Iron", reading = panel.micro().iron())
                        @template.soil.readingRow(label = "Copper", reading = panel.micro().copper())
                        @template.soil.readingRow(label = "Boron", reading = panel.micro().boron())
                        @template.soil.readingRow(label = "Chloride", reading = panel.micro().chloride())
                    </tbody>
                </table>

                <h3>Physical &amp; derived</h3>
                <table>
                    <tbody>
                        <tr><td>pH</td><td>${chars.pH().value().toString()}</td></tr>
                        <tr><td>Soil salinity (EC)</td><td>${chars.ecDsPerMeter().value().toString()} dS/m</td></tr>
                        <tr><td>CEC</td><td>${chars.cecMeqPer100g().value().toString()} meq/100g</td></tr>
                        <tr><td>Limestone</td><td>${chars.limestonePct().value().toString()} %</td></tr>
                        <tr><td>Saturation</td><td>${chars.saturationPct().value().toString()} %</td></tr>
                    </tbody>
                </table>

                <h3>Cation base saturation</h3>
                <table>
                    <tbody>
                        <tr><td>Calcium</td><td>${bs.calciumPct().toString()} %</td></tr>
                        <tr><td>Magnesium</td><td>${bs.magnesiumPct().toString()} %</td></tr>
                        <tr><td>Potassium</td><td>${bs.potassiumPct().toString()} %</td></tr>
                        <tr><td>Sodium</td><td>${bs.sodiumPct().toString()} %</td></tr>
                        <tr><td>Hydrogen</td><td>${bs.hydrogenPct().toString()} %</td></tr>
                    </tbody>
                </table>
            </section>
        @endfor
    @endif
`)
```

- [ ] **Step 5 — test engine.** Copy the insects `TestTemplateEngine` verbatim, changing only the
  domain root. `.../src/test/java/com/naturalist/soil/console/TestTemplateEngine.java` — identical to
  `domains/insects/insects-console/src/test/java/com/naturalist/insects/console/TestTemplateEngine.java`
  except:

```java
package com.naturalist.soil.console;
// ...imports identical...
final class TestTemplateEngine {
    private static final String[] TEMPLATE_ROOTS = {
            "apps/management-console/src/main/jte",
            "domains/soil/soil-console/src/main/jte",
    };
    // ...rest identical to the insects TestTemplateEngine (create(), findProjectRoot(),
    //    the private CompositeCodeResolver record)...
}
```

- [ ] **Step 6 — template tests (write first, watch fail, then add templates already above).**
  `.../src/test/java/com/naturalist/soil/console/SoilConsoleTemplateTest.java`:

```java
package com.naturalist.soil.console;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.PageRequest;
import com.naturalist.soil.SoilProfile;
import com.naturalist.soil.SoilProfileName;
import com.naturalist.soil.SoilTestContext;
import gg.jte.output.StringOutput;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SoilConsoleTemplateTest {

    private static SoilTestContext context() {
        return SoilTestContext.create(NaturalistDatabase.create());
    }

    @Test
    void list_rendersProfilesWithZoneAndCrop() {
        SoilTestContext ctx = context();
        List<SoilProfile> profiles = ctx.soilProfileInfoQuery()
                .findPage(PageRequest.console(0)).content().stream()
                .map(info -> ctx.soilProfileQuery().getBySoilProfileName(info.name()).orElseThrow())
                .toList();
        StringOutput output = new StringOutput();

        TestTemplateEngine.create().render("soil/list.jte", Map.of("profiles", profiles), output);

        String html = output.toString();
        assertThat(html).contains("box1");
        assertThat(html).contains("tomato");
        assertThat(html).contains("/soil/profiles/box1");
    }

    @Test
    void profile_rendersPanelAndCharacteristics() {
        SoilProfile box1 = context().soilProfileQuery()
                .getBySoilProfileName(SoilProfileName.of("box1")).orElseThrow();
        StringOutput output = new StringOutput();

        TestTemplateEngine.create().render("soil/profile.jte", Map.of("profile", box1), output);

        String html = output.toString();
        assertThat(html).contains("Nitrate-N");
        assertThat(html).contains("6.99");           // soluble calcium
        assertThat(html).contains("lbs/1000 ft²");   // MeasurementUnit.symbol()
        assertThat(html).contains("7.2");             // pH
        assertThat(html).contains("44.9");            // CEC
    }
}
```

- [ ] **Step 7 — verify + commit.** `mvn -q -pl domains/soil/soil-console -am verify`; expect both
  template tests green (this renders the real assembled box1 through JTE). Commit
  `feat(soil): soil-console templates + template tests`.

### Task 3: `SoilsController` + management-console wiring

**Files:**
- Create: `domains/soil/soil-console/src/main/java/com/naturalist/soil/console/SoilsController.java`
- Modify: `apps/management-console/pom.xml` (add `soil-console` to the DOMAIN CONSOLE MODULES block)
- Modify: `apps/management-console/src/main/jte/layout/page.jte` (add the "Soil" nav `<li>`)

**Interfaces consumed:** `SoilTestContext` (Task 1), templates `soil/list.jte` + `soil/profile.jte`
(Task 2).

- [ ] **Step 1 — controller.** `.../soil/console/SoilsController.java`:

```java
package com.naturalist.soil.console;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.PageRequest;
import com.naturalist.soil.SoilProfile;
import com.naturalist.soil.SoilProfileInfoQuery;
import com.naturalist.soil.SoilProfileName;
import com.naturalist.soil.SoilProfileQuery;
import com.naturalist.soil.SoilTestContext;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/soil")
public class SoilsController {

    private final SoilProfileInfoQuery soilProfileInfoQuery;
    private final SoilProfileQuery soilProfileQuery;

    SoilsController() {
        // TODO: becomes a Spring-managed bean when the rdbms adapter replaces the in-memory context.
        NaturalistDatabase db = NaturalistDatabase.create();
        SoilTestContext context = SoilTestContext.create(db);
        this.soilProfileInfoQuery = context.soilProfileInfoQuery();
        this.soilProfileQuery = context.soilProfileQuery();
    }

    @GetMapping
    String index() {
        return "redirect:/soil/profiles";
    }

    @GetMapping("/profiles")
    String profiles(Model model) {
        List<SoilProfile> profiles = soilProfileInfoQuery
                .findPage(PageRequest.console(0)).content().stream()
                .map(info -> soilProfileQuery.getBySoilProfileName(info.name()).orElseThrow())
                .toList();
        model.addAttribute("profiles", profiles);
        return "soil/list";
    }

    @GetMapping("/profiles/{name}")
    String profileDetail(@PathVariable String name, Model model) {
        var profile = soilProfileQuery.getBySoilProfileName(SoilProfileName.of(name));
        if (profile.isEmpty()) {
            return "redirect:/soil/profiles";
        }
        model.addAttribute("profile", profile.get());
        return "soil/profile";
    }
}
```

- [ ] **Step 2 — app dependency.** Add to `apps/management-console/pom.xml`, in the
  `<!-- DOMAIN CONSOLE MODULES -->` block (alphabetical, after `plants-console`):

```xml
        <dependency>
            <groupId>com.naturalist</groupId>
            <artifactId>soil-console</artifactId>
        </dependency>
```

- [ ] **Step 3 — nav link.** In `apps/management-console/src/main/jte/layout/page.jte`, in the
  `<ul class="primary-nav-links">`, add after the Plants entry:

```jte
                <li><a href="/soil">Soil</a></li>
```

- [ ] **Step 4 — verify.** `mvn -q verify` from repo root (the whole reactor, including
  `apps/management-console`, must compile with the new module + template test suites green). Then run
  the console and visit `/soil` → the four profiles; click `box1` → its analysis with the three
  nutrient tables + physical characteristics.

```bash
mvn -q -pl apps/management-console -am spring-boot:run
# then open http://localhost:8080/soil
```

- [ ] **Step 5 — commit.** `feat(soil): soil-console controller + management-console nav`.

## Self-review

- **Spec coverage:** Task 1 = `soil-test-context` (spec §Design/wiring); Task 2 = module + templates
  + tests (spec routes/templates/testing); Task 3 = controller + nav (spec routes + nav). All spec
  sections map to a task.
- **Placeholder scan:** none — every pom/template/class is full verbatim content; the one "identical
  to insects except…" (TestTemplateEngine) names the exact file to copy and the single line to change.
- **Type consistency:** `SoilTestContext.soilProfileQuery()/soilProfileInfoQuery()` used identically
  in Tasks 1–3; `PageRequest.console(0)`, `SoilProfileName.of(name)`, and the read-model accessors
  match the Global Constraints surface.
- **Risks / open items:**
  - The controller constructs its own `NaturalistDatabase` per instance (matches `InsectsController`);
    no `SoilDataConfiguration` is needed for a read-only viewer. If the app's persistence-flush
    startup expects the soil `TestEntitySource`s as beans, add a `SoilDataConfiguration` mirroring
    `InsectDataConfiguration` — verify at Task 3 Step 4 that the app starts clean.
  - Detail route with a non-kebab `{name}` makes `SoilProfileName.of` invalid → the query's arg
    observer could throw (500). This mirrors `InsectsController.orderDetail` exactly and is accepted;
    not handled here.
  - `soil/list.jte` assembles all profiles (4) to show crop + date; fine at this scale. If profiles
    grow large, switch the list to `SoilProfileInfoQuery` alone (name + zone) and move crop/date to
    detail.
