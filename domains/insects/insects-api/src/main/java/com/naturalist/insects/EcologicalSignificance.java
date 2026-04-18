package com.naturalist.insects;

import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.Constraints;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * The ecological significance of an insect species at the Oak Vista garden and
 * broader Chico Central Valley scale.
 * <p>
 * All fields are nullable — significance is documented as it is understood, and
 * not every species will have all three axes characterised at catalog level.
 * <p>
 * <b>Fields:</b>
 * <ul>
 *   <li>{@code indicatorValue} — what the presence of this species signals about
 *       garden ecosystem health. Species that require specific habitat conditions
 *       to persist are reliable indicators that those conditions are met.
 *       Example: natural lacewing colonisation indicates a diverse flowering plant
 *       community providing adult nectar resources without augmentation.</li>
 *   <li>{@code foodWebPosition} — where this species sits in the Oak Vista food web.
 *       Primary consumer, secondary predator, apex invertebrate predator, basal prey.
 *       Describes the trophic cascade effects of population change.</li>
 *   <li>{@code regionalContext} — landscape-scale or biogeographic context specific
 *       to Chico and the Central Valley: seasonal phenology, migration patterns,
 *       voltinism under the warm Mediterranean climate, regional population dynamics.</li>
 * </ul>
 */
public record EcologicalSignificance(
        @Nullable String indicatorValue,
        @Nullable String foodWebPosition,
        @Nullable String regionalContext
) implements ValueObject {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> {};
    }
}
