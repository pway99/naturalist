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
 *   <li>{@link GenusRepository} — {@link InsectGenus} entities.</li>
 *   <li>{@link FunctionalRoleRepository} — {@link InsectFunctionalRole} entities.</li>
 *   <li>{@link OrderRepository} — {@link InsectOrder} entities.</li>
 *   <li>{@link FeatureRepository} — {@link InsectFeature} entities.</li>
 *   <li>{@link FeatureAssignmentRepository} — {@link InsectFeatureAssignment} entities.</li>
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
    final GenusRepository genusRepository;
    final FunctionalRoleRepository functionalRoleRepository;
    final OrderRepository orderRepository;
    final FeatureRepository featureRepository;
    final FeatureAssignmentRepository featureAssignmentRepository;

    private InsectRepository(
            SpeciesRepository speciesRepository,
            ImageRepository imageRepository,
            FamilyRepository familyRepository,
            GenusRepository genusRepository,
            FunctionalRoleRepository functionalRoleRepository,
            OrderRepository orderRepository,
            FeatureRepository featureRepository,
            FeatureAssignmentRepository featureAssignmentRepository) {
        this.speciesRepository = speciesRepository;
        this.imageRepository = imageRepository;
        this.familyRepository = familyRepository;
        this.genusRepository = genusRepository;
        this.functionalRoleRepository = functionalRoleRepository;
        this.orderRepository = orderRepository;
        this.featureRepository = featureRepository;
        this.featureAssignmentRepository = featureAssignmentRepository;
    }

    static InsectRepository create(
            SpeciesRepository speciesRepository,
            ImageRepository imageRepository,
            FamilyRepository familyRepository,
            GenusRepository genusRepository,
            FunctionalRoleRepository functionalRoleRepository,
            OrderRepository orderRepository,
            FeatureRepository featureRepository,
            FeatureAssignmentRepository featureAssignmentRepository) {
        return new InsectRepository(speciesRepository, imageRepository, familyRepository,
                genusRepository, functionalRoleRepository, orderRepository,
                featureRepository, featureAssignmentRepository);
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

    GenusRepository genusRepository() {
        return genusRepository;
    }

    FunctionalRoleRepository functionalRoleRepository() {
        return functionalRoleRepository;
    }

    OrderRepository orderRepository() {
        return orderRepository;
    }

    FeatureRepository featureRepository() {
        return featureRepository;
    }

    FeatureAssignmentRepository featureAssignmentRepository() {
        return featureAssignmentRepository;
    }

    protected interface SpeciesRepository
            extends EntityRepository<InsectSpeciesName, InsectSpecies> {

        List<InsectSpecies> getByGenusName(InsectGenusName genusName);
    }

    protected interface ImageRepository
            extends EntityRepository<InsectImageId, InsectImage> {

        List<InsectImage> getByParentName(InsectRankName parentName);
    }

    protected interface FieldObservationRepository
            extends EntityRepository<FieldObservationId, FieldObservation> {

        java.util.List<FieldObservation> getByNaturalist(
                com.naturalist.naturalist.NaturalistName observedBy);

        java.util.List<FieldObservation> getByNaturalistAndSubjects(
                com.naturalist.naturalist.NaturalistName observedBy,
                java.util.Set<InsectRankName> subjects);
    }

    protected interface FamilyRepository
            extends EntityRepository<InsectFamilyName, InsectFamily> {

        List<InsectFamily> getByOrderName(InsectOrderName orderName);
    }

    protected interface GenusRepository
            extends EntityRepository<InsectGenusName, InsectGenus> {

        List<InsectGenus> getByFamilyName(InsectFamilyName familyName);
    }

    protected interface FunctionalRoleRepository
            extends EntityRepository<InsectFunctionalRoleId, InsectFunctionalRole> {

        List<InsectFunctionalRole> getByGuild(FunctionalGuild guild);

        java.util.Optional<InsectFunctionalRole> getByParentName(InsectRankName parentName);
    }

    protected interface OrderRepository
            extends EntityRepository<InsectOrderName, InsectOrder> {
    }

    protected interface FeatureRepository
            extends EntityRepository<InsectFeatureId, InsectFeature> {
    }

    protected interface FeatureAssignmentRepository
            extends EntityRepository<InsectFeatureAssignmentId, InsectFeatureAssignment> {

        List<InsectFeatureAssignment> getByRankName(InsectRankName rankName);

        List<InsectFeatureAssignment> getByFeatureId(InsectFeatureId featureId);
    }
}
