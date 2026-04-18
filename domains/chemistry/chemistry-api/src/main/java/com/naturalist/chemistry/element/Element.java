package com.naturalist.chemistry.element;

import com.naturalist.ddd.CatalogEntity;
import com.naturalist.observability.Constraints;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * An atomic element with its physical and chemical properties.
 * <p>
 * Element instances are loaded from elements.json at startup.
 * This class defines the schema — the JSON defines the data.
 * <p>
 * No specific elements (Ca, Mg, K etc) are hardcoded here.
 */
public record Element(
        ElementId id,
        ElementName name,
        String symbol,
        AtomicWeight atomicWeight,
        String ionicForm,
        int ionicCharge
) implements CatalogEntity<ElementId, ElementName> {

    @Override
    public Element withId(ElementId id) {
        return new Element(id, name, symbol, atomicWeight, ionicForm, ionicCharge);
    }

    public boolean isCation() {
        return ionicCharge > 0;
    }

    public boolean isAnion() {
        return ionicCharge < 0;
    }

    public boolean isDivalent() {
        return Math.abs(ionicCharge) == 2;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Element e)) return false;
        return id.equals(e.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return symbol;
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityId(id, "id")
                .entityName(name, "name")
                .namedValue(this, Element::atomicWeight, "atomicWeight");
    }
}
