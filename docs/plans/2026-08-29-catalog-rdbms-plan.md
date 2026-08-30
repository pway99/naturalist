# catalog-rdbms Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a Postgres-backed `Catalog` adapter (`kernels/catalog-rdbms`) that serves always-fresh, type-ahead search over the domains' own tables via plain SQL views, with fuzzy matching.

**Architecture:** Each participating rdbms domain ships a `<domain>_catalog_token` view over its own tables (`token, slug, is_slug, domain, entity_type`); the app unions them into `catalog_search_token`. `RdbmsCatalog` queries that view (exact/prefix/fuzzy via `pg_trgm`), rebuilds a *typed* `EntityRef` using a per-row `entity_type` → `EntityName::of` registry, and delegates the storage-independent inverse direction to a `ReferenceRouting` collaborator extracted from `InMemoryCatalog`.

**Tech Stack:** Java 21, MyBatis, PostgreSQL + `pg_trgm`, JUnit 5, the `kernels/persistence` + `kernels/persistence-test` harness (standing seeded Postgres, per-test rollback).

**Spec:** [`docs/plans/2026-08-29-catalog-rdbms-design.md`](2026-08-29-catalog-rdbms-design.md)

## Global Constraints

- **Typed identifiers only.** Search results carry a concrete `EntityName` subclass via `EntityRef`; never a raw `String`/`UUID` reference across a boundary. (CLAUDE.md non-negotiables.)
- **Package-private adapter internals.** `RdbmsCatalog`, the mapper, and the row projection are package-private; consumers go through the `Catalog` interface and the public `RdbmsCatalogAssembly` factory. (Module DAG rule 5.)
- **Never weaken a gate.** The N+1 select-count gate and the rdbms mapper-select fan-out gate (`kernels/persistence-test` `nofanout`) are armed in `RdbmsTestExtension`; catalog search must be **one** query per call, never a per-row select loop. Do not suppress.
- **Kernel stays domain-agnostic.** `kernels/catalog-rdbms` depends only on `catalog`, `framework`, `identifiers`, `persistence`. It must not depend on any `domains/*` module; typed-name construction enters as `Function<String, EntityName>` values supplied at the composition root.
- **Standing test DB is a fixed fixture.** ITs assume `apps/test-db-seeder` has seeded the DB and roll back per test. Never mutate/commit seed rows.
- **Completeness gate.** Before declaring done: `mvn install -DskipTests && mvn rewrite:dryRun -Drewrite.failOnDryRunResults=true`, plus `mvn verify`.

---

### Task 1: Kernel foundation — `MatchKind.FUZZY` + extract `ReferenceRouting`

Extract the inverse-direction fan-out from `InMemoryCatalog` into a reusable `kernels/catalog` collaborator both adapters share, and append the `FUZZY` match kind.

**Files:**
- Modify: `kernels/catalog/src/main/java/com/naturalist/catalog/MatchKind.java`
- Create: `kernels/catalog/src/main/java/com/naturalist/catalog/ReferenceRouting.java`
- Modify: `kernels/catalog-inmem/src/main/java/com/naturalist/catalog/inmem/InMemoryCatalog.java`
- Create: `kernels/catalog/src/test/java/com/naturalist/catalog/ReferenceRoutingTest.java`

**Interfaces:**
- Produces: `ReferenceRouting` with
  - `ReferenceRouting(List<EntityReferences<?>> providers, Resilience resilience)`
  - `Set<DomainId> domainsReferencing(Class<? extends EntityName> referenceType)`
  - `Map<DomainId,List<EntityRef>> findReferencesTo(EntityName target)`
- Produces: `MatchKind.FUZZY` (last ordinal).
- Consumes (from existing kernel): `EntityReferences`, `EntityRef`, `DomainId`, `Resilience`, `Timeout`, `CircuitBreaker`.

- [ ] **Step 1: Write the failing test** for `ReferenceRouting` (mirror the inverse behavior currently proven inside `InMemoryCatalogTest`).

```java
package com.naturalist.catalog;

import com.naturalist.ddd.EntityName;
import com.naturalist.resilience.Resilience;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class ReferenceRoutingTest {

    private record Plants() implements DomainId { public String value() { return "plants"; } }
    private static final class Compound extends EntityName {
        private Compound(String v) { super(v); } static Compound of(String v){ return new Compound(v);} 
        protected int maxLength(){ return 96; } }

    private static EntityReferences<Compound> provider(DomainId d, EntityRef... refs) {
        return new EntityReferences<>() {
            public DomainId domain() { return d; }
            public Class<Compound> referenceType() { return Compound.class; }
            public Stream<EntityRef> referencesTo(Compound t) { return Stream.of(refs); }
        };
    }

    @Test
    void groupsReferencesByDomain() {
        DomainId plants = new Plants();
        EntityRef ref = new EntityRef(plants, Compound.of("california-pipevine"));
        ReferenceRouting routing = new ReferenceRouting(List.of(provider(plants, ref)), Resilience.noOp());

        Map<DomainId, List<EntityRef>> found = routing.findReferencesTo(Compound.of("aristolochic-acid"));

        assertEquals(Set.of(plants), found.keySet());
        assertEquals(List.of(ref), found.get(plants));
        assertEquals(Set.of(plants), routing.domainsReferencing(Compound.class));
    }

    @Test
    void nullTargetYieldsEmptyMap() {
        ReferenceRouting routing = new ReferenceRouting(List.of(), Resilience.noOp());
        assertTrue(routing.findReferencesTo(null).isEmpty());
        assertTrue(routing.domainsReferencing(null).isEmpty());
    }
}
```

- [ ] **Step 2: Run it, verify it fails**

Run: `mvn -q -pl kernels/catalog test -Dtest=ReferenceRoutingTest`
Expected: FAIL — `ReferenceRouting` does not exist.

