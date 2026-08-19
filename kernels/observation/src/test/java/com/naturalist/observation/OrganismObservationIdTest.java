package com.naturalist.observation;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class OrganismObservationIdTest {
    @Test
    void createMintsAValidUuidV7() {
        OrganismObservationId id = OrganismObservationId.create();
        assertThat(id.isValid()).isTrue();
    }

    @Test
    void distinctIdsWithSameUuidAreEqual() {
        var id = OrganismObservationId.create();
        assertThat(OrganismObservationId.of(id.value())).isEqualTo(id);
    }
}
