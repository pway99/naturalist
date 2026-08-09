package com.naturalist.soil;

import com.naturalist.ddd.BehavioralCollection;

import java.util.Collection;
import java.util.List;

/**
 * Multi-result return type for {@link SoilProfileInfo} queries (ADR-011).
 */
public final class SoilProfileInfoCollection extends BehavioralCollection<SoilProfileInfo> {

    SoilProfileInfoCollection(Collection<SoilProfileInfo> profiles) {
        super(profiles);
    }

    public static SoilProfileInfoCollection of(Collection<SoilProfileInfo> profiles) {
        return new SoilProfileInfoCollection(profiles);
    }

    public static SoilProfileInfoCollection empty() {
        return new SoilProfileInfoCollection(List.of());
    }
}
