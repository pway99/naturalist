package com.naturalist.habitat;

/**
 * The vertical stratum within a habitat in which an organism primarily operates.
 * <p>
 * Vegetation and soil create a layered three-dimensional structure that partitions
 * the habitat into distinct ecological niches. Many species are layer-specialists —
 * bark beetles are confined to the cambium zone of woody stems; ground beetles hunt
 * exclusively at the soil surface; hoverflies forage in the open herbaceous layer
 * while their larvae develop subterraneously. An organism's vertical layer is often
 * more predictive of its ecological interactions than its horizontal habitat zone.
 * <p>
 * A {@link HabitatProfile} carries a {@code Set<VerticalLayer>} because many species
 * use different layers through their life cycle: a hoverfly that oviposits on aphid
 * colonies in the herbaceous layer ({@link #HERBACEOUS_LAYER}) but forages for
 * nectar in the open above it ({@link #SHRUB_LAYER}) occupies both.
 * <p>
 * Constants are ordered top-down from canopy to below ground.
 */
public enum VerticalLayer {

    /**
     * The uppermost tree crowns, typically above 5 m. Relevant for canopy-dependent
     * species: bark-gleaning birds and their associated insects, aerial web spiders,
     * epiphytic lichens, and species that use tree crowns for thermoregulation or
     * dispersal flight.
     */
    CANOPY,

    /**
     * The zone between canopy and shrub layer, roughly 2–5 m. Includes small
     * sub-canopy trees and the upper portions of tall shrubs. Important for
     * wood-boring beetles, stem-nesting Hymenoptera, and birds that forage
     * in the mid-story.
     */
    UNDERSTORY,

    /**
     * Woody shrubs from roughly 0.5 m to 2 m in height. A structurally complex
     * layer providing nesting cover for bumble bees, foraging habitat for
     * parasitoid wasps, and overwintering sites in pithy or hollow stems.
     */
    SHRUB_LAYER,

    /**
     * Low herbaceous vegetation from ground level to roughly 0.5 m: annuals,
     * perennials, grasses, and forbs. The primary foraging layer for most
     * flower-visiting insects and the oviposition zone for the majority of
     * phytophagous species in a garden context.
     */
    HERBACEOUS_LAYER,

    /**
     * The soil surface, leaf litter, and coarse woody debris lying directly
     * on the ground. The most species-rich invertebrate layer by density:
     * ground beetles, rove beetles, centipedes, woodlice, springtails, and
     * the predators that hunt them. Physical structure (mulch depth, surface
     * roughness) directly controls population density here.
     */
    GROUND_SURFACE,

    /**
     * Below the soil surface. Occupied by root feeders, soil-dwelling
     * predators (nematodes, mole crickets), pupal chambers of many Lepidoptera
     * and Coleoptera, nesting galleries of solitary bees, earthworm burrows,
     * and hyphal networks. Sensor data from WH51 probes characterises
     * the physical conditions of this layer.
     */
    SUBTERRANEAN
}
