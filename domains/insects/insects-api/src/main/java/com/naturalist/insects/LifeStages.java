package com.naturalist.insects;

import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.Constraints;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * The documented life cycle stages of an insect species.
 * <p>
 * {@code adult} is always present — it is the stage at which field identification
 * occurs and the stage that defines the species' ecological guild at Oak Vista.
 * <p>
 * {@code egg} and {@code larva} are nullable for two distinct reasons:
 * <ul>
 *   <li>{@code egg} — egg-stage data may simply not be documented for the species
 *       at catalog level, even if the species is holometabolous.</li>
 *   <li>{@code larva} — hemimetabolous orders (Blattodea, Orthoptera, Hemiptera)
 *       produce nymphs rather than morphologically distinct larvae; their immature
 *       stages are not modelled here. A null {@code larva} on a holometabolous
 *       species means the larval stage has not yet been catalogued, not that it
 *       does not exist.</li>
 * </ul>
 * <p>
 * The model deliberately omits a {@code pupa} stage. Pupal ecology at Oak Vista
 * reduces to substrate requirements (soil depth, plant litter) which belong in
 * {@link HabitatRequirements}. If pupal biology warrants modelling — for example,
 * to express the duration of vulnerability to soil disturbance — a {@code PupaStage}
 * can be added when the first domain entity requires it.
 */
public record LifeStages(
        @Nullable EggStage egg,
        @Nullable LarvaStage larva,
        AdultStage adult
) implements ValueObject {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i.notNull(this, LifeStages::adult, "adult");
    }
}
