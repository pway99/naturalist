package com.naturalist.insects;

import com.naturalist.taxonomy.TaxonomicClassification;
import org.jspecify.annotations.Nullable;

/**
 * The domain-typed result of a vision identification. Carries the candidate
 * {@link InsectSpecies} record (ready for catalog insert if new), the full
 * {@link TaxonomicClassification} (so the caller can create missing parent
 * ranks), the model's confidence, supporting evidence, and an optional JSON
 * string of alternatives.
 */
public record InsectIdentificationResult(
        InsectSpecies species,
        TaxonomicClassification taxonomy,
        double confidence,
        String evidence,
        @Nullable String alternativesJson
) {}
