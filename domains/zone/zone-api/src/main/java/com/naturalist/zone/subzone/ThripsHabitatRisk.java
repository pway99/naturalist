package com.naturalist.zone.subzone;

/**
 * The habitat quality of a SubZone's current surface material for thrips overwintering
 * and population establishment.
 * <p>
 * {@code ThripsHabitatRisk} is a zone-layer ecological assessment — it represents the
 * consequence of whatever surface material is present, not the material's identity.
 * The material itself (e.g. {@code MulchType}) is managed by the soil domain, which
 * owns surface management. This enum captures the ecological signal that zone-level
 * pest management decisions depend on.
 * <p>
 * <b>Biological basis:</b> Western flower thrips ({@code Frankliniella occidentalis})
 * overwinter and establish populations in loose organic surface material. Straw and
 * coarse litter provide ideal habitat — multiple protected layers, high humidity, and
 * abundant debris for pupation. Compacted or smooth materials (bare soil, burlap collar)
 * provide minimal habitat and reduce the population reservoir between seasons.
 * <p>
 * <b>Oak Vista significance:</b> The April 7, 2026 TSWV outbreak in the backyard garden
 * north section was associated with straw mulch (HIGH habitat risk). Post-outbreak
 * management thinned the north section straw from ~2 inches to ~1 inch. The transition
 * from HIGH to MODERATE risk is recorded by updating the SubZone's
 * {@code surfaceHabitatRisk} field at the application layer, driven by the soil domain's
 * MulchLayer event.
 * <p>
 * <b>Propagation:</b> When the soil domain records a {@code MulchLayer} change, the
 * application service derives the new {@code ThripsHabitatRisk} from the updated
 * {@code MulchType} and writes it back to the SubZone. Zone-api has no compile-time
 * dependency on soil-api; the mapping lives at the application layer.
 */
public enum ThripsHabitatRisk {

    /**
     * Minimal overwintering habitat.
     * <p>
     * Characteristic of dense wool mulch, which compacts tightly and provides
     * little inter-layer space for thrips pupation or movement. Population
     * establishment from overwintering adults is unlikely.
     */
    VERY_LOW,

    /**
     * Low overwintering habitat.
     * <p>
     * Characteristic of bare soil, pine needle mulch, and burlap collars.
     * Surface is either exposed (bare) or insufficiently voluminous to
     * shelter significant thrips populations between seasons.
     * Standard management risk — no elevated outbreak precautions required.
     */
    LOW,

    /**
     * Moderate overwintering habitat.
     * <p>
     * Characteristic of thinned straw (≤1 inch), wood chip mulch, or other
     * medium-density organic materials. Some thrips overwintering is possible;
     * elevated monitoring warranted in early season when host plants emerge.
     */
    MODERATE,

    /**
     * High overwintering habitat.
     * <p>
     * Characteristic of thick straw mulch (≥2 inches). Multiple inter-layer
     * voids, high humidity retention, and abundant organic debris support
     * substantial thrips populations between seasons.
     * <p>
     * The backyard garden north section carried HIGH habitat risk at the time
     * of the April 7, 2026 TSWV outbreak. Straw thinning was the first
     * post-outbreak remediation step.
     */
    HIGH
}
