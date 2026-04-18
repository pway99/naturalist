package com.naturalist.arachnids;

/**
 * The predatory hunting strategy employed by an arachnid species.
 * <p>
 * All garden arachnids documented at Oak Vista are beneficial predators.
 * The hunting strategy governs microhabitat preference and the types of
 * pest species most effectively suppressed — a distinction that matters
 * for habitat management (leaving plant debris for ambush hunters, open
 * soil surfaces for active pursuers, tall vegetation for web builders).
 */
public enum HuntingStrategy {

    /**
     * Actively pursues prey across the substrate; requires open ground or
     * vegetation surfaces. Wolf spiders (Lycosidae) are the primary Oak Vista
     * representatives — fast, wide-ranging, and effective against crawling pests.
     */
    ACTIVE_PURSUIT,

    /**
     * Waits motionless for prey to approach; relies on camouflage or concealment.
     * Crab spiders (Thomisidae) ambush pollinators on flower heads; jumping spiders
     * (Salticidae) make calculated stalking leaps from a stationary starting position.
     */
    AMBUSH,

    /**
     * Constructs a silk web to passively intercept flying or crawling prey.
     * Orb weavers (Araneidae) and sheet web spiders are typical representatives.
     * Web placement in vegetation corridors maximises interception of flying pests.
     */
    WEB_BUILDER,

    /**
     * Constructs a burrow or retreat, ambushing prey at the entrance.
     * Less common in managed garden beds; more typical of undisturbed ground.
     */
    BURROW_AMBUSH
}
