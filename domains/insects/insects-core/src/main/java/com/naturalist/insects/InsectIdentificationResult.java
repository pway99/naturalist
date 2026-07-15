package com.naturalist.insects;

import com.naturalist.taxonomy.TaxonomicClassification;

import java.util.List;

/**
 * Domain-typed result of vision identification — carries the identified rank
 * entity (polymorphic), taxonomy, identification metadata, and structured
 * features. Ready to feed into authority validation and catalog persistence.
 */
record InsectIdentificationResult(
        IdentifiedRankEntity identifiedEntity,
        TaxonomicClassification taxonomy,
        Identification identification,
        List<String> features
) {}
