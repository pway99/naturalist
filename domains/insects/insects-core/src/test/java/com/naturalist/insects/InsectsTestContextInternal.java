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
                new SpeciesRepositoryMock(db),
                new InsectImageRepositoryMock(db),
                new FamilyRepositoryMock(db),
                new GenusRepositoryMock(db),
                new InsectFunctionalRoleRepositoryMock(db),
                new OrderRepositoryMock(db),
                new InsectFeatureRepositoryMock(db),
                new InsectFeatureAssignmentRepositoryMock(db),
                new InsectObservationRepositoryMock(db));

        InsectQuery.FamilyQuery familyQuery = new FamilyQueryImpl(repository.familyRepository);
        InsectQuery.GenusQuery genusQuery = new GenusQueryImpl(repository.genusRepository, familyQuery);
        InsectQuery.SpeciesQuery speciesQuery = new SpeciesQueryImpl(repository.speciesRepository, genusQuery);
        InsectQuery.ImageQuery imageQuery = new ImageQueryImpl(
                repository.imageRepository, speciesQuery, genusQuery, familyQuery);
        InsectQuery.InsectObservationQuery observationQuery =
                new InsectObservationQueryImpl(repository.observationRepository);
        InsectQuery.FunctionalRoleQuery functionalRoleQuery =
                new FunctionalRoleQueryImpl(repository.functionalRoleRepository);
        InsectQuery.OrderQuery orderQuery = new OrderQueryImpl(repository.orderRepository);

        LibraryTestContext libraryContext = LibraryTestContext.create(db);

        this.insectQuery = new InsectQueryImpl(
                speciesQuery, imageQuery, familyQuery, genusQuery, functionalRoleQuery,
                orderQuery, libraryContext.citationAssociationQuery(),
                libraryContext.citationQuery(), noOpLifeStageQuery(),
                repository.featureRepository, repository.featureAssignmentRepository,
                observationQuery);

        InsectCommand.SpeciesCommand speciesCommand = new SpeciesCommandImpl(repository.speciesRepository);
        InsectCommand.ImageCommand imageCommand = new ImageCommandImpl(repository.imageRepository);
        InsectCommand.InsectObservationCommand observationCommand =
                new InsectObservationCommandImpl(repository.observationRepository);
        InsectCommand.OrderCommand orderCommand = new OrderCommandImpl(repository.orderRepository);
        InsectCommand.FamilyCommand familyCommand = new FamilyCommandImpl(repository.familyRepository);
        InsectCommand.GenusCommand genusCommand = new GenusCommandImpl(repository.genusRepository);
        InsectCommand.FeatureCommand featureCommand =
                new FeatureCommandImpl(repository.featureRepository);
        InsectCommand.FeatureAssignmentCommand featureAssignmentCommand =
                new FeatureAssignmentCommandImpl(repository.featureAssignmentRepository);
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
