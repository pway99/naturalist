package com.naturalist.insects;

import com.naturalist.Incubating;
import com.naturalist.data.EntityRepository;

/**
 * Namespace repository for the insects bounded context — the single discoverable entry
 * point for write-side persistence of insect catalog data.
 *
 * <p>Nested repositories scope to a single entity each:
 * <ul>
 *   <li>{@link SpeciesRepository} — {@link InsectSpecies} entities.</li>
 *   <li>{@link ImageRepository} — {@link InsectImage} entities.</li>
 * </ul>
 *
 * <p>{@code InsectRepository} is a pure container — package-private, holding no behavior
 * of its own. Mirrors the {@link InsectQuery} pattern for write-side discoverability:
 * one file per namespace, nested types for everything inside.
 */
@Incubating("Investigating a pattern where EntityRepositories are nested within a single interface")
interface InsectRepository {

    interface SpeciesRepository
            extends EntityRepository<InsectSpeciesId, InsectSpeciesName, InsectSpecies> {}

    interface ImageRepository
            extends EntityRepository<InsectImageId, InsectImageName, InsectImage> {}
}