- [ ] **Step 3: Create `ReferenceRouting`** by lifting the provider-indexing + resilience-wrapped fan-out currently in `InMemoryCatalog` (`indexProviders`, `invokeQuietly`, `invoke`, `domainsReferencing`, the body of `findReferencesTo`).

```java
package com.naturalist.catalog;

import com.naturalist.ddd.EntityName;
import com.naturalist.resilience.CircuitBreaker;
import com.naturalist.resilience.Resilience;
import com.naturalist.resilience.Timeout;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Storage-independent inverse-direction routing shared by every {@link Catalog} adapter. */
public final class ReferenceRouting {

    public static final String CATALOG_FANOUT = "catalog.fanout";

    private final Map<Class<? extends EntityName>, List<EntityReferences<?>>> providersByType;
    private final Resilience resilience;

    public ReferenceRouting(List<EntityReferences<?>> providers, Resilience resilience) {
        Map<Class<? extends EntityName>, List<EntityReferences<?>>> indexed = new LinkedHashMap<>();
        for (EntityReferences<?> p : providers) {
            indexed.computeIfAbsent(p.referenceType(), k -> new ArrayList<>()).add(p);
        }
        Map<Class<? extends EntityName>, List<EntityReferences<?>>> immutable = new LinkedHashMap<>();
        indexed.forEach((k, v) -> immutable.put(k, List.copyOf(v)));
        this.providersByType = Collections.unmodifiableMap(immutable);
        this.resilience = resilience;
    }

    public Set<DomainId> domainsReferencing(Class<? extends EntityName> referenceType) {
        if (referenceType == null) return Set.of();
        return providersByType.getOrDefault(referenceType, List.of()).stream()
                .map(EntityReferences::domain).collect(Collectors.toUnmodifiableSet());
    }

    public Map<DomainId, List<EntityRef>> findReferencesTo(EntityName target) {
        if (target == null) return Map.of();
        List<EntityReferences<?>> handlers = providersByType.getOrDefault(target.getClass(), List.of());
        Timeout timeout = resilience.timeout(CATALOG_FANOUT);
        CircuitBreaker breaker = resilience.circuitBreaker(CATALOG_FANOUT);
        Map<DomainId, List<EntityRef>> grouped = new LinkedHashMap<>();
        for (EntityReferences<?> handler : handlers) {
            List<EntityRef> refs = invokeQuietly(handler, target, timeout, breaker);
            if (!refs.isEmpty()) grouped.computeIfAbsent(handler.domain(), k -> new ArrayList<>()).addAll(refs);
        }
        Map<DomainId, List<EntityRef>> immutable = new LinkedHashMap<>();
        grouped.forEach((k, v) -> immutable.put(k, List.copyOf(v)));
        return Collections.unmodifiableMap(immutable);
    }

    private static List<EntityRef> invokeQuietly(EntityReferences<?> h, EntityName t, Timeout timeout, CircuitBreaker breaker) {
        try {
            return breaker.execute(() -> timeout.execute(() -> invoke(h, t).toList()));
        } catch (RuntimeException ignored) {
            return List.of();
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Stream<EntityRef> invoke(EntityReferences<?> h, EntityName t) {
        return ((EntityReferences) h).referencesTo(h.referenceType().cast(t));
    }
}
```

- [ ] **Step 4: Refactor `InMemoryCatalog` to delegate.** Replace its `providersByType`/`resilience` fields and the inverse-direction methods with a single `private final ReferenceRouting routing;` built in the constructor (`this.routing = new ReferenceRouting(providers, resilience);`). `domainsReferencing`/`findReferencesTo` become one-line delegations. Keep the `@Resilient(name = ReferenceRouting.CATALOG_FANOUT)` annotation on `findReferencesTo`. Delete the now-dead `indexProviders`/`invokeQuietly`/`invoke` and the `CATALOG_FANOUT` constant (use `ReferenceRouting.CATALOG_FANOUT`).

- [ ] **Step 5: Append `FUZZY` to `MatchKind`** after `PREFIX`:

```java
    /**
     * The query matched a token by substring / trigram similarity rather than a prefix — the
     * weakest signal, used by fuzzy-capable adapters (e.g. the Postgres pg_trgm adapter) to power
     * type-ahead. Ordered last.
     */
    FUZZY
```

- [ ] **Step 6: Run the affected suites**

Run: `mvn -q -pl kernels/catalog -pl kernels/catalog-inmem -am test`
Expected: PASS — `ReferenceRoutingTest` green and the existing `InMemoryCatalogTest` still green (behavior unchanged).

- [ ] **Step 7: Commit**

```bash
git add kernels/catalog kernels/catalog-inmem
git commit -m "refactor(catalog): extract ReferenceRouting; add MatchKind.FUZZY"
```

---

### Task 2: insects token view + trigram indexes

Add the per-domain searchable-token view and its `pg_trgm` indexes to the insects schema. This is the template every later domain follows.

**Files:**
- Modify: `domains/insects/insects-repository-rdbms/src/main/resources/schema/insects.sql`
- Test: `domains/insects/insects-repository-rdbms/src/test/java/com/naturalist/insects/InsectCatalogTokenViewIT.java`

**Interfaces:**
- Produces (DB): view `insect_catalog_token(token text, slug text, is_slug boolean, domain text, entity_type text)`; `entity_type ∈ {insect-order, insect-family, insect-genus, insect-species}`.

- [ ] **Step 1: Verify the common-name FK column names** (they differ per table; `insect_species_common_name` uses `species_id`). Confirm the others before writing the joins:

Run: `grep -nE "CREATE TABLE insect_(order|family|genus)_common_name" -A3 domains/insects/insects-repository-rdbms/src/main/resources/schema/insects.sql`
Use the actual `*_id` column names in Step 3's joins.

- [ ] **Step 2: Write the failing IT** proving the view exists and returns a known seeded species by slug and by common name.

