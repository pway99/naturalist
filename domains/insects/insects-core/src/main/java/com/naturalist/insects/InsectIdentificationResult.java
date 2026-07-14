package com.naturalist.insects;

import com.naturalist.taxonomy.TaxonomicClassification;

/**
 * The domain-typed result of a vision identification. Carries the candidate
 * {@link InsectSpecies} record (ready for catalog insert if new), the full
 * {@link TaxonomicClassification} (so the caller can create missing parent
 * ranks), and the {@link Identification} — confidence, supporting evidence,
 * and typed alternative candidates — ready to attach to a
 * {@link com.naturalist.insects.FieldObservation}.
 */
public record InsectIdentificationResult(
        InsectSpecies species,
        TaxonomicClassification taxonomy,
        Identification identification
) {}
