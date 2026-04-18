package com.naturalist.habitat;

/**
 * The broad ecological zone or structural habitat type in which an organism is found.
 * <p>
 * {@code HabitatZone} classifies habitat at the landscape scale — the character of the
 * place rather than its precise location. A single property like Oak Vista may contain
 * several zones simultaneously: a cultivated vegetable bed, a woodland edge where the
 * garden meets a neighbour's oak, a compost heap that functions as a decomposition
 * microhabitat, and bare soil along paths. Many species occupy more than one zone,
 * which is why {@link HabitatProfile} carries a {@code Set<HabitatZone>}.
 * <p>
 * Constants are ordered roughly from open to closed, dry to wet, reflecting the
 * primary gradient along which habitat character shifts in a Mediterranean garden context.
 */
public enum HabitatZone {

    /**
     * Open, unshaded grassland dominated by forbs, grasses, and wildflowers.
     * Supports pollinators, ground-nesting bees, and open-country predators.
     * At Oak Vista: any unmown patch allowed to naturalise.
     */
    MEADOW,

    /**
     * Exposed mineral soil with little or no vegetative cover.
     * Critical for ground-nesting solitary bees (Andrena, Halictus), ground beetles,
     * and ant colonies. Often undervalued in managed gardens but ecologically important.
     */
    BARE_GROUND,

    /**
     * Vegetable beds, annual borders, orchards, and other actively managed plantings.
     * High disturbance, nutrient-rich, structurally variable through the season.
     * The primary zone for most edible-garden beneficials at Oak Vista.
     */
    CULTIVATED,

    /**
     * Linear plantings of shrubs, brambles, or mixed woody species along fence lines,
     * property boundaries, or path edges. Provides nesting cover, overwintering shelter,
     * and movement corridors connecting isolated habitat patches.
     */
    HEDGEROW,

    /**
     * The ecotone between woodland and open ground — structurally the most diverse
     * and species-rich zone. Combines canopy shelter with lateral sun penetration.
     * High value for parasitoids, hoverflies, and edge-hunting predators.
     */
    WOODLAND_EDGE,

    /**
     * Closed-canopy forest interior, shaded understorey. Relevant for fungi-associated
     * species, bark beetles, woodland carabids, and shade-specialist plants.
     */
    WOODLAND,

    /**
     * Dense Mediterranean shrubland (Californian chaparral and equivalent). Dominated
     * by drought-adapted shrubs: Ceanothus, Arctostaphylos, Salvia. High thermal mass;
     * important for native bee overwinter sites in seed heads and hollow stems.
     */
    CHAPARRAL,

    /**
     * Wetlands, pond margins, seasonal pools, and damp low-lying areas. Supports
     * aquatic insects, amphibian predators, and moisture-dependent detritivores.
     */
    WETLAND,

    /**
     * Stream banks, ditch margins, creek-side vegetation. Linear zone of high moisture
     * and structural complexity. Insect corridor and overwintering refuge.
     */
    RIPARIAN,

    /**
     * Active compost heaps and decomposition zones. A distinct microhabitat with elevated
     * temperature, nitrogen, and fungal activity. Supports rove beetles, soldier flies,
     * earthworms, and the predators that hunt them.
     */
    COMPOST_HEAP
}
