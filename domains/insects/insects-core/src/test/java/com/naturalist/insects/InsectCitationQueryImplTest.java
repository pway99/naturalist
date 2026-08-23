package com.naturalist.insects;

import com.naturalist.data.NaturalistTestExtension;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.library.CitationAssociationQuery;
import com.naturalist.library.CitationQuery;
import com.naturalist.library.LibraryTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InsectCitationQueryImplTest {

    @RegisterExtension
    NaturalistTestExtension nte = NaturalistTestExtension.create();

    InsectFamilyRepositoryMock familyRepository = new InsectFamilyRepositoryMock(nte);
    InsectGenusRepositoryMock genusRepository = new InsectGenusRepositoryMock(nte);
    InsectSpeciesRepositoryMock speciesRepository = new InsectSpeciesRepositoryMock(nte);

    InsectQuery.FamilyQuery familyQuery = new InsectFamilyQueryImpl(familyRepository);
    InsectQuery.GenusQuery genusQuery = new InsectGenusQueryImpl(genusRepository, familyQuery);
    InsectQuery.SpeciesQuery speciesQuery = new InsectSpeciesQueryImpl(speciesRepository, genusQuery);

    LibraryTestContext libraryContext = LibraryTestContext.create(nte);
    CitationAssociationQuery citationAssociationQuery = libraryContext.citationAssociationQuery();
    CitationQuery citationQuery = libraryContext.citationQuery();

    InsectAncestryResolver ancestryResolver = new InsectAncestryResolver(speciesQuery, genusQuery, familyQuery);

    InsectCitationQueryImpl insectCitationQuery = new InsectCitationQueryImpl(
            citationAssociationQuery, citationQuery, ancestryResolver);

    @Test
    void findByRankName_rejectsNull() {
        assertThatThrownBy(() -> insectCitationQuery.findByRankName(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("rankName");
    }

    @Test
    void findByRankName_orderWithCitation_returnsCitation() {
        InsectCitationView view = insectCitationQuery.findByRankName(
                InsectOrderName.of("lepidoptera"));

        assertThat(view.subject().value()).isEqualTo("lepidoptera");
        assertThat(view.citations()).hasSize(1);
        assertThat(view.citations().getFirst().citation().name().value())
            .isEqualTo("eol-lepidoptera-747");
        assertThat(view.citations().getFirst().attachedAt().value())
                .isEqualTo("lepidoptera");
    }

    @Test
    void findByRankName_familyInheritsFromOrder() {
        InsectCitationView view = insectCitationQuery.findByRankName(
                InsectFamilyName.of("papilionidae"));

        assertThat(view.subject().value()).isEqualTo("papilionidae");
        assertThat(view.citations()).hasSize(2);
        assertThat(view.citations())
                .extracting(c -> c.attachedAt().value())
                .containsExactlyInAnyOrder("papilionidae", "lepidoptera");
    }

    @Test
    void findByRankName_speciesInheritsFullChain() {
        InsectCitationView view = insectCitationQuery.findByRankName(
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
        InsectCitationView view = insectCitationQuery.findByRankName(
                InsectOrderName.of("coleoptera"));

        assertThat(view.citations()).isEmpty();
    }
}
