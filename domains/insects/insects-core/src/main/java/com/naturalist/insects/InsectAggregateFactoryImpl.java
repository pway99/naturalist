package com.naturalist.insects;

import com.naturalist.insects.InsectEntityCollections.ImageCollection;

import java.util.Optional;

/**
 * Name-keyed assembly of {@link InsectAggregate}. Resolves the species root by slug, then
 * fetches images by the same slug. No persistence identifiers flow between the two
 * queries (ADR-021).
 */
class InsectAggregateFactoryImpl implements InsectAggregateFactory {

    private final InsectQuery.SpeciesQuery speciesQuery;
    private final InsectQuery.ImageQuery imageQuery;

    InsectAggregateFactoryImpl(InsectQuery.SpeciesQuery speciesQuery, InsectQuery.ImageQuery imageQuery) {
        this.speciesQuery = speciesQuery;
        this.imageQuery = imageQuery;
    }

    @Override
    public Optional<InsectAggregate> buildByName(InsectSpeciesName name) {
        return speciesQuery.getByName(name)
                .map(species -> {
                    ImageCollection images = imageQuery.forSpeciesName(species.name());
                    return InsectAggregate.of(species, images);
                });
    }
}
