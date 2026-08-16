package com.naturalist.plants.phytochemistry;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.chemistry.compound.CompoundName;
import com.naturalist.ddd.EntityName;
import com.naturalist.plants.PlantSpeciesName;

/**
 * Strongly typed natural key for
 * {@code com.naturalist.plants.phytochemistry.PhytochemicalConstituent} —
 * a named link between one plant species and one chemical compound, carrying
 * the role(s) the compound plays in <em>that</em> plant's biology.
 * <p>
 * The slug names the link itself (plant + compound), not either component
 * alone — mirroring the {@code PlantProgramName} convention. A single plant
 * carries many constituents (a single compound may also appear across many
 * plants), so the slug must encode both sides:
 * <pre>
 *   "california-pipevine-aristolochic-acid"
 *   "yarrow-thujone"
 *   "crimson-clover-formononetin"
 * </pre>
 * Naming after the plant alone would force artificial uniqueness when the
 * plant has many constituents to record; naming after the compound alone
 * would collide across plants that share a metabolite.
 * <p>
 * <b>Two construction paths.</b>
 * <ul>
 *   <li>{@link #of(String)} — Jackson deserialization and catalog parsing.
 *       The slug arrives as a string and is wrapped as-is. Validation
 *       happens in the {@link #isValid()} pipeline.</li>
 *   <li>{@link #of(PlantSpeciesName, CompoundName)} — programmatic construction
 *       from the two component slugs. Codifies the {@code "<plant>-<compound>"}
 *       convention so callers do not hand-format the link slug. Both
 *       inputs are already kebab-case slugs validated by their own
 *       {@code EntityName} subtypes; the result is a simple concatenation
 *       with a single hyphen separator.</li>
 * </ul>
 */
public final class PhytochemicalConstituentName extends EntityName {

    private PhytochemicalConstituentName(String value) {
        super(value);
    }

    @JsonCreator
    public static PhytochemicalConstituentName of(String value) {
        return new PhytochemicalConstituentName(value);
    }

    /**
     * Construct a constituent slug from its two component slugs by
     * concatenating {@code plantName} and {@code compoundName} with a
     * single hyphen separator — e.g. {@code PlantSpeciesName.of("california-pipevine")}
     * + {@code CompoundName.of("aristolochic-acid")} →
     * {@code "california-pipevine-aristolochic-acid"}.
     * <p>
     * The result inherits kebab-case validity from its inputs. Length is
     * bounded by {@link #maxLength()} and checked by {@link #isValid()};
     * a real-world plant + compound pair that overruns the limit is the
     * signal to choose a tighter alternative slug at the catalog entry,
     * not to extend the limit silently.
     *
     * @throws NullPointerException if either argument is null
     */
    public static PhytochemicalConstituentName of(PlantSpeciesName plantName, CompoundName compoundName) {
        return new PhytochemicalConstituentName(plantName.value() + "-" + compoundName.value());
    }

    @Override
    protected int maxLength() {
        return 129;
    }
}
