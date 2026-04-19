package com.naturalist.insects;

import java.util.Optional;

// Claude just hacking things together to see how they work, needs updates
// this should be delegating to the factory
class InsectAggregateQueryImpl implements InsectQuery.InsectAggregateQuery {
    final InsectQuery.SpeciesQuery speciesQuery;
    final InsectQuery.ImageQuery imageQuery;

    InsectAggregateQueryImpl(InsectQuery.SpeciesQuery speciesQuery, InsectQuery.ImageQuery imageQuery) {
        this.speciesQuery = speciesQuery;
        this.imageQuery = imageQuery;
    }

    @Override
    public Optional<InsectAggregate> getByName(InsectSpeciesName name) {
        InsectSpecies species = speciesQuery.getByName(name).orElseThrow();
        // This does not work because the InsectSpeciesId is not set when inserting into the TestEntitySource
        // That is a problem that should be handled, perhapse a foreign key constraint contract similar to the UniqueConstraint..
        //
        // Note: After rest the proposed solution of using a UniqueConstraint may violate the first-principles adr
        // Null ID values in the test data are starting to apply friction to the model, consider making them not null
        // the RepositoryTests will still pass because they are explicitly ignoring the 'id' fields allowing them to
        // be defined by their associated persistent source rather than in json only
        
        //InsectEntityCollections.ImageCollection imageCollection = imageQuery.forSpeciesId(species.id());

        InsectEntityCollections.ImageCollection imageCollection = imageQuery.forSpeciesName(species.name());
        return Optional.of(InsectAggregate.of(species, imageCollection));
    }

    @Override
    public Optional<InsectAggregate> getById(InsectSpeciesId id) {
        return Optional.empty();
    }
}
