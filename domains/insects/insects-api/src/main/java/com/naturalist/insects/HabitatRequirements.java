package com.naturalist.insects;

import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.Constraints;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * The habitat conditions an insect species requires to persist at Oak Vista.
 * <p>
 * These are the insect's requirements — what it needs from the environment —
 * not a description of the habitat itself. The habitat as a place belongs to
 * another domain; these fields describe the functional relationship between
 * the insect and its environment from the insect's perspective.
 * <p>
 * All fields are nullable: requirements are documented as they become known
 * and not every species will have all four axes characterised at catalog level.
 * <p>
 * <b>Fields:</b>
 * <ul>
 *   <li>{@code nectarSources} — floral resources required by adults; particularly
 *       relevant for parasitoids and pollinators whose adult longevity and
 *       fecundity depend on accessible nectar and pollen.</li>
 *   <li>{@code shelter} — daytime refuge, nesting substrate, or overwintering
 *       site. Examples: coarse mulch (ground beetles, field roaches), dead wood
 *       (carpenter bees), bare soil patches (Andrena, Halictus).</li>
 *   <li>{@code preyAvailability} — prey or host density threshold required to
 *       sustain a breeding population; relevant for predators and parasitoids.
 *       Guides decisions about maintaining aphid refugia on border plants.</li>
 *   <li>{@code lighting} — response to artificial light at night. Lacewings and
 *       crane flies are positively phototactic; this affects where adults
 *       aggregate and what microhabitats support dispersal.</li>
 * </ul>
 */
public record HabitatRequirements(
        @Nullable String nectarSources,
        @Nullable String shelter,
        @Nullable String preyAvailability,
        @Nullable String lighting
) implements ValueObject {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> {};
    }
}
