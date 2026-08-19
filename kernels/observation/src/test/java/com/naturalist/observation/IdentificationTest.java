package com.naturalist.observation;

import com.naturalist.exception.InvariantViolationException;
import com.naturalist.observability.MethodObserver;
import com.naturalist.observability.Observer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdentificationTest {

    private static final Observer observer = Observer.forClass(IdentificationTest.class);

    @Test
    void validWhenConfidenceInRangeAndEvidencePresent() {
        var id = new Identification(0.9, "wing venation",
                List.of(new Identification.Candidate("Danaus plexippus", null, 0.1)));

        assertThat(id.confidence()).isEqualTo(0.9);
        assertThat(id.evidence()).isEqualTo("wing venation");
        assertThat(id.alternatives()).hasSize(1);
    }

    @Test
    void outOfRangeConfidenceViolatesInvariant() {
        MethodObserver mo = observer.forMethod("outOfRangeConfidenceViolatesInvariant");
        var id = new Identification(1.5, "wing venation", List.of());

        assertThatThrownBy(() -> mo.observable(id, "id").throwWhenInvalid())
                .isInstanceOf(InvariantViolationException.class);
    }
}
