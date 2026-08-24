package com.naturalist.plants;

import com.naturalist.data.EntityQuery;
import com.naturalist.naturalist.NaturalistName;
import com.naturalist.observation.OrganismImage;
import com.naturalist.observation.OrganismObservation;

import java.util.Optional;
import java.util.Set;
import com.naturalist.plants.PlantEntityCollections.SpeciesCollection;
import com.naturalist.plants.PlantEntityCollections.FamilyCollection;
import com.naturalist.plants.PlantEntityCollections.GenusCollection;
import com.naturalist.plants.PlantEntityCollections.ImageCollection;

/**
 * Namespace query for the plants top-level sub-context — the single
 * discoverable entry point for reading plant catalog data.
 *
 * <p>Nested queries scope to a single entity each:
 * <ul>
 *   <li>{@link SpeciesQuery} — {@link PlantSpecies} entities.</li>
 *   <li>{@link FamilyQuery} — {@link PlantFamily} entities.</li>
 *   <li>{@link GenusQuery} — {@link PlantGenus} entities.</li>
 * </ul>
 *
 * <p><b>Usage:</b>
 * <pre>{@code
 * plantQuery.species().getByName(plantName);
 * plantQuery.species().findPage(PageRequest.console(0));
 * plantQuery.families().getByName(familyName);
 * plantQuery.genera().getByName(genusName);
 * plantQuery.genera().forFamilyName(familyName);   // genera under a family
 * }</pre>
 */
public interface PlantQuery {

    SpeciesQuery species();

    OrderQuery orders();

    FamilyQuery families();

    GenusQuery genera();

    /**
     * The composed {@link Plant} read model for a taxon at the given rank — the rank
     * record plus its resolved ancestry spine (later chunks fold in features, children,
     * role, images, and species extras). Empty when no entity exists at that rank name.
     */
    Optional<Plant> getByName(PlantRankName rankName);

    interface SpeciesQuery extends EntityQuery<PlantSpeciesName, PlantSpecies, SpeciesCollection> {

        /**
         * Species under a genus, joined on the species' typed
         * {@link PlantSpecies#genusName()} upward FK — the genus &rarr; species rollup
         * the genus detail page renders.
         */
        SpeciesCollection forGenusName(PlantGenusName genusName);

        /**
         * Species under a family, composed through the genus query: every genus in the
         * family, then every species in each genus. Mirrors {@code InsectQuery.SpeciesQuery}.
         */
        SpeciesCollection forFamilyName(PlantFamilyName familyName);

        /** Batched sibling of {@link #forGenusName(PlantGenusName)} across a set of genera. */
        SpeciesCollection forGenusNames(Set<PlantGenusName> genusNames);
    }

    interface OrderQuery
            extends EntityQuery<PlantOrderName, PlantOrder,
                    PlantEntityCollections.OrderCollection> {
    }

    interface FamilyQuery
            extends EntityQuery<PlantFamilyName, PlantFamily, FamilyCollection> {

        /** Families under an order, joined on the family's typed upward FK. */
        FamilyCollection forOrderName(PlantOrderName orderName);
    }

    EcologicalRoleQuery ecologicalRoles();

    ObservationQuery observations();

    ImageQuery images();

    FeatureQuery features();

    interface FeatureQuery {

        /**
         * The lineage-composite field marks for a taxon at the given rank — the rank's own
         * assignments plus those inherited from its ancestors, grouped ancestor-first,
         * ordinal-ordered within a group. Mirrors {@code InsectQuery.FeatureQuery.findByRankName}.
         */
        PlantFeatureView findByRankName(PlantRankName subject);
    }

    interface ImageQuery
            extends EntityQuery<PlantImageId, OrganismImage<PlantImageId, PlantObservationId, PlantRankName>, ImageCollection> {

        /**
         * All photographs attached at the given rank name — the plant analogue of
         * {@code InsectQuery.ImageQuery.forParentName}. Attachment is by the image's typed
         * {@link OrganismImage#parentName()}, so a genus-level identification surfaces the
         * genus's own photos, not its species'.
         */
        ImageCollection forParentName(PlantRankName parentName);
    }

    interface GenusQuery
            extends EntityQuery<PlantGenusName, PlantGenus, GenusCollection> {

        /**
         * Genera under a family, joined on the genus's typed
         * {@link PlantGenus#familyName()} upward FK — the natural
         * family &rarr; genera rollup the family detail page renders.
         */
        GenusCollection forFamilyName(PlantFamilyName familyName);
    }

    interface EcologicalRoleQuery
            extends EntityQuery<PlantEcologicalRoleId, PlantEcologicalRole,
                    PlantEntityCollections.EcologicalRoleCollection> {

        /**
         * The ecological role recorded for a taxon, at whatever rank it was recorded.
         * Empty when the taxon's ecology has not been characterised — which is a real
         * state, not a missing record.
         */
        Optional<PlantEcologicalRole> forPlantName(PlantRankName plantName);
    }

    interface ObservationQuery
            extends EntityQuery<PlantObservationId, OrganismObservation<PlantObservationId, PlantRankName>,
                    PlantEntityCollections.ObservationCollection> {

        /** All of a naturalist's plant observations — the "my collection" surface. */
        PlantEntityCollections.ObservationCollection forNaturalist(NaturalistName observedBy);

        /**
         * A naturalist's observations restricted to the given ranks — the bounded read
         * port the rank pages use to render a per-entity "observed" indicator.
         */
        PlantEntityCollections.ObservationCollection forNaturalistAndSubjects(
                NaturalistName observedBy, Set<PlantRankName> subjects);
    }
}
