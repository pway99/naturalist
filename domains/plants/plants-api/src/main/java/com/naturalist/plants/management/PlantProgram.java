package com.naturalist.plants.management;

import com.naturalist.ddd.NamedEntity;
import com.naturalist.fieldnotes.Description;
import com.naturalist.observability.Constraints;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.naturalist.plants.PlantFamilyName;
import com.naturalist.plants.PlantGenusName;
import com.naturalist.plants.PlantRankName;
import com.naturalist.plants.PlantRankName;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * A named plant management program at the application's site of cultivation.
 * <p>
 * A {@code PlantProgram} is operational guidance focused on a single ecological
 * concern — pest exclusion, disorder prevention, dormant treatment, larval
 * monitoring, seed-saving — for a specific plant. It is a separate aggregate
 * root from the botanical {@link com.naturalist.plants.PlantSpecies} record:
 * botanical identity is stable across sites and seasons, while management is
 * local, mutable, and grows over time as new constraints, schedules, and
 * observations are recorded.
 * <p>
 * Many programs may exist for one plant. The Pipevine, for example, carries
 * both {@code "pipevine-pesticide-exclusion"} (the absolute zero-pesticide
 * constraint) and {@code "pipevine-larval-monitoring"} (the weekly egg/larva
 * inspection schedule). Each program is named after the activity, not the
 * plant — naming after the plant alone would force artificial uniqueness when
 * the plant has more than one concern to track.
 * <p>
 * The cross-reference to {@link PlantSpeciesName} is a soft FK — there is no
 * compile-time dependency from this module on the plants module's other
 * sub-contexts. RDBMS adapters enforce referential integrity in their
 * own layer.
 * <p>
 * <b>Components.</b>
 * <ul>
 *   <li>{@code description} — Durrell four-level description of the program
 *       itself: what it is, why it matters, how it is run. Required.</li>
 *   <li>{@code constraint} — non-negotiable rule the program imposes
 *       (e.g. the Pipevine's absolute pesticide-exclusion rule). Surfaced by
 *       the PestManagement application module whenever a treatment is
 *       proposed near the referenced plant. Nullable — not every program is
 *       constraint-shaped.</li>
 *   <li>{@code notes} — operational guidance: schedules, observation cadence,
 *       application windows, planting recommendations. Nullable.</li>
 * </ul>
 */
public record PlantProgram(
        PlantProgramName name,
        @JsonTypeInfo(use = Id.NAME, property = "plantRank", include = As.EXTERNAL_PROPERTY)
        @JsonSubTypes({
                @Type(value = PlantFamilyName.class, name = "FAMILY"),
                @Type(value = PlantGenusName.class, name = "GENUS"),
                @Type(value = PlantSpeciesName.class, name = "SPECIES")
        })
        PlantRankName plantName,
        Description description,
        @Nullable String constraint,
        @Nullable String notes
) implements NamedEntity<PlantProgramName> {

    /**
     * Whether this program carries a non-negotiable constraint that must be
     * checked before any pesticide or amendment application near the
     * referenced plant.
     */
    public boolean hasConstraint() {
        return constraint != null;
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityName(name, "name")
                .identifier(plantName, "plantName")
                .valueObject(description, "description");
    }
}
