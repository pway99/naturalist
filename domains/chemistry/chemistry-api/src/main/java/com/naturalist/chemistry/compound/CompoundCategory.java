package com.naturalist.chemistry.compound;

/**
 * Coarse compound category along the structural-classification axis — the
 * bucket that {@link com.naturalist.chemistry.compound.structure.StructuralType}
 * permits roll up into for query and pattern-matching purposes.
 * <p>
 * <b>Vocabulary alignment with phytochemistry.</b> The plants domain carries
 * a parallel enum {@code com.naturalist.plants.phytochemistry.PhytochemicalCategory}
 * that organises compounds along the <em>ecological</em> axis. Where the two
 * enums share a constant name, the meaning is the same to a naturalist —
 * a {@code CompoundCategory.ALKALOID} compound is exactly what a botanist
 * means by {@code PhytochemicalCategory.ALKALOID}. The two enums are
 * deliberately separate types because the axes they describe are different:
 * <ul>
 *   <li>{@code CompoundCategory} — structural / carbon-skeleton bucket.
 *       Property of the molecule, plant-agnostic.</li>
 *   <li>{@code PhytochemicalCategory} — ecological / use bucket. Carried
 *       on the plant-side {@code PhytochemicalConstituent}, since the same
 *       molecule may appear under different ecological categories across
 *       plants in the literature.</li>
 * </ul>
 * Vocabulary alignment is the point. A consumer doing a cross-domain query
 * does not have to mentally translate between two unrelated taxonomies; the
 * name {@code "ALKALOID"} carries the same meaning on both sides.
 * <p>
 * Constants present here that are absent from {@code PhytochemicalCategory}
 * — {@link #ELEMENT}, {@link #INORGANIC}, {@link #FATTY_ACID_LIPID} — are
 * structural-axis-specific. {@code ELEMENT} and {@code INORGANIC} pair with
 * the corresponding {@code StructuralType} permits to give an explicit
 * positive answer when the carbon-skeleton question does not apply.
 * <p>
 * Constants present in {@code PhytochemicalCategory} but absent here —
 * {@code ESSENTIAL_OIL}, {@code RESIN}, {@code LATEX},
 * {@code NON_PROTEIN_AMINO_ACID}, {@code PRIMARY_METABOLITE} — describe
 * physical state or ecological role rather than carbon-skeleton family,
 * and have no clean structural-type equivalent. They live only on the
 * plants side, by design.
 */
public enum CompoundCategory {

    /** Nitrogenous bases — caffeine, nicotine, aristolochic acid, morphine. */
    ALKALOID,

    /** Isoprenoid backbone — limonene, menthol, taxol, carotenoids. */
    TERPENOID,

    /** Aromatic phenol-ring compounds not otherwise specialised — salicylic acid, vanillin, eugenol. */
    PHENOLIC,

    /** C6-C3-C6 flavonoid skeleton including anthocyanin pigments — quercetin, rutin, cyanidin. */
    FLAVONOID,

    /** Polyphenolic astringents — gallic-acid hydrolysable, proanthocyanidin condensed. */
    TANNIN,

    /** Sugar-linked compounds outside the saponin sub-class — cardiac, cyanogenic, salicin glycosides. */
    GLYCOSIDE,

    /** Glycosylated triterpene or steroid surfactants — glycyrrhizin, ginsenosides, soapwort. */
    SAPONIN,

    /** Sulfur-containing brassica metabolites — sinigrin, glucotropaeolin, glucoraphanin. */
    GLUCOSINOLATE,

    /** Low-molecular-weight organic acids — formic, oxalic, citric, malic, salicylic. */
    ORGANIC_ACID,

    /** Sugar polymer chains — pectin, cellulose, gum arabic, plant mucilages. */
    POLYSACCHARIDE,

    /**
     * Fatty acids, fatty-acid salts, glycerolipids — potassium soaps, jojoba waxes.
     * No equivalent on {@code PhytochemicalCategory}; structural-axis only.
     */
    FATTY_ACID_LIPID,

    /** Organic compounds outside the major sub-classes; matches {@code PhytochemicalCategory.OTHER}. */
    OTHER_ORGANIC,

    /**
     * Pure elemental compound — sulfur, copper, boron in unbonded form.
     * Pairs with {@code StructuralType.Element}. Structural-axis only.
     */
    ELEMENT,

    /**
     * Inorganic salt, mineral, oxide, or simple inorganic acid.
     * Pairs with {@code StructuralType.Inorganic}. Structural-axis only.
     */
    INORGANIC
}
