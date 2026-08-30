package com.naturalist.catalog.rdbms;

import com.naturalist.catalog.*;
import com.naturalist.ddd.EntityName;
import com.naturalist.insects.InsectRankName;
import com.naturalist.insects.InsectSpeciesName;
import com.naturalist.persistence.test.RdbmsTestExtension;
import com.naturalist.resilience.Resilience;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class RdbmsCatalogIT {

    @RegisterExtension
    static RdbmsTestExtension rdbms = RdbmsTestExtension.shared();

    // The shared standing Postgres view (catalog_search_token) fans in insects, plants, and
    // chemistry, and RdbmsCatalogAssembly#from validates its registry against *every*
    // domain/entity_type the live view emits — not just the ones a given test cares about.
    // CatalogTestRegistry centralizes the full registry so each per-domain IT keeps its own
    // domain-specific assertions without re-widening its own inline copy every time a new
    // domain joins the view.
    private Catalog catalog() {
        return RdbmsCatalogAssembly.from(rdbms.mapper(CatalogSearchMapper.class),
                CatalogTestRegistry.domains(), CatalogTestRegistry.reconstructors(), List.of(), Resilience.noOp());
    }

    @Test
    void searchReturnsTypedInsectRefs() {
        SearchResults results = catalog().search("apis");
        assertFalse(results.isEmpty());
        assertTrue(results.stream().allMatch(h -> "insects".equals(h.target().domain().value())));
        assertTrue(results.stream().allMatch(h -> h.target().name() instanceof InsectRankName));
    }

    @Test
    void findBySlugResolvesToTypedRef() {
        var ref = catalog().findBySlug("apis-mellifera");
        assertTrue(ref.isPresent());
        assertTrue(ref.get().name() instanceof InsectSpeciesName);
    }

    @Test
    void blankInputIsEmptyAndFiresNoObservation() {
        assertTrue(catalog().search("   ").isEmpty());
    }

    @Test
    void assemblyThrowsWhenRegistryIsMissingAnEntityTypeTheViewEmits() {
        // Starts from the full helper registry and removes "insect-order", which the live
        // seeded view does emit (see CatalogSearchMapperIT#distinctDomainTypesCoversInsects)
        // — the startup validation pass must reject this registry before any query can
        // silently drop rows. Every other domain's entity types stay present so this
        // exercises the entity-type gap specifically, not a domain gap.
        Map<String, Function<String, EntityName>> incomplete = new HashMap<>(CatalogTestRegistry.reconstructors());
        incomplete.remove("insect-order");

        assertThrows(IllegalStateException.class, () -> RdbmsCatalogAssembly.from(
                rdbms.mapper(CatalogSearchMapper.class),
                CatalogTestRegistry.domains(), incomplete, List.of(), Resilience.noOp()));
    }

    @Test
    void assemblyThrowsOnDuplicateDomainSlug() {
        // Distinct rogue DomainId kept local (not in CatalogTestRegistry) — it must trip the
        // duplicate-slug check before RdbmsCatalogAssembly#from ever reaches validateRegistry.
        record RogueInsects() implements DomainId {
            @Override
            public String value() {
                return "insects";
            }
        }
        List<DomainId> domains = new ArrayList<>(CatalogTestRegistry.domains());
        domains.add(new RogueInsects());

        assertThrows(IllegalArgumentException.class, () -> RdbmsCatalogAssembly.from(
                rdbms.mapper(CatalogSearchMapper.class),
                domains, CatalogTestRegistry.reconstructors(), List.of(), Resilience.noOp()));
    }
}
