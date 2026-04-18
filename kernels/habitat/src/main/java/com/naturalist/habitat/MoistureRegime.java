package com.naturalist.habitat;

/**
 * The characteristic soil or substrate moisture regime of a habitat.
 * <p>
 * Moisture regime is the dominant physical filter on plant community composition
 * and, transitively, on the insects, fungi, and soil organisms that depend on
 * those plants. It describes the long-run moisture character of a site rather
 * than transient conditions after irrigation or rainfall.
 * <p>
 * The four constants span the dry-to-wet gradient plus the Mediterranean
 * seasonal pattern that governs most California garden habitats.
 */
public enum MoistureRegime {

    /**
     * Dry, drought-adapted conditions. Soils drain rapidly and remain dry for
     * extended periods. Characteristic of exposed slopes, sandy berms, and
     * chaparral. Species here tolerate or require periodic desiccation.
     */
    XERIC,

    /**
     * Moderate, consistent soil moisture throughout most of the year.
     * The default condition of well-maintained garden beds receiving regular
     * irrigation. Broadest species tolerance; the baseline for most Oak Vista zones.
     */
    MESIC,

    /**
     * Consistently wet or saturated substrate. Pond margins, low-lying areas,
     * heavy clay soils with poor drainage. Selects strongly for adapted specialists:
     * aquatic insects, moisture-loving plants, amphibian-associated species.
     */
    HYDRIC,

    /**
     * Mediterranean wet-winter / dry-summer pattern. Moisture is abundant through
     * autumn and winter then declines sharply through summer. Many California native
     * plants and their associated insects are adapted to — and depend on — this
     * seasonal cycle. Supplemental irrigation disrupts the native phenological cues
     * of unadapted species.
     */
    SEASONALLY_XERIC
}