```java
package com.naturalist.insects;

import com.naturalist.persistence.test.RdbmsTestExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

class InsectCatalogTokenViewIT {

    @RegisterExtension
    static RdbmsTestExtension rdbms = RdbmsTestExtension.shared();

    private int count(String sql) throws Exception {
        try (Statement st = rdbms.connection().createStatement(); ResultSet rs = st.executeQuery(sql)) {
            rs.next(); return rs.getInt(1);
        }
    }

    @Test
    void viewExposesSlugRowsForEverySpecies() throws Exception {
        assertTrue(count("SELECT count(*) FROM insect_catalog_token "
                + "WHERE is_slug AND entity_type = 'insect-species'") > 0);
    }

    @Test
    void viewExposesFourEntityTypes() throws Exception {
        assertEquals(4, count("SELECT count(DISTINCT entity_type) FROM insect_catalog_token"));
    }
}
```

- [ ] **Step 3: Run it, verify it fails**

Run: `mvn -q -pl domains/insects/insects-repository-rdbms verify -Dit.test=InsectCatalogTokenViewIT`
Expected: FAIL — relation `insect_catalog_token` does not exist.

- [ ] **Step 4: Add `CASCADE` to the drops for the referenced tables** so the dependent view drops cleanly on re-seed. At the top of `insects.sql`, ensure the extension exists (idempotent), and change the `DROP TABLE IF EXISTS` lines for `insect_order`, `insect_family`, `insect_genus`, `insect_species` and their `*_common_name` tables to end with ` CASCADE`.

```sql
CREATE EXTENSION IF NOT EXISTS pg_trgm;
-- e.g.
DROP TABLE IF EXISTS insect_species_common_name CASCADE;
DROP TABLE IF EXISTS insect_species CASCADE;
-- …order/family/genus + their common_name tables likewise
```

- [ ] **Step 5: Append the view + trigram indexes** at the end of `insects.sql` (use the FK columns confirmed in Step 1):

```sql
CREATE OR REPLACE VIEW insect_catalog_token AS
      SELECT o.name AS token, o.name AS slug, true  AS is_slug, 'insects' AS domain, 'insect-order'   AS entity_type FROM insect_order o
UNION ALL SELECT o.taxonomic_order, o.name, false, 'insects', 'insect-order'   FROM insect_order o
UNION ALL SELECT cn.label, o.name, false, 'insects', 'insect-order'   FROM insect_order_common_name  cn JOIN insect_order  o ON cn.order_id  = o.id
UNION ALL SELECT f.name, f.name, true,  'insects', 'insect-family'  FROM insect_family f
UNION ALL SELECT f.taxonomic_family, f.name, false, 'insects', 'insect-family'  FROM insect_family f
UNION ALL SELECT cn.label, f.name, false, 'insects', 'insect-family'  FROM insect_family_common_name cn JOIN insect_family f ON cn.family_id = f.id
UNION ALL SELECT g.name, g.name, true,  'insects', 'insect-genus'   FROM insect_genus g
UNION ALL SELECT g.taxonomic_genus, g.name, false, 'insects', 'insect-genus'   FROM insect_genus g
UNION ALL SELECT cn.label, g.name, false, 'insects', 'insect-genus'   FROM insect_genus_common_name  cn JOIN insect_genus  g ON cn.genus_id  = g.id
UNION ALL SELECT s.name, s.name, true,  'insects', 'insect-species' FROM insect_species s
UNION ALL SELECT s.epithet, s.name, false, 'insects', 'insect-species' FROM insect_species s
UNION ALL SELECT cn.label, s.name, false, 'insects', 'insect-species' FROM insect_species_common_name cn JOIN insect_species s ON cn.species_id = s.id;

CREATE INDEX IF NOT EXISTS insect_order_name_trgm   ON insect_order   USING gin (lower(name) gin_trgm_ops);
CREATE INDEX IF NOT EXISTS insect_family_name_trgm  ON insect_family  USING gin (lower(name) gin_trgm_ops);
CREATE INDEX IF NOT EXISTS insect_genus_name_trgm   ON insect_genus   USING gin (lower(name) gin_trgm_ops);
CREATE INDEX IF NOT EXISTS insect_species_name_trgm ON insect_species USING gin (lower(name) gin_trgm_ops);
CREATE INDEX IF NOT EXISTS insect_species_cn_trgm   ON insect_species_common_name USING gin (lower(label) gin_trgm_ops);
```

- [ ] **Step 6: Re-seed insects and run the IT**

```bash
mvn -q -pl apps/test-db-seeder exec:java -Dexec.mainClass=com.naturalist.seeder.TestDbSeeder -Dexec.args="insects"
mvn -q -pl domains/insects/insects-repository-rdbms verify -Dit.test=InsectCatalogTokenViewIT
```
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add domains/insects/insects-repository-rdbms
git commit -m "feat(insects): catalog token view + pg_trgm indexes"
```

---

### Task 3: app fan-in view + `pg_trgm` + seeder wiring

Create the composition-root `catalog_search_token` view over the per-domain views, and a seeder step that applies it after all domains.

**Files:**
- Create: `apps/test-db-seeder/src/main/resources/schema/catalog.sql`
- Create: `apps/test-db-seeder/src/main/java/com/naturalist/seeder/CatalogSeeding.java`
- Modify: `apps/test-db-seeder/src/main/java/com/naturalist/seeder/TestDbSeeder.java`
- Test: `apps/test-db-seeder/src/test/java/com/naturalist/seeder/CatalogSearchViewIT.java` (or a small IT under insects-rdbms if the seeder module has no IT harness — see Step 1)

**Interfaces:**
- Produces (DB): view `catalog_search_token(token, slug, is_slug, domain, entity_type)` = union of the per-domain views.
- Produces: `CatalogSeeding.apply(DataSource)` — applies `schema/catalog.sql` last.

- [ ] **Step 1: Confirm the seeder module's test setup.** The seeder has `persistence-test` available transitively; if it has no failsafe config, place the IT in `domains/insects/insects-repository-rdbms` instead and name it `CatalogSearchViewIT`. Decide based on:

Run: `grep -l failsafe apps/test-db-seeder/pom.xml`

- [ ] **Step 2: Write the failing IT** — the app view unions insects (only participant so far):

```java
// package/location per Step 1; uses RdbmsTestExtension.connection()
@Test
void fanInViewReturnsInsectRows() throws Exception {
    assertTrue(count("SELECT count(*) FROM catalog_search_token WHERE domain = 'insects'") > 0);
}
```

- [ ] **Step 3: Run it, verify it fails**

Expected: FAIL — relation `catalog_search_token` does not exist.

- [ ] **Step 4: Create `schema/catalog.sql`** (extension first; union only existing per-domain views — insects for now):

```sql
CREATE EXTENSION IF NOT EXISTS pg_trgm;

