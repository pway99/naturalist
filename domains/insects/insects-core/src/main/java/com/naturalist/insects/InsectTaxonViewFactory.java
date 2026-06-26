package com.naturalist.insects;

import com.naturalist.insects.InsectEntityCollections.FeatureCollection;
import com.naturalist.insects.InsectEntityCollections.ImageCollection;
import com.naturalist.observability.Level;
import com.naturalist.observability.Observer;

import java.util.Optional;

/**
 * Name-keyed, rank-polymorphic assembly of {@link InsectTaxonView}. The {@code
 * InsectRankName} permit determines which rank entity is resolved and which view
 * permit is constructed:
 *
 * <ul>
 *   <li>{@link InsectSpeciesName}  → {@link InsectSpeciesView}</li>
 *   <li>{@link InsectGenusName}    → {@link InsectGenusView}</li>
 *   <li>{@link InsectFamilyName}   → {@link InsectFamilyView}</li>
 *   <li>{@link InsectOrderName}      → {@link InsectOrderView}</li>
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
 * but observes the assembled view with {@code observe()} — metrics only. Control
 * over what to do with a structurally invalid view belongs to the consumer.
 */
class InsectTaxonViewFactory {

    private final Observer observer = Observer.forClass(getClass());
    private final InsectQuery.SpeciesQuery speciesQuery;
    private final InsectQuery.ImageQuery imageQuery;
    private final InsectQuery.GenusQuery genusQuery;
    private final InsectQuery.FamilyQuery familyQuery;
    private final InsectQuery.OrderQuery orderQuery;

    InsectTaxonViewFactory(InsectQuery.SpeciesQuery speciesQuery,
                           InsectQuery.ImageQuery imageQuery,
                           InsectQuery.GenusQuery genusQuery,
                           InsectQuery.FamilyQuery familyQuery,
                           InsectQuery.OrderQuery orderQuery) {
        observer.arguments("constructor", i -> i
                        .notNull(speciesQuery, "speciesQuery")
                        .notNull(imageQuery, "imageQuery")
                        .notNull(genusQuery, "genusQuery")
                        .notNull(familyQuery, "familyQuery")
                        .notNull(orderQuery, "orderQuery"))
                .throwWhenInvalid();
        this.speciesQuery = speciesQuery;
        this.imageQuery = imageQuery;
        this.genusQuery = genusQuery;
        this.familyQuery = familyQuery;
        this.orderQuery = orderQuery;
    }

    Optional<InsectTaxonView> buildByName(InsectRankName name) {
        observer.arguments("buildByName", i -> i.identifier(name, "name")).throwWhenInvalid();
        return switch (name) {
            case InsectSpeciesName speciesName -> speciesQuery.getByName(speciesName)
                    .map(species -> observe(new InsectSpeciesView(
                            species, imageQuery.forParentName(species.name()),
                            FeatureCollection.empty())));
            case InsectGenusName genusName -> genusQuery.getByName(genusName)
                    .map(genus -> observe(new InsectGenusView(
                            genus, imageQuery.forParentName(genus.name()),
                            FeatureCollection.empty())));
            case InsectFamilyName familyName -> familyQuery.getByName(familyName)
                    .map(family -> observe(new InsectFamilyView(
                            family, imageQuery.forParentName(family.name()),
                            FeatureCollection.empty())));
            case InsectOrderName on -> orderQuery.getByName(on)
                    .map(order -> observe(new InsectOrderView(
                            order, imageQuery.forParentName(order.name()),
                            FeatureCollection.empty())));
            case InsectSubspeciesName subspeciesName -> Optional.empty();
        };
    }

    private <V extends InsectTaxonView> V observe(V taxonView) {
        observer.observable(taxonView, "taxonView").observe(Level.WARN);
        return taxonView;
    }
}
