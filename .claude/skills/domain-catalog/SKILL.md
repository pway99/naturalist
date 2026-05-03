---
name: domain-catalog
description: >
  Wire a domain into the cross-domain catalog kernel — forward search
  contribution, inverse back-reference providers, and the console URL
  linker. Use when adding catalog support to a domain that does not yet
  have it, or when adding a new cross-domain reference type to a domain
  that already participates. Triggers on phrases like "add catalog
  support to chemistry", "wire the insects domain into the catalog",
  "make compounds searchable", or explicit invocations like
  "/domain-catalog <domain>".
allowed-tools: Read Write Edit Glob Grep Bash
argument-hint: <domain>
---

# Domain Catalog Skill

## Purpose

Given a `<domain>` in `$ARGUMENTS` (e.g. `chemistry`, `insects`,
`apiary`), scaffold the parts that connect that domain to the catalog
kernel. The skill walks three orthogonal axes — pick the ones that
apply to this domain:

1. **`<Domain>Domain`** — `DomainId` record in `<domain>-api`.
   Prerequisite for everything else. Create only if absent.
2. **`<Domain>CatalogContribution`** — forward search SPI in
   `<domain>-core`. One per domain. Always part of "catalog support".
3. **`<Domain><Foreign>References`** — inverse SPI in `<domain>-core`.
   Zero-to-many per domain — one per cross-domain `EntityName` this
   domain holds references to. Add as the cross-domain references
   accumulate, not all at once.
4. **`<Domain>Linker`** — `EntityRefLinker` in `<domain>-console`.
   One per console module. Skip if the domain has no `*-console`.

Reference implementation: the plants stack — `PlantsDomain`,
`PlantCatalogContribution`, `PlantCompoundReferences`, `PlantsLinker`.
Plan and rationale: `kernels/catalog/PLAN-redirect.md`.

---

## Prerequisites

Before scaffolding, verify the following exist for the target domain.
If anything required is missing, stop and report — this skill does not
create entities, queries, or repositories.

- `<domain>-api/src/main/java/com/naturalist/<domain>/` package exists.
- At least one `NamedEntity` in the domain whose `EntityName` you can
  emit search tokens for (the contribution needs a non-empty source).
- A `<Entity>EntityQuery` with `allEntityNames()` / `findByNameSet(...)`
  surface for every entity the contribution will iterate. If queries
  are missing, run `/entity-query` first.

Optional but expected when applicable:

- `Test<Domain>Identifiers` with at least two known constants per
  iterated entity — needed by the contribution test.
- A `<domain>-console` Maven module — needed for the linker.

---

## Step 1 — Locate domain context and confirm scope

Find what's already in place. Run these in parallel:

- `find domains/<domain> -name "<Domain>Domain.java"` — does the
  `DomainId` exist?
- `find domains/<domain>/<domain>-core -path "*/catalog/*.java"` —
  forward / inverse implementations already present?
- `find domains/<domain>/<domain>-console -name "<Domain>Linker.java"`
  — linker already present?
- `ls domains/<domain>/` — does `<domain>-console` exist at all?

Then ask the user, **with the audit results in hand**, which of the
four artifacts to scaffold this run. Common shapes:

- *"Add catalog support to chemistry"* → all four (DomainId already
  exists; contribution + linker; ask which inverse providers).
- *"Add insect back-references for compounds"* → only step 4
  (inverse provider) — a single `Insect<Foreign>References`.
- *"New domain X just landed; wire it into the catalog"* → all four,
  including `<Domain>Domain`.

Do not assume. The skill's value is correct scaffolding, not maximal
scaffolding — the plan-redirect's whole premise is "add when needed."

---

## Step 2 — `<Domain>Domain` in `<domain>-api`

Skip if the file exists. Otherwise create
`domains/<domain>/<domain>-api/src/main/java/com/naturalist/<domain>/<Domain>Domain.java`:

