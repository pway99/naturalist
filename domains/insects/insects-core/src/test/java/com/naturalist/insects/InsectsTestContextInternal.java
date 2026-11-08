package com.naturalist.insects;

import com.naturalist.authority.AuthorityContent;
import com.naturalist.authority.AuthorityReference;
import com.naturalist.authority.AuthoritySource;
import com.naturalist.authority.ExternalAuthority;
import com.naturalist.ddd.EntityName;
import com.naturalist.featuresearch.FeatureCorpus;
import com.naturalist.featuresearch.FeatureSearch;
import com.naturalist.featuresearch.InMemoryFeatureSearch;
import com.naturalist.insects.lifestage.InsectLifeStageQuery;
import com.naturalist.observation.ImageStore;
import com.naturalist.observation.filesystem.FilesystemImageStore;
import com.naturalist.resilience.Resilience;
import com.naturalist.test.TestDirectories;
import com.naturalist.test.context.Provisions;
import com.naturalist.test.context.TestContext;
import com.naturalist.textgeneration.NoOpTextGenerationService;
import com.naturalist.textgeneration.TextGenerationService;
import com.naturalist.usage.IdentificationBudget;
import com.naturalist.vision.NoOpVisionService;
import com.naturalist.vision.VisionService;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Set;

/**
 * The test context of {@code com.naturalist.insects}. The graph builds the package's
 * {@code @DomainService} implementations and {@code @MockDomainService} repository mocks by reflection;
 * this context provides the outside-world ports the identification and photo paths need, as
 * no-ops, plus the feature search the console composition root wires over {@link InsectQuery}.
 * It also provides {@link InsectFactory}, which is no {@code @DomainService} (production builds it
 * inside {@link InsectQueryImpl}), from the same queries the graph builds.
 * Library classes come from library's own context; life-stage classes from
 * {@link com.naturalist.insects.lifestage.InsectLifeStageTestContextInternal}.
 */
public class InsectsTestContextInternal extends TestContext {

    /** Confirms nothing and fetches no content: the identification path is not exercised here. */
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

    public InsectsTestContextInternal() {
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    protected void provisions(Provisions p) {
        p.provide(ImageStore.class, g -> tempImageStore());
        p.provide(Resilience.class, g -> Resilience.noOp());
        p.provide(IdentificationBudget.class, g -> IdentificationBudget.noOp());
        p.provide(VisionService.class, g -> new NoOpVisionService());
        p.provide(TextGenerationService.class, g -> new NoOpTextGenerationService());
        p.provide(ExternalAuthority.class, g -> NO_OP_AUTHORITY);
        p.provide((Class) FeatureSearch.class, g -> new InMemoryFeatureSearch<>(() -> g.get(InsectQuery.class)
                .features().corpus().map(f -> new FeatureCorpus.Indexed<>(f.id(), f.value()))));
        p.provide(InsectFactory.class, g -> new InsectFactory(
                g.get(InsectQuery.SpeciesQuery.class), g.get(InsectQuery.ImageQuery.class),
                g.get(InsectQuery.GenusQuery.class), g.get(InsectQuery.FamilyQuery.class),
                g.get(InsectQuery.OrderQuery.class), g.get(InsectLifeStageQuery.class),
                g.get(InsectCitationQueryImpl.class), g.get(InsectFeatureQueryImpl.class),
                g.get(InsectQuery.FunctionalRoleQuery.class)));
    }

    private static ImageStore tempImageStore() {
        try {
            return new FilesystemImageStore(TestDirectories.create("insects-images"), Resilience.noOp());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
