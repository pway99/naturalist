package com.naturalist.insects;

import org.jspecify.annotations.Nullable;

/**
 * The domain-typed result of a vision identification. Carries the candidate
 * {@link InsectSpecies} record (ready for catalog insert if new), the model's
 * confidence, supporting evidence, and an optional JSON string of alternatives.
 */
public record InsectIdentificationResult(
        InsectSpecies species,
        double confidence,
        String evidence,
        @Nullable String alternativesJson
) {}
