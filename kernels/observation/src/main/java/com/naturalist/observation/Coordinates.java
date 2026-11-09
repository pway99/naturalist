package com.naturalist.observation;

import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.Constraints;

import java.util.function.Consumer;

/**
 * An exact point on the earth in decimal degrees. It exists only for the length of a request:
 * it is snapped into a {@link GridCell} and then discarded, and the app never stores it.
 * Invariants: latitude lies in [-90, 90] and longitude in [-180, 180].
 */
public record Coordinates(double latitude, double longitude) implements ValueObject {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return c -> c
                .isTrue(latitude >= -90.0 && latitude <= 90.0, "latitude")
                .isTrue(longitude >= -180.0 && longitude <= 180.0, "longitude");
    }
}
