package com.naturalist.insects;

import com.naturalist.taxonomy.TaxonomicClassification;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.util.List;

/**
 * Domain-typed result of vision identification — carries the identified rank
 * entity (polymorphic), taxonomy, identification metadata, structured
 * features, and an optional reference URL suggested by the vision model.
 */
record InsectIdentificationResult(
        IdentifiedRankEntity identifiedEntity,
        TaxonomicClassification taxonomy,
        Identification identification,
        List<String> features,
        @Nullable URI referenceUrl
) {}
