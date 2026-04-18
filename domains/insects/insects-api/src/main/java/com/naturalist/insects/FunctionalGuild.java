package com.naturalist.insects;

/**
 * The ecological role an insect plays in the garden food web and ecosystem services.
 * <p>
 * An insect may belong to multiple guilds simultaneously — a hoverfly is both
 * a predatory larva (PREDATOR) and an adult pollinator (POLLINATOR). The guild
 * set on {@link InsectSpecies} captures all roles the species exhibits at Oak Vista.
 * <p>
 * <b>Guild definitions in the Oak Vista context:</b>
 * <ul>
 *   <li>{@link #PARASITOID} — lays eggs in or on a host insect; larva consumes host.
 *       Tachinid flies and braconid wasps are the primary guild members documented
 *       in the April 2026 Oak Vista ecological assessment.</li>
 *   <li>{@link #PREDATOR} — directly consumes pest insects at adult or larval stage.
 *       Ladybug adults and larvae, hoverfly larvae.</li>
 *   <li>{@link #APEX_PREDATOR} — generalist night hunter consuming a wide spectrum
 *       of prey including other beneficials; net positive at population scale.
 *       Ground beetles (Carabidae) documented as confirmed Oak Vista residents.</li>
 *   <li>{@link #POLLINATOR} — transfers pollen; critical for tomato fruit set.
 *       Native bees (Halictus, Andrena, Xylocopa) and some flies and butterflies.</li>
 *   <li>{@link #DECOMPOSER} — breaks down organic matter; supports soil food web
 *       and nutrient cycling. Crane flies (Tipulidae larvae), field roaches.</li>
 *   <li>{@link #FOOD_WEB} — basal food web support; consumed by vertebrates and
 *       arachnids, providing indirect pest suppression benefits. Field roaches.</li>
 *   <li>{@link #MIGRATORY} — seasonal visitor; population regulated by landscape
 *       dynamics beyond the garden. Painted Lady (Vanessa cardui) migrates
 *       through Central Valley on Pacific Flyway annually.</li>
 *   <li>{@link #KEYSTONE} — disproportionate ecological impact; host-specific or
 *       indicator species. Pipevine Swallowtail (Battus philenor) is obligate on
 *       California Pipevine (Aristolochia californica) — eggs confirmed April 2026.
 *       CRITICAL: never apply pesticides to Pipevine.</li>
 * </ul>
 */
public enum FunctionalGuild {

    /**
     * Parasitic at larval stage — eggs laid in or on a pest host; lethal to host.
     * Primary biocontrol mechanism for lepidopteran and dipteran pests.
     */
    PARASITOID,

    /**
     * Directly kills and consumes pest insects, either as adult, larva, or both.
     */
    PREDATOR,

    /**
     * Generalist predator occupying the apex of the invertebrate food web;
     * net beneficial at population scale despite occasional predation of other beneficials.
     */
    APEX_PREDATOR,

    /**
     * Transfers pollen between flowers; essential for fruit and seed set.
     */
    POLLINATOR,

    /**
     * Breaks down dead organic matter, returning nutrients to the soil food web.
     */
    DECOMPOSER,

    /**
     * Basal prey species whose presence supports vertebrate and arachnid predators
     * that in turn suppress pest populations.
     */
    FOOD_WEB,

    /**
     * Seasonal visitor whose population dynamics are governed by landscape-scale
     * migration patterns rather than garden management.
     */
    MIGRATORY,

    /**
     * Disproportionate ecological importance; indicator or host-specific species
     * whose presence signals ecosystem health and whose loss cascades broadly.
     */
    KEYSTONE
}
