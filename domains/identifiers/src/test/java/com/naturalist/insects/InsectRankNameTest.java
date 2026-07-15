package com.naturalist.insects;

import com.naturalist.taxonomy.LinealRank;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InsectRankNameTest {

    @Test
    void species() {
        InsectRankName result = InsectRankName.of("battus-philenor", LinealRank.SPECIES);
        assertThat(result).isInstanceOf(InsectSpeciesName.class);
        assertThat(result.value()).isEqualTo("battus-philenor");
    }

    @Test
    void genus() {
        InsectRankName result = InsectRankName.of("battus", LinealRank.GENUS);
        assertThat(result).isInstanceOf(InsectGenusName.class);
        assertThat(result.value()).isEqualTo("battus");
    }

    @Test
    void family() {
        InsectRankName result = InsectRankName.of("papilionidae", LinealRank.FAMILY);
        assertThat(result).isInstanceOf(InsectFamilyName.class);
        assertThat(result.value()).isEqualTo("papilionidae");
    }

    @Test
    void order() {
        InsectRankName result = InsectRankName.of("lepidoptera", LinealRank.ORDER);
        assertThat(result).isInstanceOf(InsectOrderName.class);
        assertThat(result.value()).isEqualTo("lepidoptera");
    }

    @Test
    void subspecies() {
        InsectRankName result = InsectRankName.of("battus-philenor-hirsuta", LinealRank.SUBSPECIES);
        assertThat(result).isInstanceOf(InsectSubspeciesName.class);
        assertThat(result.value()).isEqualTo("battus-philenor-hirsuta");
    }

    @ParameterizedTest
    @EnumSource(value = LinealRank.class, names = {"KINGDOM", "PHYLUM", "CLASS"})
    void rejectsNonInsectRanks(LinealRank rank) {
        assertThatThrownBy(() -> InsectRankName.of("test", rank))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