DROP VIEW IF EXISTS catalog_search_token;
CREATE VIEW catalog_search_token AS
    SELECT token, slug, is_slug, domain, entity_type FROM insect_catalog_token;
```

- [ ] **Step 5: Create `CatalogSeeding`**:

```java
package com.naturalist.seeder;

import javax.sql.DataSource;

/** Applies the app-level catalog fan-in view. MUST run after every participating domain is seeded. */
final class CatalogSeeding {
    private CatalogSeeding() {}
    static void apply(DataSource dataSource) {
        Seeding.applySchema(dataSource, "schema/catalog.sql");
    }
}
```

- [ ] **Step 6: Wire it last in `TestDbSeeder.main`** (after all domain seeders, unconditionally — the fan-in view is cheap and must reflect whatever was reseeded):

```java
        CatalogSeeding.apply(dataSource);   // always last: rebuilds the fan-in view over per-domain views
```

- [ ] **Step 7: Full re-seed and run the IT**

```bash
mvn -q -pl apps/test-db-seeder exec:java -Dexec.mainClass=com.naturalist.seeder.TestDbSeeder
mvn -q -pl <module-from-step-1> verify -Dit.test=CatalogSearchViewIT
```
Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add apps/test-db-seeder
git commit -m "feat(seeder): app-level catalog_search_token fan-in view"
```

---

### Task 4: `kernels/catalog-rdbms` module — mapper + row + search SQL

Stand up the new module with the MyBatis mapper and the row projection, proven against the seeded fan-in view.

**Files:**
- Create: `kernels/catalog-rdbms/pom.xml`
- Create: `kernels/catalog-rdbms/src/main/java/com/naturalist/catalog/rdbms/CatalogTokenRow.java`
- Create: `kernels/catalog-rdbms/src/main/java/com/naturalist/catalog/rdbms/CatalogSearchMapper.java`
- Modify: `kernels/pom.xml` (add `<module>catalog-rdbms</module>`)
- Test: `kernels/catalog-rdbms/src/test/java/com/naturalist/catalog/rdbms/CatalogSearchMapperIT.java`

**Interfaces:**
- Produces: `CatalogSearchMapper` with
  - `List<CatalogTokenRow> search(@Param("q") String q)`
  - `CatalogTokenRow findBySlug(@Param("slug") String slug)`
  - `List<CatalogTokenRow> distinctDomainTypes()`
- Produces: `CatalogTokenRow` package-private fields `slug, domain, entityType, matchedToken, kind` (String) + `sim` (Double).

- [ ] **Step 1: Create `pom.xml`** (parent `kernels`; deps `catalog`, `framework`, `identifiers`, `persistence`; test-scope `persistence-test` + `identifiers-test`; failsafe plugin as in the chemistry rdbms pom).

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.naturalist</groupId><artifactId>kernels</artifactId><version>1.0.0-SNAPSHOT</version>
    </parent>
    <artifactId>catalog-rdbms</artifactId>
    <name>kernels :: catalog-rdbms</name>
    <dependencies>
        <dependency><groupId>com.naturalist</groupId><artifactId>catalog</artifactId></dependency>
        <dependency><groupId>com.naturalist</groupId><artifactId>framework</artifactId></dependency>
        <dependency><groupId>com.naturalist</groupId><artifactId>identifiers</artifactId></dependency>
        <dependency><groupId>com.naturalist</groupId><artifactId>persistence</artifactId></dependency>
        <dependency><groupId>com.naturalist</groupId><artifactId>persistence-test</artifactId><scope>test</scope></dependency>
        <dependency><groupId>com.naturalist</groupId><artifactId>identifiers-test</artifactId><scope>test</scope></dependency>
    </dependencies>
    <build><plugins>
        <plugin><groupId>org.apache.maven.plugins</groupId><artifactId>maven-failsafe-plugin</artifactId>
            <executions><execution><goals><goal>integration-test</goal><goal>verify</goal></goals></execution></executions>
        </plugin>
    </plugins></build>
</project>
```

Add `<module>catalog-rdbms</module>` to `kernels/pom.xml`.

- [ ] **Step 2: Create `CatalogTokenRow`** (plain projection, not a `Dbo`; MyBatis populates package-private fields; underscore→camel is ON):

```java
package com.naturalist.catalog.rdbms;

/** Query projection from catalog_search_token. Not a persisted Dbo — read-only search output. */
final class CatalogTokenRow {
    String slug;
    String domain;
    String entityType;   // entity_type
    String matchedToken; // matched_token (null for findBySlug/distinct)
    String kind;         // EXACT_SLUG | EXACT_TOKEN | PREFIX | FUZZY (null for findBySlug/distinct)
    Double sim;          // trigram similarity (null for findBySlug/distinct)
}
```

- [ ] **Step 3: Write the failing IT** against a seeded slug (pick a stable one from the insects seed, e.g. a species slug):

```java
package com.naturalist.catalog.rdbms;

