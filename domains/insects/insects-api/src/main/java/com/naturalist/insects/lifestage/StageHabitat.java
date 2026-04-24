package com.naturalist.insects.lifestage;

import com.naturalist.ddd.ValueObject;
import com.naturalist.habitat.HabitatProfile;
import com.naturalist.observability.Constraints;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Where a life stage lives, from the insect's perspective. Structured profile
 * from the habitat kernel plus narrative detail the profile cannot express.
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
