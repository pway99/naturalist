package com.naturalist.soil;

/**
 * The material type of a surface mulch layer applied to a soil sub-zone.
 * <p>
 * {@code MulchType} is a soil domain concept — it describes the material placed on the
 * soil surface for agronomic purposes (moisture retention, soil temperature moderation,
 * weed suppression, erosion control).
 * <p>
 * <b>Ecological bridge to zone-api (application-layer concern):</b> Each {@code MulchType}
 * has an associated thrips overwintering habitat risk. The mapping from {@code MulchType}
 * to {@code ThripsHabitatRisk} (a zone-api type) lives in the application layer — not here
 * — because soil-api and zone-api are peer api modules and neither may depend on the other.
 * When a {@link MulchLayer} change is recorded, the application layer reads the new mulch
 * type, resolves the habitat risk mapping, and updates the zone domain's
 * {@code SubZone.surfaceHabitatRisk} accordingly.
 * <p>
 * <b>Oak Vista mulch history:</b> The backyard garden north section was mulched with straw
 * (STRAW) at ~2 inches depth prior to the April 7, 2026 TSWV/thrips outbreak. Post-outbreak
 * remediation thinned the straw to ~1 inch. A future MulchLayer event will capture any
 * transition to WOOL or another LOW/VERY_LOW habitat risk material.
 */
public enum MulchType {

    /**
     * Cereal straw (wheat, rice, or oat straw).
     * <p>
     * The most common mulch at Oak Vista. Provides good moisture retention and soil
     * temperature moderation. At depth ≥2 inches, straw creates substantial inter-layer
     * void space that supports thrips overwintering and population establishment —
     * the primary ecological risk at this site.
     * <p>
     * Straw mulch was present in the backyard garden north section at the time of the
     * April 7, 2026 TSWV outbreak. Thrips habitat risk: HIGH.
     */
    STRAW,

    /**
     * Pine needle mulch (fallen conifer needles).
     * <p>
     * Acidifying over time as needles decompose — beneficial in alkaline soils like
     * Oak Vista (pH 7.2) as a mild pH buffer. Dense needle mat provides less void
     * space than straw, reducing thrips habitat. Moisture retention is moderate.
     * Thrips habitat risk: LOW.
     */
    PINE_NEEDLE,

    /**
     * Wood chip mulch (coarse arborist chips or bark).
     * <p>
     * High carbon-to-nitrogen ratio; may temporarily immobilise nitrogen at the
     * soil surface if incorporated. Good moisture retention and soil temperature
     * moderation. Provides moderate void space depending on chip size.
     * Thrips habitat risk: MODERATE.
     */
    WOOD_CHIP,

    /**
     * Wool mulch pellets or mats (compressed sheep's wool).
     * <p>
     * Compacts tightly when wet, creating a dense, nearly seamless surface layer
     * with minimal void space for thrips pupation or movement. Releases nitrogen
     * slowly as it decomposes (~10% N by weight). High-cost, low-volume application.
     * Superior thrips habitat suppression among common mulch options.
     * Thrips habitat risk: VERY_LOW.
     */
    WOOL,

    /**
     * Burlap collar or weed mat applied around individual plant stems.
     * <p>
     * Provides localised weed suppression and moisture retention around transplants.
     * Covers only a fraction of the sub-zone surface area; does not provide the
     * broad habitat disruption of a full-coverage mulch layer.
     * Thrips habitat risk: LOW.
     */
    BURLAP_COLLAR,

    /**
     * No mulch — bare, exposed soil surface.
     * <p>
     * Maximum UV exposure, soil surface desiccation, and physical disruption suppress
     * thrips populations by removing protected overwintering habitat. However, bare
     * soil is vulnerable to erosion, moisture loss, and surface crusting. Acceptable
     * only as a short-term post-outbreak management state.
     * Thrips habitat risk: LOW.
     */
    BARE;

    // ── Soil physics ──────────────────────────────────────────────────────────

    /**
     * Whether this mulch type has meaningful soil-acidifying properties over time.
     * <p>
     * Relevant at Oak Vista (pH 7.2) where mild pH buffering toward 6.5–7.0 is
     * agronomically beneficial for micronutrient availability (Mn, Fe, Zn).
     *
     * @return {@code true} for pine needle mulch, which acidifies as needles decompose
     */
    public boolean isAcidifying() {
        return this == PINE_NEEDLE;
    }

    /**
     * Whether this mulch type contributes meaningful nitrogen as it decomposes.
     * <p>
     * Relevant for nitrogen budget planning — wool decomposes to release approximately
     * 10% of its dry weight as nitrogen over the season.
     *
     * @return {@code true} for wool mulch, which releases N during decomposition
     */
    public boolean contributesNitrogen() {
        return this == WOOL;
    }
}