import com.naturalist.persistence.test.RdbmsTestExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CatalogSearchMapperIT {

    @RegisterExtension
    static RdbmsTestExtension rdbms = RdbmsTestExtension.shared();

    private CatalogSearchMapper mapper() { return rdbms.mapper(CatalogSearchMapper.class); }

    @Test
    void substringSearchReturnsClassifiedRows() {
        List<CatalogTokenRow> rows = mapper().search("pipe"); // matches e.g. pipevine common names
        assertFalse(rows.isEmpty());
        assertTrue(rows.stream().allMatch(r -> r.slug != null && r.domain != null && r.entityType != null));
        assertTrue(rows.stream().allMatch(r ->
                List.of("EXACT_SLUG","EXACT_TOKEN","PREFIX","FUZZY").contains(r.kind)));
    }

    @Test
    void distinctDomainTypesCoversInsects() {
        assertTrue(mapper().distinctDomainTypes().stream().anyMatch(r -> "insects".equals(r.domain)));
    }
}
```

- [ ] **Step 4: Run it, verify it fails**

Run: `mvn -q -pl kernels/catalog-rdbms -am verify -Dit.test=CatalogSearchMapperIT`
Expected: FAIL — mapper does not exist.

- [ ] **Step 5: Create `CatalogSearchMapper`.** One query classifies all tiers; the `ILIKE '%q%'` predicate is trgm-GIN-accelerated and captures exact/prefix/fuzzy as substrings.

```java
package com.naturalist.catalog.rdbms;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
interface CatalogSearchMapper {

    @Select("""
        SELECT slug, domain, entity_type, token AS matched_token,
               CASE
                 WHEN lower(token) = lower(#{q}) AND is_slug THEN 'EXACT_SLUG'
                 WHEN lower(token) = lower(#{q})             THEN 'EXACT_TOKEN'
                 WHEN lower(token) LIKE lower(#{q}) || '%'   THEN 'PREFIX'
                 ELSE 'FUZZY'
               END AS kind,
               similarity(lower(token), lower(#{q})) AS sim
        FROM catalog_search_token
        WHERE token ILIKE '%' || #{q} || '%'
        """)
    List<CatalogTokenRow> search(@Param("q") String q);

    @Select("""
        SELECT slug, domain, entity_type
        FROM catalog_search_token
        WHERE is_slug AND lower(slug) = lower(#{slug})
        LIMIT 1
        """)
    CatalogTokenRow findBySlug(@Param("slug") String slug);

    @Select("SELECT DISTINCT domain, entity_type FROM catalog_search_token")
    List<CatalogTokenRow> distinctDomainTypes();
}
```

- [ ] **Step 6: Run the IT, verify it passes**

Run: `mvn -q -pl kernels/catalog-rdbms -am verify -Dit.test=CatalogSearchMapperIT`
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add kernels/pom.xml kernels/catalog-rdbms
git commit -m "feat(catalog-rdbms): module skeleton, search mapper + row projection"
```

---

### Task 5: `RdbmsCatalog` + `RdbmsCatalogAssembly`

Wire the mapper, the `entity_type`→`EntityName::of` registry, `ReferenceRouting`, `MatchKind` mapping, dedup/ordering, and the unresolved-search observation into a full `Catalog`.

**Files:**
- Create: `kernels/catalog-rdbms/src/main/java/com/naturalist/catalog/rdbms/RdbmsCatalog.java`
- Create: `kernels/catalog-rdbms/src/main/java/com/naturalist/catalog/rdbms/RdbmsCatalogAssembly.java`
- Test: `kernels/catalog-rdbms/src/test/java/com/naturalist/catalog/rdbms/RdbmsCatalogIT.java`

**Interfaces:**
- Consumes: `CatalogSearchMapper`, `ReferenceRouting` (Task 1), `MatchKind.FUZZY` (Task 1).
- Produces: `RdbmsCatalogAssembly.from(CatalogSearchMapper mapper, List<DomainId> domains, Map<String,Function<String,EntityName>> nameReconstructors, List<EntityReferences<?>> providers, Resilience resilience) -> Catalog`.

- [ ] **Step 1: Write the failing IT** (insects-only registry) exercising the four `Catalog` behaviors:

```java
package com.naturalist.catalog.rdbms;

import com.naturalist.catalog.*;
import com.naturalist.ddd.EntityName;
import com.naturalist.insects.*;
import com.naturalist.persistence.test.RdbmsTestExtension;
import com.naturalist.resilience.Resilience;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class RdbmsCatalogIT {

    @RegisterExtension
    static RdbmsTestExtension rdbms = RdbmsTestExtension.shared();

    private Catalog catalog() {
        Map<String, Function<String, EntityName>> reconstructors = Map.of(
                "insect-order",   InsectOrderName::of,
                "insect-family",  InsectFamilyName::of,
                "insect-genus",   InsectGenusName::of,
                "insect-species", InsectSpeciesName::of);
        return RdbmsCatalogAssembly.from(rdbms.mapper(CatalogSearchMapper.class),
                List.of(new InsectsDomain()), reconstructors, List.of(), Resilience.noOp());
    }

    @Test
    void searchReturnsTypedInsectRefs() {
        SearchResults results = catalog().search("pipe");
        assertFalse(results.isEmpty());
        assertTrue(results.stream().allMatch(h -> "insects".equals(h.target().domain().value())));
        assertTrue(results.stream().allMatch(h -> h.target().name() instanceof InsectRankName));
    }

    @Test
    void findBySlugResolvesToTypedRef() {
        // replace with a real seeded species slug
        var ref = catalog().findBySlug("pipevine-swallowtail");
        assertTrue(ref.isEmpty() || ref.get().name() instanceof InsectSpeciesName);
    }

    @Test
    void blankInputIsEmptyAndFiresNoObservation() {
        assertTrue(catalog().search("   ").isEmpty());
    }
}
```

