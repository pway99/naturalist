package com.naturalist.console.catalog;

import com.naturalist.catalog.Catalog;
import com.naturalist.catalog.CatalogContribution;
import com.naturalist.catalog.EntityReferences;
import com.naturalist.catalog.inmem.CatalogAssembly;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.plants.PlantsTestContext;
import com.naturalist.plants.catalog.PlantCatalogContribution;
import com.naturalist.plants.catalog.PlantCompoundReferences;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Composition root for the assembled {@link Catalog} consumed by both the
 * cross-domain search surface ({@code SearchController}) and per-domain
 * back-reference panels ({@code ChemistryController} for M8).
 * <p>
 * The catalog is wired here — and not inside any per-domain controller —
 * because each domain's contribution and inverse provider crosses module
 * boundaries that no single domain can own. This {@code @Configuration}
 * is the single fan-in point, kept minimal so a future Lucene-backed
 * adapter swap is one factory call.
 */
@Configuration
public class CatalogConfiguration {

    @Bean
    Catalog catalog() {
        // TODO: replace with proper Spring-managed test data wiring once the
        // domain consoles stop constructing their own PlantsTestContext.
        PlantsTestContext plants = PlantsTestContext.create(NaturalistDatabase.create());

        List<CatalogContribution> forward = List.of(
                new PlantCatalogContribution(plants.plantQuery().plants())
        );
        List<EntityReferences<?>> inverse = List.of(
                new PlantCompoundReferences(plants.phytochemicalConstituentQuery().constituents())
        );
        return CatalogAssembly.from(forward, inverse);
    }
}
