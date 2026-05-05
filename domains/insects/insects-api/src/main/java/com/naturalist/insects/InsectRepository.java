package com.naturalist.insects;

import com.naturalist.data.EntityRepository;

import java.util.List;

/**
 * Namespace for the insects bounded context's write-side repositories — the single
 * discoverable entry point for persistence of insect catalog data.
 *
 * <p>Nested repositories scope to a single entity each:
 * <ul>
 *   <li>{@link SpeciesRepository} — {@link InsectSpecies} entities.</li>
 *   <li>{@link ImageRepository} — {@link InsectImage} entities.</li>
 *   <li>{@link FamilyRepository} — {@link InsectFamily} entities.</li>
 * </ul>
 *
 * <p>This is a {@code class}, not an {@code interface}, so the nested repository
 * contracts can carry their own access modifiers. Inside an interface, nested types
 * would be implicitly {@code public static}; inside a class, {@code protected} keeps
 * them hidden from foreign packages while permitting same-package adapter
 * implementations. The class is non-instantiable — it holds no state and no behavior,
 * only the namespace (ADR-020).
 */
class InsectRepository {
    final SpeciesRepository speciesRepository;
    final ImageRepository imageRepository;
    final FamilyRepository familyRepository;

    private InsectRepository(
            SpeciesRepository speciesRepository,
            ImageRepository imageRepository,
            FamilyRepository familyRepository) {
        this.speciesRepository = speciesRepository;
        this.imageRepository = imageRepository;
        this.familyRepository = familyRepository;
    }

    static InsectRepository create(
            SpeciesRepository speciesRepository,
            ImageRepository imageRepository,
            FamilyRepository familyRepository) {
        return new InsectRepository(speciesRepository, imageRepository, familyRepository);
    }

    SpeciesRepository speciesRepository() {
        return speciesRepository;
    }

    ImageRepository imageRepository() {
        return imageRepository;
    }

    FamilyRepository familyRepository() {
        return familyRepository;
    }

    protected interface SpeciesRepository
            extends EntityRepository<InsectSpeciesName, InsectSpecies> {
        List<InsectSpeciesName> getAllSpeciesNames();

        List<InsectSpecies> getByFunctionalGuild(FunctionalGuild functionalGuild);
    }

    protected interface ImageRepository
            extends EntityRepository<InsectImageId, InsectImage> {

        List<InsectImage> getBySpeciesName(InsectSpeciesName speciesName);
    }

    protected interface FamilyRepository
            extends EntityRepository<InsectFamilyName, InsectFamily> {
    }
}