- [ ] **Step 2: Run it, verify it fails**

Run: `mvn -q -pl kernels/catalog-rdbms -am verify -Dit.test=RdbmsCatalogIT`
Expected: FAIL — `RdbmsCatalogAssembly` does not exist.

- [ ] **Step 3: Create `RdbmsCatalog`** (package-private; forward via mapper, inverse via routing; dedup/order mirrors `InMemoryCatalog.promote`):

```java
package com.naturalist.catalog.rdbms;

import com.naturalist.catalog.*;
import com.naturalist.ddd.EntityName;
import com.naturalist.observability.Level;
import com.naturalist.observability.Observer;
import com.naturalist.resilience.Resilient;

import java.util.*;
import java.util.function.Function;

final class RdbmsCatalog implements Catalog {

    private static final Observer observer = Observer.forClass(RdbmsCatalog.class);

    private final CatalogSearchMapper mapper;
    private final Map<String, com.naturalist.catalog.DomainId> domainsBySlug;
    private final Map<String, Function<String, EntityName>> reconstructors;
    private final ReferenceRouting routing;

    RdbmsCatalog(CatalogSearchMapper mapper,
                 Map<String, com.naturalist.catalog.DomainId> domainsBySlug,
                 Map<String, Function<String, EntityName>> reconstructors,
                 ReferenceRouting routing) {
        this.mapper = mapper;
        this.domainsBySlug = Map.copyOf(domainsBySlug);
        this.reconstructors = Map.copyOf(reconstructors);
        this.routing = routing;
    }

    @Override
    public SearchResults search(String text) {
        if (text == null || text.isBlank() || text.trim().length() < 2) return SearchResults.empty();
        String q = text.trim();
        List<CatalogTokenRow> rows = mapper.search(q);

        Map<EntityRef, MatchKind> bestKind = new LinkedHashMap<>();
        Map<EntityRef, String> bestToken = new LinkedHashMap<>();
        Map<EntityRef, Double> bestSim = new LinkedHashMap<>();
        for (CatalogTokenRow row : rows) {
            EntityRef ref = toRef(row);
            if (ref == null) continue;
            MatchKind kind = MatchKind.valueOf(row.kind);
            MatchKind prev = bestKind.get(ref);
            if (prev == null || kind.ordinal() < prev.ordinal()) {
                bestKind.put(ref, kind);
                bestToken.put(ref, row.matchedToken);
                bestSim.put(ref, row.sim == null ? 0.0 : row.sim);
            }
        }

        if (bestKind.isEmpty()) {
            observer.observation(new UnresolvedSearchObservation(q.toLowerCase())).observe(Level.INFO);
            return SearchResults.empty();
        }

        List<SearchHit> hits = bestKind.entrySet().stream()
                .map(e -> new SearchHit(e.getKey(), bestToken.get(e.getKey()), e.getValue()))
                .sorted(Comparator
                        .comparingInt((SearchHit h) -> h.kind().ordinal())
                        .thenComparing(h -> -bestSim.getOrDefault(h.target(), 0.0))
                        .thenComparing(h -> h.target().name().value()))
                .toList();
        return SearchResults.of(hits);
    }

    @Override
    public Optional<EntityRef> findBySlug(String slug) {
        if (slug == null || slug.isBlank()) return Optional.empty();
        return Optional.ofNullable(mapper.findBySlug(slug.trim())).map(this::toRef).filter(Objects::nonNull);
    }

    @Override
    public Set<com.naturalist.catalog.DomainId> domainsReferencing(Class<? extends EntityName> referenceType) {
        return routing.domainsReferencing(referenceType);
    }

    @Override
    @Resilient(name = ReferenceRouting.CATALOG_FANOUT)
    public Map<com.naturalist.catalog.DomainId, List<EntityRef>> findReferencesTo(EntityName target) {
        return routing.findReferencesTo(target);
    }

    private EntityRef toRef(CatalogTokenRow row) {
        com.naturalist.catalog.DomainId domain = domainsBySlug.get(row.domain);
        Function<String, EntityName> fn = reconstructors.get(row.entityType);
        if (domain == null || fn == null) {
            observer.arguments("toRef", i -> i
                    .notNull(domain, "domain[" + row.domain + "]")
                    .notNull(fn, "reconstructor[" + row.entityType + "]"));
            return null; // defensive; startup validation should prevent this
        }
        return new EntityRef(domain, fn.apply(row.slug));
    }
}
```

- [ ] **Step 4: Create `RdbmsCatalogAssembly`** (public factory; builds `domainsBySlug`, `ReferenceRouting`, and runs the startup validation pass):

```java
package com.naturalist.catalog.rdbms;

import com.naturalist.catalog.*;
import com.naturalist.ddd.EntityName;
import com.naturalist.resilience.Resilience;

import java.util.*;
import java.util.function.Function;

/** Composition-root entry point for the Postgres-backed {@link Catalog}. */
public final class RdbmsCatalogAssembly {

    private RdbmsCatalogAssembly() {}

    public static Catalog from(CatalogSearchMapper mapper,
                               List<DomainId> domains,
                               Map<String, Function<String, EntityName>> nameReconstructors,
                               List<EntityReferences<?>> providers,
                               Resilience resilience) {
        Map<String, DomainId> bySlug = new LinkedHashMap<>();
        for (DomainId d : domains) {
            DomainId prev = bySlug.putIfAbsent(d.value(), d);
            if (prev != null && !prev.equals(d)) {
                throw new IllegalArgumentException("Duplicate domain slug \"" + d.value() + "\"");
            }
        }
        validateRegistry(mapper, bySlug, nameReconstructors);
        return new RdbmsCatalog(mapper, bySlug, nameReconstructors,
                new ReferenceRouting(providers, resilience));
    }

    /** Fail fast: every (domain, entity_type) the view can emit must resolve in the registries. */
    private static void validateRegistry(CatalogSearchMapper mapper,
                                         Map<String, DomainId> domains,
                                         Map<String, Function<String, EntityName>> reconstructors) {
        for (CatalogTokenRow row : mapper.distinctDomainTypes()) {
            if (!domains.containsKey(row.domain)) {
                throw new IllegalStateException("catalog view emits domain '" + row.domain
                        + "' with no registered DomainId");
            }
            if (!reconstructors.containsKey(row.entityType)) {
                throw new IllegalStateException("catalog view emits entity_type '" + row.entityType
                        + "' with no registered EntityName reconstructor");
            }
        }
    }
}
```

