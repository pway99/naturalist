package com.naturalist.insects;

import com.naturalist.authority.AuthorityContent;
import com.naturalist.authority.AuthorityReference;
import com.naturalist.authority.AuthoritySource;
import com.naturalist.authority.ExternalAuthority;
import com.naturalist.ddd.EntityName;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.featuresearch.FeatureCorpus;
import com.naturalist.featuresearch.FeatureSearch;
import com.naturalist.featuresearch.InMemoryFeatureSearch;
import com.naturalist.data.Page;
import com.naturalist.data.PageRequest;
import com.naturalist.insects.lifestage.InsectLifeStageEntityCollections.LifeStageCollection;
import com.naturalist.insects.lifestage.InsectLifeStageQuery;
import com.naturalist.insects.lifestage.LifeStage;
import com.naturalist.library.LibraryTestContext;
import com.naturalist.observation.ImageStore;
import com.naturalist.observation.filesystem.FilesystemImageStore;
import com.naturalist.resilience.Resilience;
import com.naturalist.textgeneration.NoOpTextGenerationService;
import com.naturalist.usage.IdentificationBudget;
import com.naturalist.vision.NoOpVisionService;
import com.naturalist.vision.VisionService;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
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
    private final InsectEntityCommands entityCommands;
    private final InsectCatalogIdentificationTransaction catalogIdentificationTransaction;
    private final InsectAddPhotoTransaction addPhotoTransaction;

    private InsectsTestContextInternal(NaturalistDatabase db, ImageStore imageStore, Resilience resilience,
                                       IdentificationBudget budget, VisionService visionService,
                                       ExternalAuthority authority) {
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
        this.entityCommands = new InsectEntityCommands(
                speciesCommand, imageCommand, observationCommand,
                orderCommand, familyCommand, genusCommand,
                featureCommand, featureAssignmentCommand);
        this.addPhotoTransaction = new InsectAddPhotoTransaction(this.entityCommands);
        this.catalogIdentificationTransaction = new InsectCatalogIdentificationTransaction(
                this.entityCommands, this.insectQuery);
        InsectQuery wiredQuery = this.insectQuery;
        FeatureSearch<InsectFeatureId> featureSearch = new InMemoryFeatureSearch<>(() -> wiredQuery.features().corpus()
                .map(f -> new FeatureCorpus.Indexed<>(f.id(), f.value())));
        InsectCommand.IdentificationCommand identificationCommand = new InsectIdentificationCommandImpl(
                resilience, budget, visionService, new NoOpTextGenerationService(), authority,
                libraryContext.libraryCommand(), this.insectQuery, featureSearch, imageStore,
                this.catalogIdentificationTransaction);
        this.insectCommand = new InsectCommandImpl(this.entityCommands,
                new InsectSightingCommandImpl(observationCommand, observationQuery),
                new InsectPhotoCommandImpl(imageStore, this.addPhotoTransaction),
                identificationCommand);
    }

    static InsectsTestContextInternal create(NaturalistDatabase db) {
        return create(db, tempImageStore());
    }

    static InsectsTestContextInternal create(NaturalistDatabase db, ImageStore imageStore) {
        return create(db, imageStore, Resilience.noOp(), IdentificationBudget.noOp(),
                new NoOpVisionService(), NO_OP_AUTHORITY);
    }

    /**
     * Full overload: the identification port ({@code insectCommand().identification()}) runs
     * behind the given rate-limiting {@link Resilience}, {@link IdentificationBudget},
     * {@link VisionService} and {@link ExternalAuthority}.
     */
    static InsectsTestContextInternal create(NaturalistDatabase db, ImageStore imageStore, Resilience resilience,
                                             IdentificationBudget budget, VisionService visionService,
                                             ExternalAuthority authority) {
        return new InsectsTestContextInternal(db, imageStore, resilience, budget, visionService, authority);
    }

    private static ImageStore tempImageStore() {
        try {
            return new FilesystemImageStore(Files.createTempDirectory("insects-images"), Resilience.noOp());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    InsectQuery insectQuery() {
        return insectQuery;
    }

    InsectCommand insectCommand() {
        return insectCommand;
    }

    InsectEntityCommands entityCommands() {
        return entityCommands;
    }

    InsectCatalogIdentificationTransaction catalogIdentificationTransaction() {
        return catalogIdentificationTransaction;
    }

    InsectAddPhotoTransaction addPhotoTransaction() {
        return addPhotoTransaction;
    }

    /** Confirms nothing and fetches no content — the identification path is not exercised here. */
    private static final ExternalAuthority NO_OP_AUTHORITY = new ExternalAuthority() {
        private final AuthoritySource source = new AuthoritySource("test", "Test Authority");

        @Override
        public AuthoritySource source() {
            return source;
        }

        @Override
        public Set<AuthorityReference> lookup(EntityName name) {
            return Set.of();
        }

        @Override
        public AuthorityContent fetchContent(AuthorityReference reference) {
            throw new UnsupportedOperationException("no-op authority fetches no content");
        }
    };

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
