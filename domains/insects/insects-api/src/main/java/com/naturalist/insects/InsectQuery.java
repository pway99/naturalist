package com.naturalist.insects;

import com.naturalist.data.AggregateQuery;
import com.naturalist.data.EntityQuery;
import com.naturalist.insects.InsectEntityCollections.ImageCollection;
import com.naturalist.insects.InsectEntityCollections.SpeciesCollection;

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
 * <p>{@code InsectQuery} is a pure container — it holds no query logic of its own, only
 * navigation accessors. Each consistency boundary is an explicit nested query. This
 * mirrors the {@code ElementRepository} pattern for read-side discoverability: one file
 * per namespace, nested types for everything inside.
 *
 * <p><b>Usage:</b>
 * <pre>{@code
 * insectQuery.species().get(speciesName);     // InsectSpecies
 * insectQuery.images().get(imageName);        // InsectImage
 * insectQuery.insect().get(speciesName);      // InsectAggregate rooted at speciesName
 * }</pre>
 */
public interface InsectQuery {

    InsectAggregateQuery insect();

    SpeciesQuery species();

    ImageQuery images();

    interface InsectAggregateQuery
            extends AggregateQuery<InsectSpeciesId, InsectSpeciesName, InsectAggregate> {}

    interface SpeciesQuery
            extends EntityQuery<InsectSpeciesId, InsectSpeciesName, InsectSpecies, SpeciesCollection> {}

    interface ImageQuery
            extends EntityQuery<InsectImageId, InsectImageName, InsectImage, ImageCollection> {}
}