```java
package com.naturalist.

<domain>;

import com.naturalist.catalog.DomainId;
import com.naturalist.infrastructure.DomainService;

/**
 * The <domain> domain — see {@code domains/<domain>/}.
 * <p>
 * Owned by {@code <domain>-api} per the plan's "open {@link DomainId}"
 * decision: each domain ships its own slug-bearing subtype, the kernel
 * knows the names of no domains, and the catalog assembly enforces slug
 * uniqueness across registered subtypes at startup.
 */
@DomainService
public record<Domain> Domain() implements

DomainId {

    @Override public String value () {
        return "<domain>";
    }

    @Override public String toString () {
        return value();
    }
}
```

`<Domain>` is the Pascal-case domain noun (`Chemistry`, `Insects`,
`Apiary`); `<domain>` is the lowercase Maven module slug.

POM update for `<domain>-api/pom.xml` — add (alphabetised within the
`com.naturalist` block):

```xml

<dependency>
    <groupId>com.naturalist</groupId>
    <artifactId>catalog</artifactId>
</dependency>
```

If the api pom does not currently depend on `catalog`, this is the
first time the domain participates — also add the matching
`<dependencyManagement>` entry to the root pom only if missing
(catalog ships there already; usually nothing to do).

A two- or three-test class in
`<domain>-api/src/test/java/.../<Domain>DomainTest.java` covering
`value()`, `toString()`, equality (record default), and the
`DomainId` contract is appropriate but optional — model on existing
`PlantsDomainTest` if present.

---

## Step 3 — `<Domain>CatalogContribution` in `<domain>-core`

Create
`domains/<domain>/<domain>-core/src/main/java/com/naturalist/<domain>/catalog/<Domain>CatalogContribution.java`:

```java
package com.naturalist.

<domain>.catalog;

import com.naturalist.catalog.CatalogContribution;
import com.naturalist.catalog.DomainId;
import com.naturalist.catalog.EntityRef;
import com.naturalist.<domain>.<Domain>Domain;
import com.naturalist.<domain>.<Entity>;
import com.naturalist.<domain>.<Package>EntityCollections.<Entity>Collection;
import com.naturalist.<domain>.<Package>Query;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.observability.Observer;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Forward-direction catalog contribution for the <domain> domain.
 * Emits one {@link SearchableEntity} per <Entity> with the tokens under
 * which a young naturalist might search.
 *
 * <h2>Token collisions are normal</h2>
 * Per the redirect plan's M4′ entry, this contribution emits every
 * derivable token unconditionally — ambiguity filtering is the
 * search index's concern, not the contribution's.
 *
 * <h2>Live derivation</h2>
 * {@link #searchableEntities()} returns a fresh stream on every call.
 * Entities added after assembly are reflected automatically.
 */
@DomainService
public class <Domain>CatalogContribution implements

        CatalogContribution {

            private static final DomainId DOMAIN = new <Domain>Domain();

            private final <Package > Query.<Entity>EntityQuery < entityPlural >;

            public <Domain > CatalogContribution( < Package > Query.<Entity>EntityQuery < entityPlural >){
                Observer.forClass( < Domain > CatalogContribution.class)
                .arguments("constructor", i -> i.notNull( < entityPlural >, "<entityPlural>"))
                .throwWhenInvalid();
                this.<entityPlural> = < entityPlural >;
            }

            @Override
            public DomainId domain () {
                return DOMAIN;
            }

            @Override
            public Stream<SearchableEntity> searchableEntities () {
                var names = <entityPlural >.all<Entity> Names ();
                if (names.isEmpty()) {
                    return Stream.empty();
                }
        <Entity > Collection collection =
                        < entityPlural >.findByNameSet(names.stream().collect(Collectors.toSet()));
                return collection.stream().map( < Domain > CatalogContribution::toSearchableEntity);
            }

            private static SearchableEntity toSearchableEntity ( < Entity > entity){
                EntityRef target = new EntityRef(DOMAIN, entity.name());
                return new SearchableEntity(target, tokensFor(entity));
            }

            private static Stream<String> tokensFor ( < Entity > entity){
                Stream.Builder<String> tokens = Stream.builder();
                tokens.add(entity.name().value());
                // domain-specific tokens go here — see "Token derivation" below
                return tokens.build();
            }
        }
```

