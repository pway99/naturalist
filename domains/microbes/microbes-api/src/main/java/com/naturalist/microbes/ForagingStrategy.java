package com.naturalist.microbes;

/**
 * The host-seeking foraging strategy of an entomopathogenic nematode species.
 * <p>
 * Foraging strategy determines the vertical distribution of pest control activity
 * in the soil profile and the categories of pest effectively targeted. Matching
 * strategy to the target pest's behaviour and depth is the primary selection criterion
 * when choosing a nematode species for application.
 */
public enum ForagingStrategy {

    /**
     * Waits at or near the soil surface for a mobile host to pass.
     * Effective against actively moving pests that cross the soil surface
     * (e.g. Small Hive Beetle larvae migrating to the soil to pupate,
     * late-instar caterpillars). <i>Steinernema carpocapsae</i> is the
     * archetype — high infectivity against mobile hosts; less effective
     * against sedentary deep-soil pests.
     */
    AMBUSH,

    /**
     * Actively moves through the soil matrix in search of hosts.
     * Effective against sedentary soil-dwelling pests at depth (e.g. white grubs,
     * cutworms in the root zone). <i>Heterorhabditis bacteriophora</i> is the
     * primary Oak Vista cruiser species — penetrates 20–30 cm when irrigated in.
     */
    CRUISER,

    /**
     * Intermediate strategy — some active host-seeking combined with ambush-style
     * nictation. <i>Steinernema feltiae</i> exhibits this strategy, making it
     * effective against both surface fungus gnat larvae and shallow-soil pests.
     */
    INTERMEDIATE
}
