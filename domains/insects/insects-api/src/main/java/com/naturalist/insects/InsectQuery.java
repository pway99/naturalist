package com.naturalist.insects;

import com.naturalist.data.EntityQuery;
import com.naturalist.insects.InsectEntityCollections.FamilyCollection;
import com.naturalist.insects.InsectEntityCollections.FunctionalRoleCollection;
import com.naturalist.insects.InsectEntityCollections.GenusCollection;
import com.naturalist.insects.InsectEntityCollections.ImageCollection;
import com.naturalist.insects.InsectEntityCollections.SpeciesCollection;

import java.util.Optional;

/**
 * Namespace query for the insects bounded context — the single discoverable entry point
 * for reading insect catalog data.
 *
 * <p>Nested queries scope to a single consistency concern each:
 * <ul>
 *   <li>{@link InsectAggregateQuery} — the catalog-view {@link InsectAggregate}
 *       (rank entity + images), rooted at whichever Linnaean rank the parent name
 *       identifies (family, genus, or species).</li>
 *   <li>{@link SpeciesQuery} — {@link InsectSpecies} entities in isolation.</li>
 *   <li>{@link ImageQuery} — {@link InsectImage} entities in isolation.</li>

 *   <li>{@link FamilyQuery} — {@link InsectFamily} entities in isolation.</li>
 *   <li>{@link GenusQuery} — {@link InsectGenus} entities in isolation.</li>
 *   <li>{@link FunctionalRoleQuery} — {@link InsectFunctionalRole} entities,
 *       carrying the cross-rank {@code (guilds, beneficial)} assignment.</li>
 * </ul>
 *
 * <p><b>Usage:</b>
 * <pre>{@code
 * insectQuery.species().getByName(speciesName);   // InsectSpecies
 * insectQuery.images().getByName(imageName);      // InsectImage
 * insectQuery.insect().getByName(speciesName);    // InsectSpeciesAggregate
 * insectQuery.insect().getByName(genusName);      // InsectGenusAggregate
 * insectQuery.insect().getByName(familyName);     // InsectFamilyAggregate
 * insectQuery.families().getByName(familyName);   // InsectFamily
 * insectQuery.genera().getByName(genusName);      // InsectGenus
 * insectQuery.genera().forFamilyName(familyName);  // genera under a family
 * insectQuery.species().forGenusName(genusName);   // species under a genus
 * insectQuery.species().forFamilyName(familyName); // species under a family (typed FK)
 * insectQuery.functionalRoles().getByGuild(guild);// InsectFunctionalRoleCollection
 * }</pre>
 */
public interface InsectQuery {

    InsectAggregateQuery insect();

    SpeciesQuery species();

    ImageQuery images();

    FamilyQuery families();

    GenusQuery genera();

    FunctionalRoleQuery functionalRoles();

    interface InsectAggregateQuery {

        /**
         * Resolve the {@link InsectAggregate} rooted at the given rank name. The
         * concrete permit returned matches the {@link InsectRankName} permit passed in
         * (species → {@link InsectSpeciesAggregate}, genus → {@link InsectGenusAggregate},
         * family → {@link InsectFamilyAggregate}). Subspecies-rank names always return
         * {@link Optional#empty()} — no subspecies entity exists in the catalog yet.
         */
        Optional<InsectAggregate> getByName(InsectRankName name);
    }

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
    }

    interface ImageQuery
            extends EntityQuery<InsectImageId, InsectImage, ImageCollection> {

        ImageCollection forParentName(InsectRankName parentName);
    }

    interface FamilyQuery extends EntityQuery<InsectFamilyName, InsectFamily, FamilyCollection> {
    }

    interface GenusQuery extends EntityQuery<InsectGenusName, InsectGenus, GenusCollection> {

        GenusCollection forFamilyName(InsectFamilyName familyName);
    }

    interface FunctionalRoleQuery
            extends EntityQuery<InsectFunctionalRoleId, InsectFunctionalRole, FunctionalRoleCollection> {

        FunctionalRoleCollection getByGuild(FunctionalGuild guild);

        Optional<InsectFunctionalRole> getByParentName(InsectRankName parentName);
    }
}
