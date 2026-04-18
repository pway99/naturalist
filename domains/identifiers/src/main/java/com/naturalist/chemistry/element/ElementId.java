package com.naturalist.chemistry.element;

import com.naturalist.ddd.PersistenceId;

/**
 * Strongly typed identifier for Element entities.
 * Elements are the atomic building blocks referenced by compounds.
 * Example: ElementId.of("Ca"), ElementId.of("Mg")
 */
public final class ElementId extends PersistenceId<Long> {

    private ElementId(Long value) {
        super(value);
    }

    public static ElementId of(Long value) {
        return new ElementId(value);
    }
}
