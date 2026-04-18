package com.naturalist.habitat;

/**
 * The characteristic light exposure of a habitat, measured as daily direct solar
 * irradiance under typical growing-season conditions.
 * <p>
 * Light is the primary energetic driver for plant community structure, which in turn
 * determines the foraging, nesting, and thermoregulatory options available to insects
 * and other organisms. An organism's {@code LightRegime} describes the habitat it
 * uses, not necessarily its physiological light requirement.
 * <p>
 * Boundary definitions follow the widely used horticultural convention (hours of
 * unobstructed direct sun per day) but are interpreted in an ecological rather than
 * agronomic sense.
 */
public enum LightRegime {

    /**
     * Six or more hours of direct sun per day. Open, unshaded conditions.
     * Supports the highest plant species richness in Mediterranean gardens and
     * the greatest diversity of thermophilic insects — solitary bees, hoverflies,
     * butterflies — that require warmth for flight and foraging.
     */
    FULL_SUN,

    /**
     * Three to six hours of direct sun, with shade for part of the day.
     * Transitional between open and shaded; common at east-facing beds and
     * the outer edge of tree canopies. Supports both sun-adapted and shade-tolerant
     * species, often at the highest overall density of ground beetles and carabids.
     */
    PARTIAL_SUN,

    /**
     * Shifting, intermittent light filtered through a moving canopy.
     * Characteristic of woodland understories with gaps, pergolas, or vine-draped
     * structures. Photosynthetic irradiance fluctuates through sunflecks.
     * Supports shade-adapted plants and the invertebrates associated with leaf litter,
     * bark, and decaying wood in low-light conditions.
     */
    DAPPLED,

    /**
     * Fewer than three hours of direct sun; ambient diffuse light only.
     * North-facing walls, dense canopy interiors, and narrow corridors between
     * structures. Species richness is lower but includes specialist shade inhabitants:
     * woodlice, centipedes, fungus gnats, and moisture-dependent decomposers.
     */
    FULL_SHADE
}
