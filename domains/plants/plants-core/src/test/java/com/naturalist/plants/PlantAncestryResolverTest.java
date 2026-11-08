package com.naturalist.plants;

import com.naturalist.data.NaturalistTestExtension;
import com.naturalist.test.context.Wired;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

class PlantAncestryResolverTest {

    @RegisterExtension
    final NaturalistTestExtension nte = NaturalistTestExtension.create();

    @Wired PlantAncestryResolver resolver;

    @Test
    void ancestry_ofGenus_isAncestorFirst() {
        // helianthus → asteraceae → asterales; returned ancestor-first (order → … → subject).
        assertThat(resolver.ancestry(PlantGenusName.of("helianthus")))
                .containsExactly(
                        PlantOrderName.of("asterales"),
                        PlantFamilyName.of("asteraceae"),
                        PlantGenusName.of("helianthus"));
    }

    @Test
    void ancestry_ofOrder_isSelfOnly() {
        assertThat(resolver.ancestry(PlantOrderName.of("asterales")))
                .containsExactly(PlantOrderName.of("asterales"));
    }
}
