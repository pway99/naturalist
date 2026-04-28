package com.naturalist.arachnids;

import com.naturalist.ddd.NamedEntity;
import com.naturalist.fieldnotes.Description;
import com.naturalist.observability.Constraints;
import com.naturalist.taxonomy.TaxonomicClassification;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * A catalogued arachnid species (class Arachnida) observed at Oak Vista.
 * <p>
 * All arachnids in the Oak Vista catalog are beneficial predators. The
 * {@link HuntingStrategy} captures the predatory mode, which governs microhabitat
 * preference and management implications (e.g. retaining ground cover for ambush
 * hunters, avoiding broad-spectrum pesticide treatments that decimate web builders).
 * <p>
 * Arachnida includes spiders (Araneae), predatory mites (Acari), harvestmen
 * (Opiliones), and pseudoscorpions (Pseudoscorpiones). The order field on
 * {@link TaxonomicClassification} distinguishes these at the coarsest level.
 * <p>
 * Unlike insects, arachnids do not fill pollinator or decomposer roles at Oak Vista;
 * their ecological service is invertebrate pest suppression. The
 * {@code huntingStrategy} provides more domain-meaningful differentiation than
 * a generic guild enum would for this class.
 */
public record ArachnidSpecies(
        ArachnidName name,
        TaxonomicClassification taxonomy,
        Description description,
        HuntingStrategy huntingStrategy,
        @Nullable String sightingNotes
) implements NamedEntity<ArachnidName> {

    /**
     * Whether this species constructs a web to intercept prey passively.
     */
    public boolean isWebBuilder() {
        return huntingStrategy == HuntingStrategy.WEB_BUILDER;
    }

    /**
     * Whether this species requires open substrate (ground or vegetation surface)
     * and benefits from reduced surface mulch in foraging corridors.
     */
    public boolean isActivePursuer() {
        return huntingStrategy == HuntingStrategy.ACTIVE_PURSUIT;
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityName(name, "name")
                .valueObject(taxonomy, "taxonomy")
                .valueObject(description, "description")
                .notNull(this, ArachnidSpecies::huntingStrategy, "huntingStrategy");
    }
}
