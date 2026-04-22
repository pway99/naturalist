package com.naturalist.worms;

import com.naturalist.ddd.NamedEntity;
import com.naturalist.fieldnotes.Description;
import com.naturalist.observability.Constraints;
import com.naturalist.taxonomy.TaxonomicClassification;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * A catalogued annelid worm species (phylum Annelida) observed at Oak Vista.
 * <p>
 * The Annelida documented at Oak Vista are primarily earthworms (order Haplotaxida,
 * family Lumbricidae) — the ecological engineers of the soil food web. Earthworm
 * activity is a direct indicator of soil biological health and is closely linked to
 * organic matter content, moisture, and disturbance history (see TillageEvent in
 * the soil domain for disturbance tracking).
 * <p>
 * {@code preferredDepthCm} captures the primary burrowing depth, relevant to
 * tillage-impact assessments: rototilling to 20 cm disrupts the bulk of earthworm
 * populations and requires a 21-day macropore recovery period before soil structure
 * and worm activity normalize.
 * <p>
 * {@code castingRateGPerDayPerWorm} — a quantitative parameter for the university
 * description level; enables soil amendment rate calculations in the SoilRehabilitation
 * application module.
 */
public record WormSpecies(
        WormName name,
        TaxonomicClassification taxonomy,
        Description description,
        int preferredDepthCm,
        @Nullable Double castingRateGPerDayPerWorm,
        @Nullable String sightingNotes
) implements NamedEntity<WormName> {

    /**
     * Whether this species is a deep burrower (>10 cm), creating vertical macropores
     * that improve drainage — relevant to the slow-drainage issue documented in both
     * Oak Vista beds following rototilling (April 2026).
     */
    public boolean isDeepBurrower() {
        return preferredDepthCm > 10;
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityName(name, "name")
                .notNull(this, WormSpecies::taxonomy, "taxonomy")
                .notNull(this, WormSpecies::description, "description");
    }
}
