package com.naturalist.zone;

import com.naturalist.ddd.ValueObject;
import com.naturalist.measurements.SlopeDegrees;
import com.naturalist.observability.Constraints;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * The compass orientation of a Zone — the direction a slope, surface, or structure faces.
 * <p>
 * At Oak Vista: the apiary hive entrance faces {@code EAST}; the backyard garden bed
 * faces {@code SOUTH}; all raised beds are flat ({@code slopeDegrees = 0.0°}).
 */
public record Aspect(
        CompassDirection primaryDirection,
        @Nullable CompassDirection secondaryDirection,
        SlopeDegrees slopeDegrees
) implements ValueObject {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .notNull(this, Aspect::primaryDirection, "primaryDirection")
                .namedValue(slopeDegrees, "slopeDegrees");
    }
}
