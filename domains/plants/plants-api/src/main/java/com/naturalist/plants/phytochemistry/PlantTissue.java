package com.naturalist.plants.phytochemistry;

/**
 * The anatomical tissue or organ where a phytochemical constituent is
 * concentrated within the plant.
 * <p>
 * Tissue location is first-class data because the same compound expressed in
 * different tissues carries different ecological meaning. Cyanogenic
 * glycosides in {@link #SEED}s defend the next generation; the same
 * compounds in {@link #LEAF}s deter foliar herbivores; in {@link #FRUIT}
 * pulp they are usually absent (so the fruit can recruit dispersers). A
 * single {@code PhytochemicalConstituent} may declare multiple tissues —
 * compounds rarely stay in one place.
 * <p>
 * {@link #WHOLE_PLANT} is reserved for compounds genuinely distributed
 * throughout — preferring an explicit tissue list keeps catalog entries
 * informative.
 */
public enum PlantTissue {

    /**
     * Below-ground absorbing/anchoring tissue.
     */
    ROOT,

    /**
     * Underground horizontal stem — Iris, ginger, mint runners.
     */
    RHIZOME,

    /**
     * Underground storage organ derived from stem or modified leaves — onion, potato, garlic.
     */
    BULB_OR_TUBER,

    /**
     * Above-ground supporting structure.
     */
    STEM,

    /**
     * Outer woody-stem layer — cinchona quinine, salix salicin.
     */
    BARK,

    /**
     * Heartwood and xylem.
     */
    WOOD,

    /**
     * Foliage.
     */
    LEAF,

    /**
     * Reproductive structure including petal pigments and fragrance.
     */
    FLOWER,

    /**
     * Floral nectar.
     */
    NECTAR,

    /**
     * Pollen grains.
     */
    POLLEN,

    /**
     * Fruit pulp / pericarp.
     */
    FRUIT,

    /**
     * Seeds — embryo and endosperm.
     */
    SEED,

    /**
     * Latex exudate, where present (separate from "compound is in latex" bookkeeping).
     */
    LATEX,

    /**
     * Resin canals or ducts.
     */
    RESIN,

    /**
     * Trichomes / glandular hairs — site of many monoterpene-rich secretions.
     */
    TRICHOME,

    /**
     * Reserved for compounds genuinely systemic — prefer explicit tissues where known.
     */
    WHOLE_PLANT
}
