package com.naturalist.insects;

import com.naturalist.taxonomy.LinealRank;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Per-permit verification that {@link InsectRankName#rank()} returns the matching
 * {@link LinealRank} constant. The contract: each leaf permit reports its rank
 * directly, with no down-casting required of the consumer.
 *
 * <p>The reflection-based rank derivation that lived in {@code guild.jte}
 * (class-name string stripping) is the consumer this method replaces.
 */
class InsectRankNameRankTest {

    @Test
    void familyPermitReportsFamilyRank() {
        InsectRankName name = InsectFamilyName.of("papilionidae");
        assertThat(name.rank()).isEqualTo(LinealRank.FAMILY);
    }

    @Test
    void genusPermitReportsGenusRank() {
        InsectRankName name = InsectGenusName.of("battus");
        assertThat(name.rank()).isEqualTo(LinealRank.GENUS);
    }

    @Test
    void speciesPermitReportsSpeciesRank() {
        InsectRankName name = InsectSpeciesName.of("battus-philenor");
        assertThat(name.rank()).isEqualTo(LinealRank.SPECIES);
    }

    @Test
    void subspeciesPermitReportsSubspeciesRank() {
        InsectRankName name = InsectSubspeciesName.of("battus-philenor-hirsuta");
        assertThat(name.rank()).isEqualTo(LinealRank.SUBSPECIES);
    }
}
