package com.naturalist.plants;

/**
 * The growth form and life cycle duration of a {@link PlantSpecies} at Oak Vista.
 * <p>
 * Life form governs management cadence — annuals are replanted or allowed to
 * self-seed each season; perennials require different pruning and division
 * schedules; trees have multi-decade management horizons; grasses require
 * specific mowing height protocols (tall fescue at 4–6 inches minimum per
 * the Oak Vista IPM protocol for leafhopper suppression and skipper larval habitat).
 */
public enum PlantLifeForm {

    /**
     * Completes its full life cycle (germination, flowering, seed set, death)
     * within a single growing season. At Oak Vista: dill, alyssum, crimson clover,
     * tomatoes, sweet pea. Many Oak Vista annuals are prolific self-seeders that
     * naturalize effectively once established.
     */
    ANNUAL,

    /**
     * Lives for three or more seasons, dying back to roots in cool/dry periods
     * and regrowing. At Oak Vista: white clover, wild violet, creeping thyme,
     * sage, borage (self-seeding perennial), tall fescue, scented geranium.
     */
    PERENNIAL,

    /**
     * A climbing or scrambling plant that uses a support structure (fence, trellis,
     * stake) to grow vertically. At Oak Vista: California Pipevine (fence trellis),
     * Passiflora edulis (steel trellis), ornamental Passiflora, sweet pea (fence vine).
     * Vines may be annual or perennial — life cycle is captured in the life form
     * for management purposes regardless of duration.
     */
    VINE,

    /**
     * A woody-stemmed plant without a single dominant trunk; multi-stemmed and
     * typically under 5 metres. At Oak Vista: scented geranium hedge
     * (Pelargonium graveolens, 6–8 feet wide).
     */
    SHRUB,

    /**
     * A woody plant with a single dominant trunk reaching over 3 metres.
     * At Oak Vista: peach, nectarine, Asian pear, fig, Pineapple Guava, citrus,
     * persimmon. Trees have multi-decade management horizons and form the
     * structural backbone of the front yard orchard.
     */
    TREE,

    /**
     * A graminoid (grass or grass-like) plant grown as a lawn, meadow, or cover.
     * At Oak Vista: tall fescue lawn maintained at 4–6 inches for leafhopper
     * suppression and skipper larval habitat. Also includes planned summer
     * cover crop grasses (sorghum-sudangrass Phase 2b).
     */
    GRASS
}
