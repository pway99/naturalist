package com.naturalist.taxonomy;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class RankNameTest {
    @Test
    void exposesSlugAndRank() {
        RankName name = new RankName() {
            @Override public String value() { return "salvia"; }
            @Override public LinealRank rank() { return LinealRank.GENUS; }
        };
        assertThat(name.value()).isEqualTo("salvia");
        assertThat(name.rank()).isEqualTo(LinealRank.GENUS);
    }
}