### Token derivation

The slug is always emitted (it powers `EXACT_SLUG` matches). Beyond
that, what tokens make sense **is a domain decision** the user should
confirm. Common patterns from existing domains:

- **Organism domains** (plants, insects, arachnids, …) — taxonomic
  binomial (`genus + " " + species`), genus alone, abbreviated
  binomial (`G. species`), and each `CommonName` label. See
  `PlantCatalogContribution.tokensFor(...)` for the canonical shape.
- **Chemistry compounds** — slug, `commonName`, molecular formula
  (`compoundInfo.formula()`), and any well-known synonyms held in
  `properties`. Decide per case whether to emit fragment tokens (a
  search for `acid` should not return every organic acid).
- **Products / SKUs** — slug and `displayName`; possibly each
  contained `CompoundName.value()` so a search for an ingredient
  surfaces formulations containing it.
- **Multi-entity domains** — a single contribution may iterate more
  than one entity type by composing multiple `EntityQuery` accessors
  through one or several `Stream`-emitting helper methods. Mirror the
  approach in `PlantCatalogContribution` for each entity, then
  `Stream.concat(...)` or flat-map within `searchableEntities()`.

If common-name tokens are wanted but the entity does not yet carry
`Set<CommonName> commonNames`, propose the field addition in
`<domain>-api` first (the api module already depends on `field-notes`,
so `CommonName` is reachable) — that is its own change, not a piece
of this skill.

### Test for the contribution

