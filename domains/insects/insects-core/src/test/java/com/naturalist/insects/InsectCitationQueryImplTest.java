package com.naturalist.insects;

import com.naturalist.data.NaturalistDatabaseExtension;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.library.CitationAssociationQuery;
import com.naturalist.library.LibraryTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InsectCitationQueryImplTest {

    @RegisterExtension
    NaturalistDatabaseExtension db = NaturalistDatabaseExtension.create();

    FamilyRepositoryMock familyRepository = new FamilyRepositoryMock(db);
    GenusRepositoryMock genusRepository = new GenusRepositoryMock(db);
    SpeciesRepositoryMock speciesRepository = new SpeciesRepositoryMock(db);

    InsectQuery.FamilyQuery familyQuery = new FamilyQueryImpl(familyRepository);
    InsectQuery.GenusQuery genusQuery = new GenusQueryImpl(genusRepository, familyQuery);
    InsectQuery.SpeciesQuery speciesQuery = new SpeciesQueryImpl(speciesRepository, genusQuery);

    CitationAssociationQuery citationAssociationQuery =
            LibraryTestContext.create(db).citationAssociationQuery();

    InsectCitationQueryImpl citationQuery = new InsectCitationQueryImpl(
            citationAssociationQuery, speciesQuery, genusQuery, familyQuery);

    @Test
    void findByRankName_rejectsNull() {
        assertThatThrownBy(() -> citationQuery.findByRankName(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("rankName");
    }

    @Test
    void findByRankName_orderWithCitation_returnsCitation() {
        InsectCitationView view = citationQuery.findByRankName(
                InsectOrderName.of("lepidoptera"));

        assertThat(view.subject().value()).isEqualTo("lepidoptera");
        assertThat(view.citations()).hasSize(1);
        assertThat(view.citations().getFirst().citationName().value())
                .isEqualTo("eol-battus-philenor-130502");
        assertThat(view.citations().getFirst().attachedAt().value())
                .isEqualTo("lepidoptera");
    }

    @Test
    void findByRankName_familyInheritsFromOrder() {
        InsectCitationView view = citationQuery.findByRankName(
                InsectFamilyName.of("papilionidae"));

        assertThat(view.subject().value()).isEqualTo("papilionidae");
        assertThat(view.citations()).hasSize(2);
        assertThat(view.citations())
                .extracting(c -> c.attachedAt().value())
                .containsExactlyInAnyOrder("papilionidae", "lepidoptera");
    }

    @Test
    void findByRankName_speciesInheritsFullChain() {
        InsectCitationView view = citationQuery.findByRankName(
                InsectSpeciesName.of("battus-philenor"));

        assertThat(view.subject().value()).isEqualTo("battus-philenor");
        assertThat(view.citations()).hasSize(2);
        assertThat(view.citations())
                .extracting(c -> c.attachedAt().value())
                .containsExactlyInAnyOrder("papilionidae", "lepidoptera");
    }

    @Test
    void findByRankName_orderWithNoCitations_returnsEmpty() {
        // Use an order with no citation associations
        InsectCitationView view = citationQuery.findByRankName(
                InsectOrderName.of("coleoptera"));

        assertThat(view.citations()).isEmpty();
    }
}
