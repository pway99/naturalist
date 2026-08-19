package com.naturalist.insects;

import com.naturalist.ddd.Aggregate;
import com.naturalist.observability.Constraints;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.function.Consumer;
import com.naturalist.observation.OrganismImage;
import com.naturalist.observation.OrganismObservation;

/**
 * Write-side consistency boundary for adding a photograph to the catalog.
 * Carries an {@link OrganismImage} and an optional {@link OrganismObservation}
 * (present when a naturalist is signed in). Cross-entity invariants enforce
 * FK consistency: when the observation is present, the image's
 * {@code observationId} must match the observation's {@code id}, and the
 * observation's {@code subject} must match the image's {@code parentName}.
 * When no observation is present, the image must have a {@code null}
 * {@code observationId}.
 */
public record PhotoAddition(
        OrganismImage<InsectImageId, InsectObservationId, InsectRankName> image,
        @Nullable OrganismObservation<InsectObservationId, InsectRankName> observation
) implements Aggregate {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .namedEntity(image, "image")
                .namedEntityOrNull(observation, "observation")
                .isTrue(imageObservationIdMatchesObservation(),
                        "imageObservationIdMatchesObservation")
                .isTrue(observationSubjectMatchesImageParent(),
                        "observationSubjectMatchesImageParent");
    }

    private boolean imageObservationIdMatchesObservation() {
        if (image == null) return true;
        if (observation == null) return image.observationId() == null;
        return Objects.equals(image.observationId(), observation.id());
    }

    private boolean observationSubjectMatchesImageParent() {
        if (image == null || observation == null) return true;
        return observation.subject().equals(image.parentName());
    }
}
