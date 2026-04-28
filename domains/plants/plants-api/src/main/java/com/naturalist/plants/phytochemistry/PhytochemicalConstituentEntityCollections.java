package com.naturalist.plants.phytochemistry;

import com.naturalist.ddd.BehavioralCollection;

import java.util.Collection;
import java.util.List;

/**
 * Namespace for the phytochemistry sub-context's {@link BehavioralCollection}
 * return types. One file per namespace, nested types for everything inside (ADR-020).
 */
public interface PhytochemicalConstituentEntityCollections {

    final class PhytochemicalConstituentCollection
            extends BehavioralCollection<PhytochemicalConstituent> {

        PhytochemicalConstituentCollection(Collection<PhytochemicalConstituent> constituents) {
            super(constituents);
        }

        public static PhytochemicalConstituentCollection of(Collection<PhytochemicalConstituent> constituents) {
            return new PhytochemicalConstituentCollection(constituents);
        }

        public static PhytochemicalConstituentCollection empty() {
            return new PhytochemicalConstituentCollection(List.of());
        }
    }
}
