package com.naturalist.insects;

import com.naturalist.data.EntityQuery;
import com.naturalist.insects.InsectEntityCollections.FamilyCollection;
import com.naturalist.insects.InsectEntityCollections.FunctionalRoleCollection;
import com.naturalist.insects.InsectEntityCollections.GenusCollection;
import com.naturalist.insects.InsectEntityCollections.ImageCollection;
import com.naturalist.insects.InsectEntityCollections.OrderCollection;
import com.naturalist.insects.InsectEntityCollections.SpeciesCollection;

import java.util.Optional;
import java.util.Set;
import com.naturalist.observation.OrganismImage;
import com.naturalist.observation.OrganismObservation;

/**
 * Namespace query for the insects bounded context — the single discoverable entry point
 * for reading insect catalog data.
 *
 * <p>Nested queries scope to a single consistency concern each:
 * <ul>
 *   <li>{@link SpeciesQuery} — {@link InsectSpecies} entities in isolation.</li>
 *   <li>{@link ImageQuery} — {@link OrganismImage} entities in isolation.</li>

 *   <li>{@link FamilyQuery} — {@link InsectFamily} entities in isolation.</li>
 *   <li>{@link GenusQuery} — {@link InsectGenus} entities in isolation.</li>
 *   <li>{@link FunctionalRoleQuery} — {@link InsectFunctionalRole} entities,
 *       carrying the cross-rank {@code (guilds, beneficial)} assignment.</li>
 * </ul>
 *
 * <p><b>Usage:</b>
 * <pre>{@code
 * insectQuery.species().getByName(speciesName);      // InsectSpecies
 * insectQuery.images().getByName(imageName);         // OrganismImage
 * insectQuery.families().getByName(familyName);      // InsectFamily
 * insectQuery.genera().getByName(genusName);         // InsectGenus
 * insectQuery.genera().forFamilyName(familyName);    // genera under a family
 * insectQuery.species().forGenusName(genusName);     // species under a genus
 * insectQuery.species().forFamilyName(familyName);   // species under a family (typed FK)
 * insectQuery.functionalRoles().getByGuild(guild);   // InsectFunctionalRoleCollection
 * }</pre>
 */
public interface InsectQuery {

    SpeciesQuery species();

    ImageQuery images();

    ObservationQuery observations();

    FamilyQuery families();

    GenusQuery genera();

    FunctionalRoleQuery functionalRoles();

    OrderQuery orders();

    CitationQuery citations();

    FeatureQuery features();

    /**
     * Assembles the full {@link Insect} read model rooted at the given rank name.
     * Resolves the rank chain from the given name up to the order, fetches images,
     * life stages, and citations, then returns the composed result. Returns
     * {@link Optional#empty()} when no entity exists at the given name, or when
     * the name is an {@link InsectSubspeciesName} (no subspecies entity exists yet).
     */
    Optional<Insect> getByName(InsectRankName name);

    interface SpeciesQuery extends EntityQuery<InsectSpeciesName, InsectSpecies, SpeciesCollection> {

        /**
         * Members of a genus, joined on the species's typed {@link InsectSpecies#genusName()}
         * upward FK. Species without a catalogued parent genus carry a {@code null}
         * {@code genusName} and are absent from the result.
         */
        SpeciesCollection forGenusName(InsectGenusName genusName);

        /**
         * Members of a family, joined on the species's typed {@link InsectSpecies#familyName()}
         * upward FK (e.g. {@code battus-philenor} → {@code papilionidae}). Species without
         * a catalogued parent family carry a {@code null} {@code familyName} and are absent
         * from the result.
         */
        SpeciesCollection forFamilyName(InsectFamilyName familyName);

        /** Batched sibling of {@link #forGenusName(InsectGenusName)} across a set of genera. */
        SpeciesCollection forGenusNames(Set<InsectGenusName> genusNames);
    }

    interface ImageQuery
            extends EntityQuery<InsectImageId, OrganismImage<InsectImageId, InsectObservationId, InsectRankName>, ImageCollection> {

        ImageCollection forParentName(InsectRankName parentName);

        /** Batched sibling of {@link #forParentName(InsectRankName)} across a set of ranks. */
        ImageCollection forParentNames(Set<InsectRankName> parentNames);

        /**
         * Returns all images for the given rank and all descendant ranks in the
         * Linnaean hierarchy. For a species, this is just the species' own images.
         * For a genus, it includes the genus' images plus all member species' images.
         * For a family, it walks genera and their species. For an order, it walks
         * families, genera, and species.
         */
        ImageCollection forRankHierarchy(InsectRankName rankName);
    }

    interface ObservationQuery
            extends EntityQuery<InsectObservationId, OrganismObservation<InsectObservationId, InsectRankName>,
                    InsectEntityCollections.ObservationCollection> {

        /** All of a naturalist's observations — used for the species-list "my collection" filter. */
        InsectEntityCollections.ObservationCollection forNaturalist(
                com.naturalist.naturalist.NaturalistName observedBy);

        /**
         * A naturalist's observations restricted to the given ranks — bounded read port for the
         * rank pages (order/family/genus/species detail), which pass the ranks they display to
         * render a per-entity "collected" indicator.
         */
        InsectEntityCollections.ObservationCollection forNaturalistAndSubjects(
                com.naturalist.naturalist.NaturalistName observedBy,
                java.util.Set<InsectRankName> subjects);
    }

    interface FamilyQuery extends EntityQuery<InsectFamilyName, InsectFamily, FamilyCollection> {

        FamilyCollection forOrderName(InsectOrderName orderName);
    }

    interface GenusQuery extends EntityQuery<InsectGenusName, InsectGenus, GenusCollection> {

        GenusCollection forFamilyName(InsectFamilyName familyName);

        /** Batched sibling of {@link #forFamilyName(InsectFamilyName)} across a set of families. */
        GenusCollection forFamilyNames(Set<InsectFamilyName> familyNames);

        GenusCollection forOrderName(InsectOrderName orderName);
    }

    interface FunctionalRoleQuery
            extends EntityQuery<InsectFunctionalRoleId, InsectFunctionalRole, FunctionalRoleCollection> {

        FunctionalRoleCollection getByGuild(FunctionalGuild guild);

        Optional<InsectFunctionalRole> getByParentName(InsectRankName parentName);

        /** Batched sibling of {@link #getByParentName(InsectRankName)} across a set of ranks. */
        FunctionalRoleCollection getByParentNames(Set<InsectRankName> parentNames);
    }

    interface OrderQuery extends EntityQuery<InsectOrderName, InsectOrder, OrderCollection> {
    }

    interface CitationQuery {
        InsectCitationView findByRankName(InsectRankName rankName);
    }

    interface FeatureQuery {

        /**
         * Returns the lineage-composited, display-ready {@link InsectFeatureView} for the
         * given rank — the full conspicuous-to-diagnostic groups the organism inherits
         * from its ancestry, one {@link InsectFeatureView.RankGroup} per contributing rank.
         * Never {@code null}; {@link InsectFeatureView#groups()} is empty when no rank in
         * the ancestry carries a feature assignment.
         */
        InsectFeatureView findByRankName(InsectRankName subject);

        /**
         * Returns the ranks carrying the given feature — the reverse lookup ("which taxa
         * have chewing mouthparts"). Joins through {@link InsectFeatureAssignment} by
         * {@link InsectFeatureId}.
         */
        Set<InsectRankName> findByFeature(InsectFeatureId featureId);
    }
}
