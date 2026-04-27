package com.naturalist.plants.phytochemistry;

/**
 * Coarse ecological/use category for a plant secondary metabolite, from a
 * naturalist's perspective.
 * <p>
 * <b>Two-axis classification.</b> Phytochemistry recognises two orthogonal
 * classification axes for any given compound:
 * <ol>
 *   <li><b>Structural class</b> — what kind of molecule it is by carbon
 *       skeleton (e.g. <i>indole alkaloid</i>, <i>monoterpene</i>,
 *       <i>flavonol</i>). This is a property of the molecule itself and
 *       lives on {@code chemistry.CompoundInfo} (deferred to a follow-up
 *       chemistry-domain change).</li>
 *   <li><b>Ecological/use category</b> — the bucket a field naturalist or a
 *       phytochemistry textbook would use as a section header. This enum.</li>
 * </ol>
 * The two axes are deliberately coarser than a strict structural taxonomy:
 * a plant compound is rarely classified the same way in an ecology paper as
 * in an organic chemistry monograph. Categories here cover the bands that
 * carry shared ecological behaviour — most {@link #ALKALOID} compounds are
 * nitrogenous defensive deterrents regardless of their indole / tropane /
 * pyrrolizidine substructure, and that ecological generalisation is what a
 * naturalist queries on.
 * <p>
 * <b>Adding categories.</b> When a real plant catalog entry doesn't fit any
 * existing constant, prefer adding a new category to misclassifying — the
 * enum is a domain dictionary, not a closed taxonomy. {@link #OTHER} exists
 * only as a fallback while a new category is being agreed upon, never as a
 * permanent home.
 */
public enum PhytochemicalCategory {

    /** Nitrogenous bases — caffeine, nicotine, aristolochic acid, morphine. */
    ALKALOID,

    /** Isoprenoid backbone — limonene, menthol, pyrethrin, taxol. */
    TERPENOID,

    /** Aromatic phenol-ring compounds not otherwise specialised. */
    PHENOLIC,

    /** Phenolic pigments and signaling molecules — anthocyanins, flavonols. */
    FLAVONOID,

    /** Polyphenolic astringents — condensed and hydrolysable tannins. */
    TANNIN,

    /** Sugar-linked compounds — cardiac, cyanogenic, and salicin glycosides. */
    GLYCOSIDE,

    /** Glycosidic foaming triterpenoids/steroids — yucca, soapwort, soyasapogenols. */
    SAPONIN,

    /** Sulphur-containing brassica metabolites — sinigrin, glucotropaeolin. */
    GLUCOSINOLATE,

    /** Non-protein amino acids — canavanine, mimosine, β-N-oxalyl-L-α,β-diaminopropionic acid. */
    NON_PROTEIN_AMINO_ACID,

    /** Volatile aromatic mixture pressed or distilled from tissue — lavender, thyme oils. */
    ESSENTIAL_OIL,

    /** Viscous secretion, often terpenoid — pine resin, frankincense. */
    RESIN,

    /** Milky exudate — Asclepias cardenolides, Papaver opium, dandelion latex. */
    LATEX,

    /** Low-molecular-weight organic acids — citric, oxalic, salicylic, malic. */
    ORGANIC_ACID,

    /** Mucilage and gum polysaccharides — okra mucilage, gum arabic. */
    POLYSACCHARIDE,

    /** Sugars, common amino acids — included for completeness when a primary metabolite carries notable ecological role. */
    PRIMARY_METABOLITE,

    /** Fallback while a new category is being agreed upon. Never a permanent home. */
    OTHER
}
