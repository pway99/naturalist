package com.naturalist.insects;

import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.Constraints;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

/**
 * The machine (vision) identification behind a {@link FieldObservation}.
 * <p>
 * Present only on observations created by vision identification; a manual
 * sighting carries a {@code null} {@link FieldObservation#identification()}.
 * Groups the three co-occurring facts of a vision result — the model's
 * {@code confidence}, the {@code evidence} (which visible features supported
 * the call), and the runner-up {@link Candidate}s it considered — so that
 * "was this vision-identified?" is a single null check and the machine data
 * never bleeds into the naturalist's own {@link FieldObservation#notes()}.
 */
public record Identification(
        double confidence,
        String evidence,
        List<Candidate> alternatives
) implements ValueObject {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .inRange(confidence, 0.0, 1.0, "confidence")
                .notBlank(evidence, "evidence")
                .valueObjectCollection(this, Identification::alternatives, "alternatives");
    }

    /**
     * A runner-up candidate the vision model weighed but did not select.
     * <p>
     * {@code scientificName} is a plain {@link String}, not a typed
     * {@link InsectSpeciesName}: an alternative may name a species absent from
     * the catalog, so it carries no cross-entity reference. {@code commonName}
     * is nullable — the model does not always supply one.
     */
    public record Candidate(
            String scientificName,
            @Nullable String commonName,
            double confidence
    ) implements ValueObject {

        @Override
        public Consumer<? extends Constraints> invariants() {
            return i -> i
                    .notBlank(scientificName, "scientificName")
                    .inRange(confidence, 0.0, 1.0, "confidence");
        }
    }
}
