package com.naturalist.insects;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.Page;
import com.naturalist.data.PageRequest;
import com.naturalist.insects.lifestage.InsectLifeStageEntityCollections.LifeStageCollection;
import com.naturalist.insects.lifestage.InsectLifeStageQuery;
import com.naturalist.insects.lifestage.LifeStage;
import com.naturalist.library.LibraryTestContext;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Test wiring for {@code insects-core} tests. Follows the same pattern as
 * {@link InsectsTestContext} — constructs the package-private implementations
 * within the namespace, then exposes the public contract — but lives in core's
 * own test classpath to avoid the Maven cycle that prevents core from depending
 * on {@code insects-test-context}.
 *
 * <p>Library and life-stage queries use no-op stubs since the transaction and
 * command tests do not exercise those paths. When Spring DI replaces the manual
 * composition, both this class and {@code InsectsTestContext} go away.
 */
class InsectsTestContextInternal {

    private final InsectQuery insectQuery;
    private final InsectCommand insectCommand;
    private final InsectCatalogIdentificationTransaction catalogIdentificationTransaction;
    private final InsectAddPhotoTransaction addPhotoTransaction;

    private InsectsTestContextInternal(NaturalistDatabase db) {
        InsectRepository repository = InsectRepository.create(
                new InsectSpeciesRepositoryMock(db),
                new InsectImageRepositoryMock(db),
                new InsectFamilyRepositoryMock(db),
                new InsectGenusRepositoryMock(db),
                new InsectFunctionalRoleRepositoryMock(db),
                new InsectOrderRepositoryMock(db),
                new InsectFeatureRepositoryMock(db),
                new InsectFeatureAssignmentRepositoryMock(db),
                new InsectObservationRepositoryMock(db));

        InsectQuery.FamilyQuery familyQuery = new InsectFamilyQueryImpl(repository.familyRepository);
        InsectQuery.GenusQuery genusQuery = new InsectGenusQueryImpl(repository.genusRepository, familyQuery);
        InsectQuery.SpeciesQuery speciesQuery = new InsectSpeciesQueryImpl(repository.speciesRepository, genusQuery);
        InsectQuery.ImageQuery imageQuery = new InsectImageQueryImpl(
                repository.imageRepository, speciesQuery, genusQuery, familyQuery);
        InsectQuery.ObservationQuery observationQuery =
                new InsectObservationQueryImpl(repository.observationRepository);
        InsectQuery.FunctionalRoleQuery functionalRoleQuery =
                new InsectFunctionalRoleQueryImpl(repository.functionalRoleRepository);
        InsectQuery.OrderQuery orderQuery = new InsectOrderQueryImpl(repository.orderRepository);

        LibraryTestContext libraryContext = LibraryTestContext.create(db);

        InsectAncestryResolver ancestryResolver =
                new InsectAncestryResolver(speciesQuery, genusQuery, familyQuery);
        InsectCitationQueryImpl citationQuery = new InsectCitationQueryImpl(
                libraryContext.citationAssociationQuery(), libraryContext.citationQuery(), ancestryResolver);
        InsectFeatureQueryImpl featureQuery = new InsectFeatureQueryImpl(
                repository.featureRepository, repository.featureAssignmentRepository, ancestryResolver);
        this.insectQuery = new InsectQueryImpl(
                speciesQuery, imageQuery, familyQuery, genusQuery, functionalRoleQuery,
                orderQuery, citationQuery, featureQuery,
                noOpLifeStageQuery(), observationQuery);

        InsectCommand.SpeciesCommand speciesCommand = new InsectSpeciesCommandImpl(repository.speciesRepository);
        InsectCommand.ImageCommand imageCommand = new InsectImageCommandImpl(repository.imageRepository);
        InsectCommand.ObservationCommand observationCommand =
                new InsectObservationCommandImpl(repository.observationRepository);
        InsectCommand.OrderCommand orderCommand = new InsectOrderCommandImpl(repository.orderRepository);
        InsectCommand.FamilyCommand familyCommand = new InsectFamilyCommandImpl(repository.familyRepository);
        InsectCommand.GenusCommand genusCommand = new InsectGenusCommandImpl(repository.genusRepository);
        InsectCommand.FeatureCommand featureCommand =
                new InsectFeatureCommandImpl(repository.featureRepository);
        InsectCommand.FeatureAssignmentCommand featureAssignmentCommand =
                new InsectFeatureAssignmentCommandImpl(repository.featureAssignmentRepository);
        this.insectCommand = new InsectCommandImpl(
                speciesCommand, imageCommand, observationCommand,
                orderCommand, familyCommand, genusCommand,
                featureCommand, featureAssignmentCommand);

        this.catalogIdentificationTransaction = new InsectCatalogIdentificationTransaction(
                this.insectCommand, this.insectQuery);
        this.addPhotoTransaction = new InsectAddPhotoTransaction(this.insectCommand);
    }

    static InsectsTestContextInternal create(NaturalistDatabase db) {
        return new InsectsTestContextInternal(db);
    }

    InsectQuery insectQuery() {
        return insectQuery;
    }

    InsectCommand insectCommand() {
        return insectCommand;
    }

    InsectCatalogIdentificationTransaction catalogIdentificationTransaction() {
        return catalogIdentificationTransaction;
    }

    InsectAddPhotoTransaction addPhotoTransaction() {
        return addPhotoTransaction;
    }

    private static InsectLifeStageQuery noOpLifeStageQuery() {
        return () -> new InsectLifeStageQuery.LifeStageEntityQuery() {
            @Override
            public LifeStageCollection forParentName(InsectRankName parentName) {
                return LifeStageCollection.empty();
            }

            @Override
            public Optional<LifeStage> getByName(com.naturalist.insects.LifeStageName name) {
                return Optional.empty();
            }

            @Override
            public LifeStageCollection findByNameSet(Set<com.naturalist.insects.LifeStageName> names) {
                return LifeStageCollection.empty();
            }

            @Override
            public Page<LifeStage> findPage(PageRequest pageRequest) {
                return new Page<>(List.of(), pageRequest.pageNumber(), pageRequest.pageSize(), 0, false);
            }
        };
    }
}
