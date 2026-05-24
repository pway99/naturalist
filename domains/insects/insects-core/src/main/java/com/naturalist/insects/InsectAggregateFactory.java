package com.naturalist.insects;

import com.naturalist.insects.InsectEntityCollections.ImageCollection;
import com.naturalist.observability.Level;
import com.naturalist.observability.Observer;

import java.util.Optional;

/**
 * Name-keyed, rank-polymorphic assembly of {@link InsectAggregate}. The {@code
 * InsectRankName} permit determines which rank entity is resolved and which aggregate
 * permit is constructed:
 *
 * <ul>
 *   <li>{@link InsectSpeciesName}  → {@link InsectSpeciesAggregate}</li>
 *   <li>{@link InsectGenusName}    → {@link InsectGenusAggregate}</li>
 *   <li>{@link InsectFamilyName}   → {@link InsectFamilyAggregate}</li>
 *   <li>{@link InsectSubspeciesName} → {@link Optional#empty()} (no entity exists yet)</li>
 * </ul>
 *
 * <p>Image fetch is uniform across ranks — {@code imageQuery.forParentName(name)} returns
 * the photographs attached at that rank, since {@code InsectImage.parentName} is itself
 * polymorphic over {@code InsectRankName} (Path A). No persistence identifiers flow
 * between the entity and image queries (ADR-021).
 *
 * <p>Observability follows the producer/consumer rule (ADR-017): the factory validates
 * its own arguments with {@code throwWhenInvalid()} — the producer's boundary contract —
 * but observes the assembled aggregate with {@code observe()} — metrics only. Control
 * over what to do with a structurally invalid aggregate belongs to the consumer.
 */
class InsectAggregateFactory {

    private final Observer observer = Observer.forClass(getClass());
    private final InsectQuery.SpeciesQuery speciesQuery;
    private final InsectQuery.ImageQuery imageQuery;
    private final InsectQuery.GenusQuery genusQuery;
    private final InsectQuery.FamilyQuery familyQuery;

    InsectAggregateFactory(InsectQuery.SpeciesQuery speciesQuery,
                           InsectQuery.ImageQuery imageQuery,
                           InsectQuery.GenusQuery genusQuery,
                           InsectQuery.FamilyQuery familyQuery) {
        observer.arguments("constructor", i -> i
                        .notNull(speciesQuery, "speciesQuery")
                        .notNull(imageQuery, "imageQuery")
                        .notNull(genusQuery, "genusQuery")
                        .notNull(familyQuery, "familyQuery"))
                .throwWhenInvalid();
        this.speciesQuery = speciesQuery;
        this.imageQuery = imageQuery;
        this.genusQuery = genusQuery;
        this.familyQuery = familyQuery;
    }

    Optional<InsectAggregate> buildByName(InsectRankName name) {
        observer.arguments("buildByName", i -> i.identifier(name, "name")).throwWhenInvalid();
        return switch (name) {
            case InsectSpeciesName speciesName -> speciesQuery.getByName(speciesName)
                    .map(species -> observe(new InsectSpeciesAggregate(
                            species, imageQuery.forParentName(species.name()))));
            case InsectGenusName genusName -> genusQuery.getByName(genusName)
                    .map(genus -> observe(new InsectGenusAggregate(
                            genus, imageQuery.forParentName(genus.name()))));
            case InsectFamilyName familyName -> familyQuery.getByName(familyName)
                    .map(family -> observe(new InsectFamilyAggregate(
                            family, imageQuery.forParentName(family.name()))));
            case InsectSubspeciesName subspeciesName -> Optional.empty();
        };
    }

    private <A extends InsectAggregate> A observe(A aggregate) {
        observer.observable(aggregate, "insectAggregate").observe(Level.WARN);
        return aggregate;
    }
}
