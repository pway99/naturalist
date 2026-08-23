package com.naturalist.insects;

import com.naturalist.data.NaturalistTestExtension;
import com.naturalist.taxonomy.LinealRank;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class InsectFeatureQueryImplTest {

    @RegisterExtension
    NaturalistTestExtension nte = NaturalistTestExtension.create();

    InsectFamilyRepositoryMock familyRepository = new InsectFamilyRepositoryMock(nte);
    InsectGenusRepositoryMock genusRepository = new InsectGenusRepositoryMock(nte);
    InsectSpeciesRepositoryMock speciesRepository = new InsectSpeciesRepositoryMock(nte);
    InsectFeatureRepositoryMock featureRepository = new InsectFeatureRepositoryMock(nte);
    InsectFeatureAssignmentRepositoryMock assignmentRepository =
            new InsectFeatureAssignmentRepositoryMock(nte);

    InsectQuery.FamilyQuery familyQuery = new InsectFamilyQueryImpl(familyRepository);
    InsectQuery.GenusQuery genusQuery = new InsectGenusQueryImpl(genusRepository, familyQuery);
    InsectQuery.SpeciesQuery speciesQuery = new InsectSpeciesQueryImpl(speciesRepository, genusQuery);
    InsectAncestryResolver ancestryResolver =
            new InsectAncestryResolver(speciesQuery, genusQuery, familyQuery);

    InsectFeatureQueryImpl featureQuery = new InsectFeatureQueryImpl(
            featureRepository, assignmentRepository, ancestryResolver);

    @Test
    void findByRankName_groupsAncestorFirst_familyGroupLast() {
        // battus-philenor: species → ... → papilionidae (family) → lepidoptera (order).
        // JSON seeds order-rank (lepidoptera) assignments; add a family-rank one.
        InsectFeature tailed = InsectFeature.of(InsectFeatureId.create(), "tailed hindwings");
        featureRepository.insert(tailed);
        assignmentRepository.insert(new InsectFeatureAssignment(
                InsectFeatureAssignmentId.create(), tailed.id(),
                InsectFamilyName.of("papilionidae"), 0));

        InsectFeatureView view = featureQuery.findByRankName(
                TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name);

        // Ancestor-first: the ORDER group precedes the FAMILY group.
        assertThat(view.groups()).isNotEmpty();
        assertThat(view.groups().getFirst().rank().rank()).isEqualTo(LinealRank.ORDER);
        InsectFeatureView.RankGroup last = view.groups().getLast();
        assertThat(last.rank()).isEqualTo(InsectFamilyName.of("papilionidae"));
        assertThat(last.features()).extracting(InsectFeature::value)
                .containsExactly("tailed hindwings");
    }

    @Test
    void findByRankName_ordersFeaturesWithinRankByOrdinal() {
        InsectFeatureView view = featureQuery.findByRankName(InsectOrderName.of("lepidoptera"));
        // Single ORDER group; its features are ordinal-ordered (seeded: ordinal 1
        // "complete metamorphosis", ordinal 2 "scaled wings").
        assertThat(view.groups()).hasSize(1);
        assertThat(view.groups().getFirst().rank()).isEqualTo(InsectOrderName.of("lepidoptera"));
        assertThat(view.groups().getFirst().features())
                .extracting(InsectFeature::value)
                .containsExactly("complete metamorphosis", "scaled wings");
    }

    @Test
    void findByRankName_rejectsNull() {
        assertThat(catchThrowable(() -> featureQuery.findByRankName(null)))
                .isInstanceOf(com.naturalist.exception.InvariantViolationException.class);
    }
}
