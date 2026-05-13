package com.naturalist.microbes;

import com.naturalist.ddd.NamedEntity;
import com.naturalist.fieldnotes.Description;
import com.naturalist.observability.Constraints;
import com.naturalist.taxonomy.TaxonomicClassification;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * A beneficial entomopathogenic nematode species applied for biological pest control.
 * <p>
 * Entomopathogenic nematodes parasitise soil-dwelling insects by entering through
 * natural body openings or the cuticle, releasing symbiotic bacteria (Xenorhabdus
 * or Photorhabdus spp.) that kill the host within 24–48 hours. Species identity
 * is critical — different species infect different pest stages and require different
 * temperature conditions.
 * <p>
 * <b>Oak Vista nematode register (April 2026):</b>
 * <ul>
 *   <li><i>Steinernema carpocapsae</i> — primary application for Small Hive Beetle
 *       (SHB) larvae in the apiary. Applied as NemaSeek Hi after 7 PM to avoid
 *       UV degradation. <b>S. feltiae does NOT control SHB.</b></li>
 *   <li><i>Steinernema feltiae</i> — fungus gnat larvae (Bradysia spp.) in tomato
 *       beds. Effective at cooler soil temperatures than other Steinernema.</li>
 *   <li><i>Heterorhabditis bacteriophora</i> — cutworms, white grubs, and other
 *       deep-soil pests. Cruiser foraging strategy; penetrates deeper than
 *       ambush-strategy Steinernema.</li>
 * </ul>
 * <p>
 * {@code minApplicationTempF} and {@code maxApplicationTempF} define the soil
 * temperature window for effective application. Outside this range the nematodes
 * are immobile or killed. These bounds are checked by the VarroaManagement and
 * PestManagement application modules before recommending application.
 * <p>
 * {@code applicationAfterSunset} flags species that require application after
 * 7 PM to avoid UV-induced mortality — relevant to operational scheduling.
 */
public record BeneficialNematode(
        NematodeName name,
        TaxonomicClassification taxonomy,
        Description description,
        String primaryTargetPest,
        int minApplicationTempF,
        int maxApplicationTempF,
        boolean applicationAfterSunset,
        ForagingStrategy foragingStrategy,
        @Nullable String applicationNotes
) implements NamedEntity<NematodeName> {

    /**
     * Whether the given soil temperature is within the effective application range.
     *
     * @param soilTempF current soil temperature in °F
     * @return true if application is likely to be effective
     */
    public boolean isEffectiveAtTemperature(int soilTempF) {
        return soilTempF >= minApplicationTempF && soilTempF <= maxApplicationTempF;
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityName(name, "name")
                .valueObject(taxonomy, "taxonomy")
                .valueObject(description, "description")
                .notNull(primaryTargetPest, "primaryTargetPest")
                .notNull(foragingStrategy, "foragingStrategy");
    }
}
