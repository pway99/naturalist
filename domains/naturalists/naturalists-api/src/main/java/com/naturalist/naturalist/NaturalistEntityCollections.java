package com.naturalist.naturalist;

import com.naturalist.ddd.BehavioralCollection;

import java.util.Collection;
import java.util.List;

/**
 * Namespace for the naturalists sub-context's {@link BehavioralCollection}
 * return types. One file per namespace, nested types for everything inside (ADR-020).
 */
public interface NaturalistEntityCollections {

    final class NaturalistCollection extends BehavioralCollection<Naturalist> {

        NaturalistCollection(Collection<Naturalist> naturalists) {
            super(naturalists);
        }

        public static NaturalistCollection of(Collection<Naturalist> naturalists) {
            return new NaturalistCollection(naturalists);
        }

        public static NaturalistCollection empty() {
            return new NaturalistCollection(List.of());
        }
    }
}
