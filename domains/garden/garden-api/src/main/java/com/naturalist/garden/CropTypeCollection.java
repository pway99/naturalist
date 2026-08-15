package com.naturalist.garden;

import com.naturalist.ddd.BehavioralCollection;

import java.util.Collection;
import java.util.List;

/**
 * Multi-result return type for {@link CropType} queries (ADR-011).
 */
public final class CropTypeCollection extends BehavioralCollection<CropType> {

    CropTypeCollection(Collection<CropType> crops) {
        super(crops);
    }

    public static CropTypeCollection of(Collection<CropType> crops) {
        return new CropTypeCollection(crops);
    }

    public static CropTypeCollection empty() {
        return new CropTypeCollection(List.of());
    }
}
