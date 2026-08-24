package com.naturalist.insects;

import com.naturalist.data.EntityRepository;

import java.util.List;
import java.util.Set;
import com.naturalist.observation.OrganismImage;
import com.naturalist.observation.OrganismObservation;

/**
 * Namespace for the insects bounded context's write-side repositories — the single
 * discoverable entry point for persistence of insect catalog data.
 *
 * <p>Nested repositories scope to a single entity each:
 * <ul>
 *   <li>{@link SpeciesRepository} — {@link InsectSpecies} entities.</li>
 *   <li>{@link ImageRepository} — {@link OrganismImage} entities.</li>
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
    final ObservationRepository observationRepository;

    private InsectRepository(
            SpeciesRepository speciesRepository,
            ImageRepository imageRepository,
            FamilyRepository familyRepository,
            GenusRepository genusRepository,
            FunctionalRoleRepository functionalRoleRepository,
            OrderRepository orderRepository,
            FeatureRepository featureRepository,
            FeatureAssignmentRepository featureAssignmentRepository,
            ObservationRepository observationRepository) {
        this.speciesRepository = speciesRepository;
        this.imageRepository = imageRepository;
        this.familyRepository = familyRepository;
        this.genusRepository = genusRepository;
        this.functionalRoleRepository = functionalRoleRepository;
        this.orderRepository = orderRepository;
        this.featureRepository = featureRepository;
        this.featureAssignmentRepository = featureAssignmentRepository;
        this.observationRepository = observationRepository;
    }

    static InsectRepository create(
            SpeciesRepository speciesRepository,
            ImageRepository imageRepository,
            FamilyRepository familyRepository,
            GenusRepository genusRepository,
            FunctionalRoleRepository functionalRoleRepository,
            OrderRepository orderRepository,
            FeatureRepository featureRepository,
            FeatureAssignmentRepository featureAssignmentRepository,
            ObservationRepository observationRepository) {
        return new InsectRepository(speciesRepository, imageRepository, familyRepository,
                genusRepository, functionalRoleRepository, orderRepository,
                featureRepository, featureAssignmentRepository, observationRepository);
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

    ObservationRepository observationRepository() {
        return observationRepository;
    }

    protected interface SpeciesRepository
            extends EntityRepository<InsectSpeciesName, InsectSpecies> {

        List<InsectSpecies> getByGenusName(InsectGenusName genusName);

        List<InsectSpecies> getByGenusNames(Set<InsectGenusName> genusNames);
    }

    protected interface ImageRepository
            extends EntityRepository<InsectImageId, OrganismImage<InsectImageId, InsectObservationId, InsectRankName>> {

        List<OrganismImage<InsectImageId, InsectObservationId, InsectRankName>> getByParentName(InsectRankName parentName);

        List<OrganismImage<InsectImageId, InsectObservationId, InsectRankName>> getByParentNames(Set<InsectRankName> parentNames);
    }

    protected interface ObservationRepository
            extends EntityRepository<InsectObservationId, OrganismObservation<InsectObservationId, InsectRankName>> {

        java.util.List<OrganismObservation<InsectObservationId, InsectRankName>> getByNaturalist(
                com.naturalist.naturalist.NaturalistName observedBy);

        java.util.List<OrganismObservation<InsectObservationId, InsectRankName>> getByNaturalistAndSubjects(
                com.naturalist.naturalist.NaturalistName observedBy,
                java.util.Set<InsectRankName> subjects);
    }

    protected interface FamilyRepository
            extends EntityRepository<InsectFamilyName, InsectFamily> {

        List<InsectFamily> getByOrderName(InsectOrderName orderName);

        /** Batched sibling of {@link #getByOrderName} across a set of orders. */
        List<InsectFamily> getByOrderNames(Set<InsectOrderName> orderNames);
    }

    protected interface GenusRepository
            extends EntityRepository<InsectGenusName, InsectGenus> {

        List<InsectGenus> getByFamilyName(InsectFamilyName familyName);

        List<InsectGenus> getByFamilyNames(Set<InsectFamilyName> familyNames);
    }

    protected interface FunctionalRoleRepository
            extends EntityRepository<InsectFunctionalRoleId, InsectFunctionalRole> {

        List<InsectFunctionalRole> getByGuild(FunctionalGuild guild);

        java.util.Optional<InsectFunctionalRole> getByParentName(InsectRankName parentName);

        List<InsectFunctionalRole> getByParentNames(Set<InsectRankName> parentNames);
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

        List<InsectFeatureAssignment> getByRankNames(Set<InsectRankName> rankNames);

        List<InsectFeatureAssignment> getByFeatureId(InsectFeatureId featureId);
    }
}