- [ ] **Step 5: Run the IT, verify it passes** (fix the `findBySlug` slug in the test to a real seeded species slug first — confirm with `grep -m1 '"name"' domains/insects/*.json` or the seed source).

Run: `mvn -q -pl kernels/catalog-rdbms -am verify -Dit.test=RdbmsCatalogIT`
Expected: PASS.

- [ ] **Step 6: Confirm the fan-out gate is satisfied** — search is a single mapper call; the IT ran under `RdbmsTestExtension` with the gate armed and did not raise `RepeatedSelectException`. No `@AllowRepeatedSelect` anywhere.

- [ ] **Step 7: Commit**

```bash
git add kernels/catalog-rdbms
git commit -m "feat(catalog-rdbms): RdbmsCatalog + assembly with typed-name reconstruction"
```

---

### Task 6: plants participation

Add plants to the token views, the fan-in view, and prove typed plant refs.

**Files:**
- Modify: `domains/plants/plants-repository-rdbms/src/main/resources/schema/plants.sql`
- Modify: `apps/test-db-seeder/src/main/resources/schema/catalog.sql`
- Test: `kernels/catalog-rdbms/src/test/java/com/naturalist/catalog/rdbms/RdbmsCatalogPlantsIT.java`

**Interfaces:**
- Produces (DB): view `plant_catalog_token(...)`; `entity_type ∈ {plant-order, plant-family, plant-genus, plant-species}`.
- Consumes: `PlantOrderName::of`, `PlantFamilyName::of`, `PlantGenusName::of`, `PlantSpeciesName::of`.

- [ ] **Step 1: Write the failing IT** (plants registry) — mirror `RdbmsCatalogIT` with a seeded plant slug and assert `name() instanceof PlantSpeciesName` for a species hit.

```java
// registry:
Map.of("plant-order", PlantOrderName::of, "plant-family", PlantFamilyName::of,
       "plant-genus", PlantGenusName::of, "plant-species", PlantSpeciesName::of)
// domains: List.of(new PlantsDomain())
// assert search("<seeded plant token>") returns hits all in domain "plants"
```

- [ ] **Step 2: Run it, verify it fails** (`plant_catalog_token` missing).

Run: `mvn -q -pl kernels/catalog-rdbms -am verify -Dit.test=RdbmsCatalogPlantsIT`
Expected: FAIL.

- [ ] **Step 3: Confirm plant common-name FK columns** (species uses a `*_id`; the `label` column was confirmed present):

Run: `grep -nE "CREATE TABLE plant_(order|family|genus|species)_common_name" -A3 domains/plants/plants-repository-rdbms/src/main/resources/schema/plants.sql`

- [ ] **Step 4: Add `pg_trgm`, `CASCADE` drops, the view, and indexes** to `plants.sql` (same shape as insects; genus scientific column is `taxonomic_genus`):

```sql
CREATE EXTENSION IF NOT EXISTS pg_trgm;   -- at top; make the referenced DROP TABLEs CASCADE

CREATE OR REPLACE VIEW plant_catalog_token AS
      SELECT o.name, o.name, true,  'plants', 'plant-order'   FROM plant_order o
UNION ALL SELECT o.taxonomic_order, o.name, false, 'plants', 'plant-order'   FROM plant_order o
UNION ALL SELECT cn.label, o.name, false, 'plants', 'plant-order'   FROM plant_order_common_name  cn JOIN plant_order  o ON cn.order_id  = o.id
UNION ALL SELECT f.name, f.name, true,  'plants', 'plant-family'  FROM plant_family f
UNION ALL SELECT f.taxonomic_family, f.name, false, 'plants', 'plant-family'  FROM plant_family f
UNION ALL SELECT cn.label, f.name, false, 'plants', 'plant-family'  FROM plant_family_common_name cn JOIN plant_family f ON cn.family_id = f.id
UNION ALL SELECT g.name, g.name, true,  'plants', 'plant-genus'   FROM plant_genus g
UNION ALL SELECT g.taxonomic_genus, g.name, false, 'plants', 'plant-genus'   FROM plant_genus g
UNION ALL SELECT cn.label, g.name, false, 'plants', 'plant-genus'   FROM plant_genus_common_name  cn JOIN plant_genus  g ON cn.genus_id  = g.id
UNION ALL SELECT s.name, s.name, true,  'plants', 'plant-species' FROM plant_species s
UNION ALL SELECT s.epithet, s.name, false, 'plants', 'plant-species' FROM plant_species s
UNION ALL SELECT cn.label, s.name, false, 'plants', 'plant-species' FROM plant_species_common_name cn JOIN plant_species s ON cn.species_id = s.id;

CREATE INDEX IF NOT EXISTS plant_order_name_trgm   ON plant_order   USING gin (lower(name) gin_trgm_ops);
CREATE INDEX IF NOT EXISTS plant_family_name_trgm  ON plant_family  USING gin (lower(name) gin_trgm_ops);
CREATE INDEX IF NOT EXISTS plant_genus_name_trgm   ON plant_genus   USING gin (lower(name) gin_trgm_ops);
CREATE INDEX IF NOT EXISTS plant_species_name_trgm ON plant_species USING gin (lower(name) gin_trgm_ops);
CREATE INDEX IF NOT EXISTS plant_species_cn_trgm   ON plant_species_common_name USING gin (lower(label) gin_trgm_ops);
```