Create
`domains/<domain>/<domain>-core/src/test/java/com/naturalist/<domain>/<Domain>CatalogContributionTest.java`
**(in the entity's package, not under `.catalog`)** so the test sees
package-private repository mocks and query impls. Pattern after
`PlantCatalogContributionTest`:

```java
package com.naturalist.

<domain>;

import com.naturalist.catalog.*;
import com.naturalist.catalog.inmem.CatalogAssembly;
import com.naturalist.data.NaturalistDatabaseExtension;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.<domain>.catalog.<Domain>CatalogContribution;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class

<Domain> CatalogContributionTest {

    @RegisterExtension
    NaturalistDatabaseExtension db = NaturalistDatabaseExtension.create();

    private final <Package > Repository.<Entity>EntityRepository repository =
            new <Entity>EntityRepositoryMock(db);
    private final <Package > Query.<Entity>EntityQuery entityQuery =
            new <Entity>EntityQueryImpl(repository);
    private final <Domain > CatalogContribution contribution =
            new <Domain>CatalogContribution(entityQuery);

    @Test
    void domainIs<Domain > () {
        assertThat(contribution.domain()).isEqualTo(new <Domain>Domain());
    }

    @Test
    void constructorRejectsNullEntityQuery () {
        assertThatThrownBy(() -> new <Domain>CatalogContribution(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("<entityPlural>");
    }

    @Test
    void contributionEmitsOneSearchableEntityPerEntity () {
        long count = entityQuery.all < Entity > Names().size();
        assertThat(contribution.searchableEntities().count()).isEqualTo(count);
    }

    @Test
    void everySearchableEntityIsAttributedToThe<Domain > Domain() {
        contribution.searchableEntities().forEach(e ->
                assertThat(e.target().domain()).isEqualTo(new <Domain>Domain()));
    }

    @Test
    void knownEntityIsReachableThroughItsSlug () {
        Catalog catalog = CatalogAssembly.from(contribution);
        EntityRef expected = new EntityRef(
                new <Domain>Domain(),
                Test < Domain > Identifiers./* path to */.name);

        assertThat(catalog.search("<known-slug>").stream())
                .as("slug '<known-slug>' should resolve to %s as EXACT_SLUG", expected)
                .anyMatch(h -> h.target().equals(expected)
                        && h.kind() == MatchKind.EXACT_SLUG);
    }

    // Add one test per token family the contribution emits — binomial,
    // common-name, formula, etc. Use real fixture data from the JSON
    // catalog; do not synthesise entities.
    //
    // Use AssertJ `.as("…should resolve to %s", expected)` on every
    // search assertion. When a search returns the wrong target, the
    // default failure message prints only the raw collection diff —
    // useful for AND/OR debugging of token derivation but unhelpful
    // for *which* entity the search resolved to. The description
    // makes the expected EntityRef explicit in the message.
}
```

### Picking "unknown token" probes

When the test asserts that an unknown token returns no hits, **avoid
strings whose substrings are common English fragments**. The kernel
tokenises on whitespace and hyphens, so probes like
`"not-an-insect-anywhere"` decompose into `not`, `an`, `insect`,
`anywhere` — and `an` is a token a real entity might emit (any
binomial whose species epithet starts with `an`-, any common name
containing the word "an"). The probe then matches a real entity and
the assertion fails for the wrong reason.

Use an opaque, alphabetic-only probe whose tokens cannot collide:
`"qqqqxxxx"`, `"zzzzzzz"`. One word, no separators, no real-language
fragment.

POM updates for `<domain>-core/pom.xml`:

```xml
<!-- main scope -->
<dependency>
    <groupId>com.naturalist</groupId>
    <artifactId>catalog</artifactId>
</dependency>

        <!-- test scope (for CatalogAssembly.from in tests) -->
<dependency>
<groupId>com.naturalist</groupId>
<artifactId>catalog-inmem</artifactId>
<scope>test</scope>
</dependency>
```

---

## Step 4 — `<Domain><Foreign>References` (inverse providers)

One file per cross-domain `EntityName` type. The naming convention is
**`<Domain><Foreign>References`** — `PlantCompoundReferences` answers
"what plants reference this `CompoundName`?". `<Foreign>` is the
foreign entity's bare name, prefix-stripped where unambiguous
(`Compound`, not `Chemistry`).

For each provider the user wants this run, identify:

- The **foreign `EntityName` subclass** it answers for (`CompoundName`,
  `PlantName`, …). Cross-check the import path against the
  `identifiers/` module.
- The **owning entity in this domain** that holds the reference (e.g.
  `PhytochemicalConstituent.compoundName`).
- The **`EntityQuery` accessor** that returns the matching entities
  (e.g. `forCompoundName(target)` returning a
  `PhytochemicalConstituentCollection`). If no such accessor exists
  the user must add it via `/entity-query` before this provider can
  be written.
- Whether the result should include **two refs per match** — the owner
  and the link record — as `PlantCompoundReferences` does. Default
  yes when the link entity carries its own `EntityName`; otherwise
  one ref to the owner.

Create
`domains/<domain>/<domain>-core/src/main/java/com/naturalist/<domain>/catalog/<Domain><Foreign>References.java`:

```java
package com.naturalist.

<domain>.catalog;

import com.naturalist.catalog.DomainId;
import com.naturalist.catalog.EntityRef;
import com.naturalist.catalog.EntityReferences;
import com.naturalist.<foreignDomain>.<Foreign>Name;
import com.naturalist.<domain>.<Domain>Domain;
import com.naturalist.<domain>.<linkSubpackage>.<LinkEntity>;
import com.naturalist.<domain>.<linkSubpackage>.<LinkPackage>EntityCollections.<LinkEntity>Collection;
import com.naturalist.<domain>.<linkSubpackage>.<LinkPackage>Query;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.observability.Observer;
import com.naturalist.resilience.Resilient;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Inverse-direction catalog provider for the <domain> domain. Answers
 * "which <domain> entities reference this {@link <Foreign>Name}?"
 *
 * <h2>Live, not cached</h2>
 * The kernel re-queries on every fan-out. No caching here either.
 *
 * <h2>Empty target</h2>
 * A {@code null} target short-circuits to an empty stream. Defensive —
 * the {@code Catalog} surface already maps null to an empty result.
 */
@DomainService
@Resilient(name = "catalog.fanout")
public class <Domain><Foreign>References implements EntityReferences<<Foreign>Name>{

        private static final DomainId DOMAIN = new <Domain>Domain();

        private final <LinkPackage>Query .<LinkEntity>EntityQuery<linkPlural>;

        public <Domain>

        <Foreign> References(<LinkPackage>Query.<LinkEntity>EntityQuery<linkPlural>) {
            Observer.forClass( < Domain > < Foreign > References.class)
                .arguments("constructor", i -> i.notNull( < linkPlural >, "<linkPlural>"))
                .throwWhenInvalid();
            this.<linkPlural> = < linkPlural >;
        }

        @Override
        public DomainId domain() {
            return DOMAIN;
        }

        @Override
        public Class<<Foreign>Name>

        referenceType() {
            return <Foreign > Name.class;
        }

        @Override
        public Stream<EntityRef> referencesTo(<Foreign>Name target) {
            if (target == null) {
                return Stream.empty();
            }
        <LinkEntity > Collection matches = < linkPlural >.for<Foreign > Name(target);

            Set << OwnerName >> seenOwners = new LinkedHashSet<>();
            Stream.Builder<EntityRef> refs = Stream.builder();
            matches.stream().forEach(link -> {
                if (seenOwners.add(link.<ownerNameAccessor> ())){
                    refs.add(new EntityRef(DOMAIN, link.<ownerNameAccessor> ()));
                }
                refs.add(new EntityRef(DOMAIN, link.name()));
            });
            return refs.build();
        }
}
```

If the domain holds the foreign reference directly on the owning
entity (rather than through a link record), the inner loop simplifies
to a single ref per owner — drop the `seenOwners`/two-ref pattern and
emit `new EntityRef(DOMAIN, owner.name())` per match.

### Test for the inverse provider

Create
`domains/<domain>/<domain>-core/src/test/java/com/naturalist/<domain>/<linkSubpackage>/<Domain><Foreign>ReferencesTest.java`
in the link entity's package so the package-private query impl is
visible. Pattern after `PlantCompoundReferencesTest` — cover:

- `domain()` returns `<Domain>Domain`
- `referenceType()` returns `<Foreign>Name.class`
- Constructor null-rejection
- A known target resolves to the expected refs (use real fixture data)
- A second known target resolves independently
- Unknown target returns empty
- `null` target returns empty
- Refs are distinguishable by `name() instanceof` for owner vs link
- `Catalog.findReferencesTo(target)` groups under `<Domain>Domain`
- `Catalog.findReferencesTo` returns empty for unknown
- `Catalog.domainsReferencing(<Foreign>Name.class)` includes
  `<Domain>Domain`

The last three exercises construct a `Catalog` via
`CatalogAssembly.from(List.of(), List.of(provider))` — confirms the
provider is correctly indexed by reference type. Same dependency
additions apply as Step 3.

---

## Step 5 — `<Domain>Linker` in `<domain>-console`

Skip if the domain has no `<domain>-console` module, or the file
already exists.

Create
`domains/<domain>/<domain>-console/src/main/java/com/naturalist/<domain>/console/catalog/<Domain>Linker.java`:

```java
package com.naturalist.

<domain>.console.catalog;

import com.naturalist.catalog.EntityRef;
import com.naturalist.catalog.EntityRefLinker;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.

<domain>.<Entity>Name;
// import every other <domain>-owned EntityName subclass

/**
 * <Domain>-domain {@link EntityRefLinker}: maps every <domain>-owned
 * {@code EntityName} type to the URL of its detail page on the
 * <domain> console. The single place to look when adding a new
 * <domain> entity or moving an existing one to a new route.
 */
@DomainService
public class <Domain>Linker implements

EntityRefLinker {

    @Override
    public String linkFor (EntityRef ref){
        return switch (ref.name()) {
            case <Entity > Name n -> "/<domain>/" + n.value();
                // one case per <domain>-owned EntityName subclass
                default -> null;
        };
    }
}
```

Enumerate **every** `EntityName` subclass owned by the domain — grep
`identifiers/src/main/java/com/naturalist/<domain>/` to find them.
Each route must match an actual controller mapping in the console;
do not invent routes.

POM updates for `<domain>-console/pom.xml`:

```xml

<dependency>
    <groupId>com.naturalist</groupId>
    <artifactId>catalog</artifactId>
</dependency>
```

The composite linker in `apps/management-console/.../catalog/` picks
the new linker up automatically via `@DomainService` discovery — no
edit to the app's composition root.

---

## Step 6 — Verify

Do **not** run Maven yourself. Stop at the source edits and tell the
user the commands to run, scoped to whatever was scaffolded:

```bash
mvn test -pl domains/<domain>/<domain>-core -am          # contribution + inverse
mvn test -pl domains/<domain>/<domain>-api  -am          # if DomainDomain added
mvn test -pl apps/management-console        -am          # smoke-test the wiring
```

The composition root in `apps/management-console/.../catalog/CatalogConfiguration.java`
collects the new `@DomainService` beans automatically; no edit there
is needed unless `DomainServiceScan`'s base-package allowlist does
not yet include the new domain (current pilot is plants + catalog;
extend the allowlist if so).

---

## Naming Conventions Summary

| Artifact             | Name                                                 | Location                                                                  |
|----------------------|------------------------------------------------------|---------------------------------------------------------------------------|
| Domain id            | `<Domain>Domain` (record)                            | `<domain>-api/src/main/java/com/naturalist/<domain>/`                     |
| Forward contribution | `<Domain>CatalogContribution`                        | `<domain>-core/src/main/java/com/naturalist/<domain>/catalog/`            |
| Forward test         | `<Domain>CatalogContributionTest`                    | `<domain>-core/src/test/java/com/naturalist/<domain>/`                    |
| Inverse provider     | `<Domain><Foreign>References` (one per foreign type) | `<domain>-core/src/main/java/com/naturalist/<domain>/catalog/`            |
| Inverse test         | `<Domain><Foreign>ReferencesTest`                    | `<domain>-core/src/test/java/com/naturalist/<domain>/<linkSubpackage>/`   |
| Console linker       | `<Domain>Linker`                                     | `<domain>-console/src/main/java/com/naturalist/<domain>/console/catalog/` |

---

## Cross-references

- `kernels/catalog/PLAN-redirect.md` — the search-and-discovery reframe.
  The whole rationale for token-based contributions, common-name
  harvesting, and the three-axis SPI lives here.
- `kernels/catalog/src/main/java/com/naturalist/catalog/` — SPI
  surfaces (`CatalogContribution`, `EntityReferences`, `EntityRefLinker`,
  `Catalog`, `EntityRef`, `DomainId`, `MatchKind`, `SearchHit`,
  `SearchResults`).
- `kernels/catalog-inmem/src/main/java/com/naturalist/catalog/inmem/CatalogAssembly.java`
  — the assembly factory tests instantiate.
- `domains/plants/plants-core/src/main/java/com/naturalist/plants/catalog/`
  — reference implementation for both forward and inverse SPIs.
- `apps/management-console/src/main/java/com/naturalist/console/catalog/`
  — composition root (`CatalogConfiguration`, `CompositeEntityRefLinker`).
- ADR-022 — typed identifiers, `EntityName` slug as cross-domain key.
- ADR-023 — open `DomainId` (each domain ships its own subtype).
- ADR-025 — `@DomainService` marker and Spring discovery.
- ADR-026 — resilience policy (`@Resilient(name = "catalog.fanout")`
  on inverse providers).
