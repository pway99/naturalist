package com.naturalist.chemistry.compound.depiction;

import com.naturalist.chemistry.compound.CompoundName;
import com.naturalist.ddd.BehavioralCollection;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public final class DepictionCollection extends BehavioralCollection<CompoundDepiction> {

    DepictionCollection(Collection<CompoundDepiction> depictions) {
        super(depictions);
    }

    public static DepictionCollection of(Collection<CompoundDepiction> depictions) {
        return new DepictionCollection(depictions);
    }

    public static DepictionCollection empty() {
        return new DepictionCollection(List.of());
    }

    /**
     * Look up the depiction for a given compound within this collection. The
     * {@code compoundName} field is unique on {@link CompoundDepiction} (one
     * depiction per compound), so the result is at most one entity.
     */
    public Optional<CompoundDepiction> getByCompoundName(CompoundName compoundName) {
        return stream()
                .filter(depiction -> depiction.compoundName().equals(compoundName))
                .findFirst();
    }
}
