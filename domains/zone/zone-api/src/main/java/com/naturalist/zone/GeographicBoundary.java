package com.naturalist.zone;

import com.naturalist.ddd.ValueObject;
import com.naturalist.measurements.AreaSquareFeet;
import com.naturalist.observability.Constraints;

import java.util.function.Consumer;

public record GeographicBoundary(
        AreaSquareFeet areaSqft,
        BoundaryShape shape
) implements ValueObject {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .namedValue(areaSqft, "areaSqft")
                .notNull(shape, "shape");
    }
}
