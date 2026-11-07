package com.naturalist.insects;

import com.naturalist.data.NaturalistTestExtension;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.test.context.Wired;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InsectCitationQueryImplTest {

    @RegisterExtension
    NaturalistTestExtension nte = NaturalistTestExtension.create();

    @Wired InsectCitationQueryImpl insectCitationQuery;

    @Test
    void findByRankName_rejectsNull() {
        assertThatThrownBy(() -> insectCitationQuery.findByRankName(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("rankName");
    }

    @Test
    void findByRankName_orderWithCitation_returnsCitation() {
        InsectCitations view = insectCitationQuery.findByRankName(
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
        InsectCitations view = insectCitationQuery.findByRankName(
                InsectFamilyName.of("papilionidae"));

        assertThat(view.subject().value()).isEqualTo("papilionidae");
        assertThat(view.citations()).hasSize(2);
        assertThat(view.citations())
                .extracting(c -> c.attachedAt().value())
                .containsExactlyInAnyOrder("papilionidae", "lepidoptera");
    }

    @Test
    void findByRankName_speciesInheritsFullChain() {
        InsectCitations view = insectCitationQuery.findByRankName(
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
        InsectCitations view = insectCitationQuery.findByRankName(
                InsectOrderName.of("coleoptera"));

        assertThat(view.citations()).isEmpty();
    }
}
