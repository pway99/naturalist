package com.naturalist.insects.lifestages;

import com.naturalist.ddd.ValueObject;
import com.naturalist.habitat.HabitatProfile;
import com.naturalist.observability.Constraints;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Where a life stage lives at Oak Vista, from the insect's perspective.
 * <p>
 * Stages have distinct habitat footprints within a single species: a
 * <i>Battus philenor</i> egg is on the underside of an <i>Aristolochia
 * californica</i> leaf; the larva works through the same plant's foliage; the
 * pupa attaches to a nearby stem or structural element with a cremaster and silk
 * girdle; the adult ranges across the garden between nectar sources and the host
 * plant's location. Each stage has its own answer to "where does it live."
 * <p>
 * {@code profile} carries the structured habitat classification from the
 * {@code habitat} kernel — the same zone/moisture/light/vertical-layer vocabulary
 * used by plants, arachnids, and other organisms at their catalog level. This is
 * the cross-domain habitat vocabulary; the DAG layer and any habitat-overlap
 * queries reason over this structured form.
 * <p>
 * The narrative fields complement the structured profile with insect-specific
 * detail the profile cannot express:
 * <ul>
 *   <li>{@code substrate} — what the stage physically sits on or in. Leaf undersides
 *       for eggs, host plant foliage for phytophagous larvae, stems or structures
 *       for pupae, flowers and perches for adults.</li>
 *   <li>{@code microclimate} — microhabitat conditions the stage requires:
 *       temperature range, humidity, sun exposure. Often narrower than the containing
 *       habitat profile; a larva may require leaf-underside humidity that the zone-
 *       level profile does not capture.</li>
 *   <li>{@code spatialNotes} — where within the garden the stage is found. Relevant
 *       when the stage concentrates in particular zones (e.g., adult flight
 *       concentrating around the pipevine trellis rather than ranging uniformly).</li>
 * </ul>
 * <p>
 * {@code profile} is required — every documented stage has at least a coarse
 * structured habitat classification. Narrative fields are nullable and populated as
 * observation accumulates.
 */
public record StageHabitat(
        HabitatProfile profile,
        @Nullable String substrate,
        @Nullable String microclimate,
        @Nullable String spatialNotes
) implements ValueObject {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i.valueObject(profile, "profile");
    }
}
