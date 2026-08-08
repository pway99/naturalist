package com.naturalist.insects.console;

import com.naturalist.insects.InsectFamilyName;
import com.naturalist.insects.InsectFeature;
import com.naturalist.insects.InsectFeatureId;
import com.naturalist.insects.InsectFeatureView;
import com.naturalist.insects.InsectOrderName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FeatureGroupTest {

    private static InsectFeatureView.RankedFeature mark(
            String value, com.naturalist.insects.InsectRankName at, int ordinal) {
        return new InsectFeatureView.RankedFeature(
                InsectFeature.of(InsectFeatureId.create(), value), at, ordinal);
    }

    @Test
    void ofGroupsByAssignedRankPreservingViewOrder() {
        var order = InsectOrderName.of("coleoptera");
        var family = InsectFamilyName.of("chrysomelidae");
        var view = new InsectFeatureView(family, List.of(
                mark("hardened forewings", order, 0),
                mark("chewing mouthparts", order, 1),
                mark("strongly domed body", family, 0)));

        var groups = FeatureGroup.of(view);

        assertThat(groups).hasSize(2);
        assertThat(groups.getFirst().rankLabel()).isEqualTo("Order");
        assertThat(groups.getFirst().rankSlug()).isEqualTo("coleoptera");
        assertThat(groups.getFirst().marks())
                .containsExactly("hardened forewings", "chewing mouthparts");
        assertThat(groups.getLast().rankLabel()).isEqualTo("Family");
        assertThat(groups.getLast().marks()).containsExactly("strongly domed body");
    }

    @Test
    void ofSortsWithinARankByOrdinal() {
        var family = InsectFamilyName.of("chrysomelidae");
        var view = new InsectFeatureView(family, List.of(
                mark("second", family, 1),
                mark("third", family, 2),
                mark("first", family, 0)));

        var groups = FeatureGroup.of(view);

        assertThat(groups).hasSize(1);
        assertThat(groups.getFirst().marks()).containsExactly("first", "second", "third");
    }

    @Test
    void ofReturnsEmptyForNullView() {
        assertThat(FeatureGroup.of(null)).isEmpty();
    }

    @Test
    void ofReturnsEmptyForViewWithNoFeatures() {
        var view = new InsectFeatureView(InsectFamilyName.of("chrysomelidae"), List.of());

        assertThat(FeatureGroup.of(view)).isEmpty();
    }
}
