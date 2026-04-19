package com.naturalist.insects;

import com.naturalist.data.EntityRepository;

import java.util.Collection;
import java.util.List;

/**
 * Namespace for the insects bounded context's write-side repositories — the single
 * discoverable entry point for persistence of insect catalog data.
 *
 * <p>Nested repositories scope to a single entity each:
 * <ul>
 *   <li>{@link SpeciesRepository} — {@link InsectSpecies} entities.</li>
 *   <li>{@link ImageRepository} — {@link InsectImage} entities.</li>
 * </ul>
 *
 * <p>This is a {@code class}, not an {@code interface}, so the nested repository
 * contracts can carry their own access modifiers. Inside an interface, nested types
 * would be implicitly {@code public static}; inside a class, {@code protected} keeps
 * them hidden from foreign packages while permitting same-package adapter
 * implementations and subclass-based composite implementations. The class is
 * non-instantiable: it holds no state and no behavior, only the namespace.
 *
 * <p>See ADR-020.
 */
class InsectRepository {
    final protected SpeciesRepository speciesRepository;
    final protected ImageRepository imageRepository;

    InsectRepository(SpeciesRepository speciesRepository, ImageRepository imageRepository) {
        this.speciesRepository = speciesRepository;
        this.imageRepository = imageRepository;
    }

    protected interface SpeciesRepository
            extends EntityRepository<InsectSpeciesId, InsectSpeciesName, InsectSpecies> {}

    protected interface ImageRepository
            extends EntityRepository<InsectImageId, InsectImageName, InsectImage> {
        List<InsectImage> getBySpeciesName(InsectSpeciesName speciesName);

        List<InsectImage> getBySpeciesId(InsectSpeciesId speciesId);
    }
}
