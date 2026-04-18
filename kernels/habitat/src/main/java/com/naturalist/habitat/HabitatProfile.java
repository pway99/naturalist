package com.naturalist.habitat;

import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.Constraints;
import org.jspecify.annotations.Nullable;

import java.util.Set;
import java.util.function.Consumer;

/**
 * A structured description of the habitat character associated with an organism or location.
 * <p>
 * {@code HabitatProfile} is the composable type that domain entities embed when they need
 * to describe habitat in structural terms. It combines four orthogonal axes — zone, moisture,
 * light, and vertical layer — into a single value that can be compared, filtered, and
 * reasoned about across domains.
 * <p>
 * <b>Design intent:</b> this is a description of habitat character, not a named habitat
 * entity. "Riparian edge, mesic, partial sun, herbaceous and ground surface" is a profile;
 * "Oak Vista North Creek Bank" is a Zone (a named, located place in the zones domain).
 * The distinction matters: a profile travels with a catalogued organism as a description
 * of where it lives in general; a Zone is a specific, identifiable place on a specific
 * property.
 * <p>
 * <b>Set semantics:</b> both {@code zones} and {@code layers} are sets because many
 * organisms span more than one zone or vertical stratum. A rove beetle that hunts on
 * the ground surface of both compost heaps and cultivated beds occupies
 * {@link HabitatZone#COMPOST_HEAP} and {@link HabitatZone#CULTIVATED} simultaneously.
 * A hoverfly whose larvae develop subterraneously while adults forage in the herbaceous
 * layer occupies both {@link VerticalLayer#SUBTERRANEAN} and
 * {@link VerticalLayer#HERBACEOUS_LAYER}.
 * <p>
 * <b>Nullable axes:</b> {@code moisture}, {@code light}, and {@code layers} are nullable
 * because habitat characterisation is incremental. A catalog entry may know an insect's
 * zones before its moisture preferences are documented. Null means "not yet characterised",
 * not "absent" or "irrelevant".
 * <p>
 * <b>Minimum constraint:</b> {@code zones} must be non-null and non-empty — at least one
 * zone must be known before a profile is meaningful.
 */
public record HabitatProfile(
        Set<HabitatZone> zones,
        @Nullable MoistureRegime moisture,
        @Nullable LightRegime light,
        @Nullable Set<VerticalLayer> layers
) implements ValueObject {

    /**
     * Whether this profile includes the given zone.
     *
     * @param zone the zone to test
     * @return {@code true} if {@code zones} contains {@code zone}
     */
    public boolean occupies(HabitatZone zone) {
        return zones.contains(zone);
    }

    /**
     * Whether this profile includes the given vertical layer.
     * Returns {@code false} if {@code layers} has not been characterised.
     *
     * @param layer the layer to test
     * @return {@code true} if {@code layers} is non-null and contains {@code layer}
     */
    public boolean operatesIn(VerticalLayer layer) {
        return layers != null && layers.contains(layer);
    }

    /**
     * Whether this profile is characterised to the given moisture regime.
     * Equivalent to {@code moisture != null && moisture == regime}.
     *
     * @param regime the moisture regime to test
     * @return {@code true} if moisture has been characterised and matches {@code regime}
     */
    public boolean hasMoistureRegime(MoistureRegime regime) {
        return regime.equals(moisture);
    }

    /**
     * Whether this profile is characterised to the given light regime.
     * Equivalent to {@code light != null && light == regime}.
     *
     * @param regime the light regime to test
     * @return {@code true} if light has been characterised and matches {@code regime}
     */
    public boolean hasLightRegime(LightRegime regime) {
        return regime.equals(light);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .notNull(this, HabitatProfile::zones, "zones");
    }
}