- [ ] **Step 5: Add plants to `catalog.sql`**:

```sql
CREATE VIEW catalog_search_token AS
          SELECT token, slug, is_slug, domain, entity_type FROM insect_catalog_token
UNION ALL SELECT token, slug, is_slug, domain, entity_type FROM plant_catalog_token;
```

- [ ] **Step 6: Re-seed plants + catalog, run the IT**

```bash
mvn -q -pl apps/test-db-seeder exec:java -Dexec.mainClass=com.naturalist.seeder.TestDbSeeder -Dexec.args="plants"
mvn -q -pl apps/test-db-seeder exec:java -Dexec.mainClass=com.naturalist.seeder.TestDbSeeder   # rebuild fan-in
mvn -q -pl kernels/catalog-rdbms -am verify -Dit.test=RdbmsCatalogPlantsIT
```
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add domains/plants/plants-repository-rdbms apps/test-db-seeder kernels/catalog-rdbms
git commit -m "feat(plants): catalog token view + rdbms catalog participation"
```

---

### Task 7: chemistry participation

Add chemistry (element + compound) to the token views and fan-in.

**Files:**
- Modify: `domains/chemistry/chemistry-repository-rdbms/src/main/resources/schema/chemistry.sql`
- Modify: `apps/test-db-seeder/src/main/resources/schema/catalog.sql`
- Test: `kernels/catalog-rdbms/src/test/java/com/naturalist/catalog/rdbms/RdbmsCatalogChemistryIT.java`

**Interfaces:**
- Produces (DB): view `chemistry_catalog_token(...)`; `entity_type ∈ {element, compound}`.
- Consumes: `ElementName::of`, `CompoundName::of`.

- [ ] **Step 1: Write the failing IT** (chemistry registry `Map.of("element", ElementName::of, "compound", CompoundName::of)`, domains `List.of(new ChemistryDomain())`); assert a compound search returns a hit whose `name() instanceof CompoundName`.

- [ ] **Step 2: Run it, verify it fails** (`chemistry_catalog_token` missing).

Run: `mvn -q -pl kernels/catalog-rdbms -am verify -Dit.test=RdbmsCatalogChemistryIT`
Expected: FAIL.

- [ ] **Step 3: Add `pg_trgm`, `CASCADE` drops for `element`/`compound`, the view, indexes** to `chemistry.sql` (element: `name` slug + `symbol`; compound: `name` slug + `common_name`):

```sql
CREATE EXTENSION IF NOT EXISTS pg_trgm;   -- at top; make element/compound DROP TABLEs CASCADE

CREATE OR REPLACE VIEW chemistry_catalog_token AS
      SELECT name,   name, true,  'chemistry', 'element'  FROM element
UNION ALL SELECT symbol, name, false, 'chemistry', 'element'  FROM element
UNION ALL SELECT name,        name, true,  'chemistry', 'compound' FROM compound
UNION ALL SELECT common_name, name, false, 'chemistry', 'compound' FROM compound;

CREATE INDEX IF NOT EXISTS element_name_trgm    ON element  USING gin (lower(name) gin_trgm_ops);
CREATE INDEX IF NOT EXISTS compound_name_trgm   ON compound USING gin (lower(name) gin_trgm_ops);
CREATE INDEX IF NOT EXISTS compound_common_trgm ON compound USING gin (lower(common_name) gin_trgm_ops);
```

- [ ] **Step 4: Add chemistry to `catalog.sql`**:

```sql
UNION ALL SELECT token, slug, is_slug, domain, entity_type FROM chemistry_catalog_token
```

- [ ] **Step 5: Re-seed chemistry + catalog, run the IT**

```bash
mvn -q -pl apps/test-db-seeder exec:java -Dexec.mainClass=com.naturalist.seeder.TestDbSeeder -Dexec.args="chemistry"
mvn -q -pl apps/test-db-seeder exec:java -Dexec.mainClass=com.naturalist.seeder.TestDbSeeder   # rebuild fan-in
mvn -q -pl kernels/catalog-rdbms -am verify -Dit.test=RdbmsCatalogChemistryIT
```
Expected: PASS.

- [ ] **Step 6: Full completeness gate**

```bash
mvn install -DskipTests && mvn rewrite:dryRun -Drewrite.failOnDryRunResults=true
mvn verify
```
Expected: BUILD SUCCESS; no rewrite results.

- [ ] **Step 7: Commit**

```bash
git add domains/chemistry/chemistry-repository-rdbms apps/test-db-seeder kernels/catalog-rdbms
git commit -m "feat(chemistry): catalog token view + rdbms catalog participation"
```

---

## Notes for the executor

- **Element symbols are short.** A 1–2 char `symbol` token (e.g. `Fe`) won't use the trigram GIN index (needs ≥3 chars) — Postgres seq-scans it, which is fine at element-table scale. `RdbmsCatalog.search` already rejects input `< 2` chars.
- **`ILIKE` metacharacters.** If a query may contain `%` or `_`, escape them before the mapper call (they are `ILIKE` wildcards). Seed tokens are kebab/alpha, so this is a hardening step, not a correctness blocker for the ITs.
- **Single-domain reseed drops the fan-in view** (CASCADE through the per-domain view). Always run the seeder with no args, or re-run it once more with no args, to rebuild `catalog_search_token` after a scoped reseed — the Task 6/7 command blocks already do this.
- **Discriminator strings are a contract** shared between each `*.sql` view and each IT/app registry. Keep them identical; the assembly's `validateRegistry` turns a drift into a fail-fast at startup.
