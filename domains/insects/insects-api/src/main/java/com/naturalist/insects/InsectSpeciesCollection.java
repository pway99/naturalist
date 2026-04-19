package com.naturalist.insects;

import com.naturalist.ddd.BehavioralCollection;

import java.util.Collection;
import java.util.List;

public final class InsectSpeciesCollection extends BehavioralCollection<InsectSpecies> {

    InsectSpeciesCollection(Collection<InsectSpecies> species) {
        super(species);
    }

    public static InsectSpeciesCollection of(Collection<InsectSpecies> species) {
        return new InsectSpeciesCollection(species);
    }

    public static InsectSpeciesCollection empty() {
        return new InsectSpeciesCollection(List.of());
    }
}
