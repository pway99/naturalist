package com.naturalist.console.atlas;

import com.naturalist.atlas.Atlas;
import com.naturalist.atlas.AtlasContribution;
import com.naturalist.atlas.EntityReferences;
import com.naturalist.atlas.inmem.AtlasAssembly;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.plants.PlantsTestContext;
import com.naturalist.plants.atlas.PlantAtlasContribution;
import com.naturalist.plants.atlas.PlantCompoundReferences;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Composition root for the assembled {@link Atlas} consumed by both the
 * cross-domain search surface ({@code SearchController}) and per-domain
 * back-reference panels ({@code ChemistryController} for M8).
 * <p>
 * The atlas is wired here — and not inside any per-domain controller —
 * because each domain's contribution and inverse provider crosses module
 * boundaries that no single domain can own. This {@code @Configuration}
 * is the single fan-in point, kept minimal so a future Lucene-backed
 * adapter swap is one factory call.
 */
@Configuration
public class AtlasConfiguration {

    @Bean
    Atlas atlas() {
        // TODO: replace with proper Spring-managed test data wiring once the
        // domain consoles stop constructing their own PlantsTestContext.
        PlantsTestContext plants = PlantsTestContext.create(NaturalistDatabase.create());

        List<AtlasContribution> forward = List.of(
                new PlantAtlasContribution(plants.plantQuery().plants())
        );
        List<EntityReferences<?>> inverse = List.of(
                new PlantCompoundReferences(plants.phytochemicalConstituentQuery().constituents())
        );
        return AtlasAssembly.from(forward, inverse);
    }
}
