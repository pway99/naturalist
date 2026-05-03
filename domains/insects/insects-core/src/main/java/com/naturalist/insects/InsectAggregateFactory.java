package com.naturalist.insects;

import com.naturalist.insects.InsectEntityCollections.ImageCollection;
import com.naturalist.observability.Level;
import com.naturalist.observability.Observer;

import java.util.Optional;

/**
 * Name-keyed assembly of {@link InsectAggregate}. Resolves the species root by slug, then
 * fetches images by the same slug. No persistence identifiers flow between the two
 * queries (ADR-021).
 *
 * <p>Observability follows the producer/consumer rule (ADR-017): the factory validates
 * its own arguments with {@code throwWhenInvalid()} — that is the producer's boundary
 * contract — but observes the assembled aggregate with {@code observe()} — metrics only.
 * Control over what to do with a structurally invalid aggregate belongs to the consumer,
 * not the producer.
 */
class InsectAggregateFactory {

    private final Observer observer = Observer.forClass(getClass());
    private final InsectQuery.SpeciesQuery speciesQuery;
    private final InsectQuery.ImageQuery imageQuery;

    InsectAggregateFactory(InsectQuery.SpeciesQuery speciesQuery, InsectQuery.ImageQuery imageQuery) {
        observer.arguments("constructor", i -> i
                        .notNull(speciesQuery, "speciesQuery")
                        .notNull(imageQuery, "imageQuery"))
                .throwWhenInvalid();
        this.speciesQuery = speciesQuery;
        this.imageQuery = imageQuery;
    }

    Optional<InsectAggregate> buildByName(InsectSpeciesName name) {
        observer.arguments("buildByName", i -> i.entityName(name, "name")).throwWhenInvalid();
        return speciesQuery.getByName(name)
                .map(species -> {
                    ImageCollection images = imageQuery.forSpeciesName(species.name());
                    InsectAggregate aggregate = InsectAggregate.of(species, images);
                    observer.observable(aggregate, "insectAggregate").observe(Level.WARN);
                    return aggregate;
                });
    }
}
