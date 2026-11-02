package com.naturalist.insects;

import com.naturalist.exception.InvariantViolationException;
import com.naturalist.naturalist.NaturalistName;
import com.naturalist.observability.Observer;
import com.naturalist.observation.OrganismObservation;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InsectViewsTest {
    private static final Observer observer = Observer.forClass(InsectViewsTest.class);

    @Test
    void viewer_rejectsObservationKeyedByForeignImage() {
        OrganismObservation<InsectObservationId, InsectRankName> observation = new OrganismObservation<InsectObservationId, InsectRankName>(
                InsectObservationId.create(), NaturalistName.of("pat"), InsectSpeciesName.of("battus-philenor"),
                Instant.parse("2026-07-01T12:00:00Z"), null, null, null);
        InsectViews.Viewer viewer = new InsectViews.Viewer(
                true, InsectEntityCollections.ImageCollection.empty(), Map.of(InsectImageId.create(), observation));

        assertThatThrownBy(() -> observer.observable(viewer, "viewer").throwWhenInvalid())
                .isInstanceOf(InvariantViolationException.class);
    }

    @Test
    void viewer_anonymousIsValid() {
        assertThat(observer.observable(InsectViews.Viewer.anonymous(), "viewer").violations()).isEmpty();
    }
}
