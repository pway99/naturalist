package com.naturalist.insects;

import com.naturalist.data.EntityQuery;
import com.naturalist.insects.InsectEntityCollections.FamilyCollection;
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
 *       (species + images), rooted at {@link InsectSpecies}.</li>
 *   <li>{@link SpeciesQuery} — {@link InsectSpecies} entities in isolation.</li>
 *   <li>{@link ImageQuery} — {@link InsectImage} entities in isolation.</li>
 *   <li>{@link FamilyQuery} — {@link InsectFamily} entities in isolation.</li>
 *   <li>{@link GenusQuery} — {@link InsectGenus} entities in isolation.</li>
 * </ul>
 *
 * <p><b>Usage:</b>
 * <pre>{@code
 * insectQuery.species().getByName(speciesName);   // InsectSpecies
 * insectQuery.images().getByName(imageName);      // InsectImage
 * insectQuery.insect().getByName(speciesName);    // InsectAggregate rooted at speciesName
 * insectQuery.families().getByName(familyName);   // InsectFamily
 * insectQuery.genera().getByName(genusName);      // InsectGenus
 * }</pre>
 */
public interface InsectQuery {

    InsectAggregateQuery insect();

    SpeciesQuery species();

    ImageQuery images();

    FamilyQuery families();

    GenusQuery genera();

    interface InsectAggregateQuery {
        Optional<InsectAggregate> getByName(InsectSpeciesName name);
    }

    interface SpeciesQuery extends EntityQuery<InsectSpeciesName, InsectSpecies, SpeciesCollection> {

        SpeciesCollection getByFunctionalGuild(FunctionalGuild functionalGuild);
    }

    interface ImageQuery
            extends EntityQuery<InsectImageId, InsectImage, ImageCollection> {

        ImageCollection forSpeciesName(InsectSpeciesName speciesName);
    }

    interface FamilyQuery extends EntityQuery<InsectFamilyName, InsectFamily, FamilyCollection> {
    }

    interface GenusQuery extends EntityQuery<InsectGenusName, InsectGenus, GenusCollection> {
    }
}
