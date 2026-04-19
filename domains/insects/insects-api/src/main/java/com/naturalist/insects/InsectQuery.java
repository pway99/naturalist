package com.naturalist.insects;

import com.naturalist.data.NamedEntityQuery;
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
 * </ul>
 *
 * <p><b>Usage:</b>
 * <pre>{@code
 * insectQuery.species().getByName(speciesName);   // InsectSpecies
 * insectQuery.images().getByName(imageName);      // InsectImage
 * insectQuery.insect().getByName(speciesName);    // InsectAggregate rooted at speciesName
 * }</pre>
 */
public interface InsectQuery {

    InsectAggregateQuery insect();

    SpeciesQuery species();

    ImageQuery images();

    interface InsectAggregateQuery {
        Optional<InsectAggregate> getByName(InsectSpeciesName name);
    }

    interface SpeciesQuery
            extends NamedEntityQuery<InsectSpeciesName, InsectSpecies, SpeciesCollection> {}

    interface ImageQuery
            extends NamedEntityQuery<InsectImageName, InsectImage, ImageCollection> {

        ImageCollection forSpeciesName(InsectSpeciesName speciesName);
    }
}
