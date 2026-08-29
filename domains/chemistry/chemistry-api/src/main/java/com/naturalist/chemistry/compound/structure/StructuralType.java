package com.naturalist.chemistry.compound.structure;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.naturalist.chemistry.compound.CompoundCategory;
import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.Constraints;

import java.util.function.Consumer;

/**
 * Structural type of a compound — the carbon-skeleton taxonomy for
 * organic molecules, plus explicit {@link Element} and {@link Inorganic}
 * permits for the cases where that taxonomy does not apply.
 * <p>
 * {@code StructuralType} answers <em>what kind of molecule</em> a compound
 * is at the level of its biosynthetic / Lewis-structure family. It is the
 * complement to two other classification axes already on
 * {@code CompoundInfo}:
 * <ul>
 *   <li>{@code ChemicalNature} — organic / inorganic / organometallic.</li>
 *   <li>{@code PhysicalForm} — element / mineral / salt / acid / base /
 *       complex.</li>
 * </ul>
 * Where those axes describe what a compound <em>is</em> in macroscopic
 * terms, {@code StructuralType} describes the carbon framework
 * specifically: alkaloid family, terpenoid carbon-count tier, phenolic
 * subfamily, glycoside type. For inorganic salts, minerals, and pure
 * elements no carbon-skeleton family applies, and rather than encoding
 * that absence as {@code null} the type carries explicit {@link Element}
 * and {@link Inorganic} permits. The field on {@code CompoundInfo} is
 * therefore non-null: a naturalist reading a compound never has to infer
 * meaning from a missing value, and pattern-matching consumers can
 * dispatch on every case exhaustively.
 * <p>
 * The {@link Element} and {@link Inorganic} permits intentionally restate
 * information also visible through {@code ChemicalNature} and
 * {@code PhysicalForm}. They are not redundant — they are the
 * structural-type axis's own way of saying "the carbon-skeleton question
 * has no answer here", which is a different statement from "this molecule
 * has no organic chemistry."
 * <p>
 * <b>Two-axis classification with phytochemistry.</b> The plants domain's
 * {@code PhytochemicalCategory} carries a complementary, coarser axis
 * organised for ecological queries (alkaloid, glucosinolate, latex, …).
 * The two axes are deliberately not collapsed into each other — a thymol
 * record is a {@link Monoterpene} structurally and a
 * {@code PhytochemicalCategory.ESSENTIAL_OIL} ecologically, and a query
 * may want either answer.
 * <p>
 * Sealed so individual permits can carry attached state if and when they
 * need it. Initial permits are stateless record types; treat them as the
 * sealed-interface equivalent of enum constants until a concrete class
 * acquires data. Mirrors the pattern of {@code FunctionalRole} in this
 * same package's {@code role/} sub-package.
 * <p>
 * <b>Adding permits.</b> When a real catalog entry doesn't fit any
 * existing permit, add a new one. The list is intentionally non-exhaustive
 * — an over-eager closed taxonomy would force misclassification and erode
 * the literature value of the catalog. Use {@link OtherOrganic} only as a
 * fallback while a new permit is being agreed upon, never as a permanent
 * home.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
@JsonSubTypes({
        // ── Non-organic — explicit "axis does not apply" permits ────────
        @JsonSubTypes.Type(value = StructuralType.Element.class, name = "ELEMENT"),
        @JsonSubTypes.Type(value = StructuralType.Inorganic.class, name = "INORGANIC"),

        // ── Alkaloids (nitrogen-containing) ──────────────────────────────
        @JsonSubTypes.Type(value = StructuralType.IndoleAlkaloid.class, name = "INDOLE_ALKALOID"),
        @JsonSubTypes.Type(value = StructuralType.TropaneAlkaloid.class, name = "TROPANE_ALKALOID"),
        @JsonSubTypes.Type(value = StructuralType.PurineAlkaloid.class, name = "PURINE_ALKALOID"),
        @JsonSubTypes.Type(value = StructuralType.PyrrolizidineAlkaloid.class, name = "PYRROLIZIDINE_ALKALOID"),
        @JsonSubTypes.Type(value = StructuralType.QuinolineAlkaloid.class, name = "QUINOLINE_ALKALOID"),
        @JsonSubTypes.Type(value = StructuralType.IsoquinolineAlkaloid.class, name = "ISOQUINOLINE_ALKALOID"),
        @JsonSubTypes.Type(value = StructuralType.OtherAlkaloid.class, name = "OTHER_ALKALOID"),

        // ── Terpenoids (isoprenoid by carbon count) ──────────────────────
        @JsonSubTypes.Type(value = StructuralType.Monoterpene.class, name = "MONOTERPENE"),
        @JsonSubTypes.Type(value = StructuralType.Sesquiterpene.class, name = "SESQUITERPENE"),
        @JsonSubTypes.Type(value = StructuralType.Diterpene.class, name = "DITERPENE"),
        @JsonSubTypes.Type(value = StructuralType.Triterpene.class, name = "TRITERPENE"),
        @JsonSubTypes.Type(value = StructuralType.Tetraterpene.class, name = "TETRATERPENE"),

        // ── Phenolic family ──────────────────────────────────────────────
        @JsonSubTypes.Type(value = StructuralType.SimplePhenolic.class, name = "SIMPLE_PHENOLIC"),
        @JsonSubTypes.Type(value = StructuralType.Flavonoid.class, name = "FLAVONOID"),
        @JsonSubTypes.Type(value = StructuralType.Anthocyanin.class, name = "ANTHOCYANIN"),
        @JsonSubTypes.Type(value = StructuralType.Tannin.class, name = "TANNIN"),

        // ── Glycosides (sugar-linked) ────────────────────────────────────
        @JsonSubTypes.Type(value = StructuralType.CardiacGlycoside.class, name = "CARDIAC_GLYCOSIDE"),
        @JsonSubTypes.Type(value = StructuralType.CyanogenicGlycoside.class, name = "CYANOGENIC_GLYCOSIDE"),
        @JsonSubTypes.Type(value = StructuralType.Saponin.class, name = "SAPONIN"),
        @JsonSubTypes.Type(value = StructuralType.OtherGlycoside.class, name = "OTHER_GLYCOSIDE"),

        // ── Sulfur-containing ────────────────────────────────────────────
        @JsonSubTypes.Type(value = StructuralType.Glucosinolate.class, name = "GLUCOSINOLATE"),

        // ── Other organic ────────────────────────────────────────────────
        @JsonSubTypes.Type(value = StructuralType.OrganicAcid.class, name = "ORGANIC_ACID"),
        @JsonSubTypes.Type(value = StructuralType.FattyAcidLipid.class, name = "FATTY_ACID_LIPID"),
        @JsonSubTypes.Type(value = StructuralType.Polysaccharide.class, name = "POLYSACCHARIDE"),
        @JsonSubTypes.Type(value = StructuralType.OtherOrganic.class, name = "OTHER_ORGANIC")
})
public sealed interface StructuralType extends ValueObject {

    /**
     * Default no-op {@code invariants()} — stateless permits have no
     * constraints of their own. Permits that acquire state (e.g. a future
     * {@code IndoleAlkaloid(RingSubstitution substitution)}) override this
     * to declare their own validation. {@code CompoundInfo} validates
     * {@code structuralType} as a {@link ValueObject} via
     * {@code valueObject(...)}, walking whatever invariants the concrete
     * permit defines — call sites do not need to be updated when a permit
     * is promoted from stateless to stateful.
     */
    @Override
    default Consumer<? extends Constraints> invariants() {
        return i -> {
        };
    }

    /**
     * The {@link CompoundCategory} bucket this structural type rolls up into.
     * <p>
     * Each permit's category answer is decided here, in one place, via an
     * exhaustive switch over the sealed permits — the compiler will refuse
     * to compile if a new permit is added without a corresponding case.
     * Consumers compare on {@code category()} (or {@code switch} over it)
     * rather than {@code instanceof}-chaining the sealed permits.
     */
    default CompoundCategory category() {
        return switch (this) {
            // ── Non-organic ──────────────────────────────────────────
            case Element x -> CompoundCategory.ELEMENT;
            case Inorganic x -> CompoundCategory.INORGANIC;

            // ── Alkaloids ────────────────────────────────────────────
            case IndoleAlkaloid x -> CompoundCategory.ALKALOID;
            case TropaneAlkaloid x -> CompoundCategory.ALKALOID;
            case PurineAlkaloid x -> CompoundCategory.ALKALOID;
            case PyrrolizidineAlkaloid x -> CompoundCategory.ALKALOID;
            case QuinolineAlkaloid x -> CompoundCategory.ALKALOID;
            case IsoquinolineAlkaloid x -> CompoundCategory.ALKALOID;
            case OtherAlkaloid x -> CompoundCategory.ALKALOID;

            // ── Terpenoids ──────────────────────────────────────────
            case Monoterpene x -> CompoundCategory.TERPENOID;
            case Sesquiterpene x -> CompoundCategory.TERPENOID;
            case Diterpene x -> CompoundCategory.TERPENOID;
            case Triterpene x -> CompoundCategory.TERPENOID;
            case Tetraterpene x -> CompoundCategory.TERPENOID;

            // ── Phenolic family ─────────────────────────────────────
            case SimplePhenolic x -> CompoundCategory.PHENOLIC;
            case Flavonoid x -> CompoundCategory.FLAVONOID;
            case Anthocyanin x -> CompoundCategory.FLAVONOID;  // anthocyanins are flavonoids
            case Tannin x -> CompoundCategory.TANNIN;

            // ── Glycosides ──────────────────────────────────────────
            case CardiacGlycoside x -> CompoundCategory.GLYCOSIDE;
            case CyanogenicGlycoside x -> CompoundCategory.GLYCOSIDE;
            case Saponin x -> CompoundCategory.SAPONIN;
            case OtherGlycoside x -> CompoundCategory.GLYCOSIDE;

            // ── Sulfur-containing ───────────────────────────────────
            case Glucosinolate x -> CompoundCategory.GLUCOSINOLATE;

            // ── Other organic ───────────────────────────────────────
            case OrganicAcid x -> CompoundCategory.ORGANIC_ACID;
            case FattyAcidLipid x -> CompoundCategory.FATTY_ACID_LIPID;
            case Polysaccharide x -> CompoundCategory.POLYSACCHARIDE;
            case OtherOrganic x -> CompoundCategory.OTHER_ORGANIC;
        };
    }

    /**
     * The stable discriminator string for this permit — the same token the Jackson
     * {@code @JsonSubTypes} mapping uses ({@code "MONOTERPENE"}, {@code "INDOLE_ALKALOID"},
     * …). Exhaustive over the sealed permits, so a new permit will not compile without a
     * corresponding case; {@link #ofKind(String)} is its inverse. Persistence adapters use
     * the pair to store and rebuild a structural type as a plain column, no Jackson.
     */
    default String kind() {
        return switch (this) {
            case Element x -> "ELEMENT";
            case Inorganic x -> "INORGANIC";
            case IndoleAlkaloid x -> "INDOLE_ALKALOID";
            case TropaneAlkaloid x -> "TROPANE_ALKALOID";
            case PurineAlkaloid x -> "PURINE_ALKALOID";
            case PyrrolizidineAlkaloid x -> "PYRROLIZIDINE_ALKALOID";
            case QuinolineAlkaloid x -> "QUINOLINE_ALKALOID";
            case IsoquinolineAlkaloid x -> "ISOQUINOLINE_ALKALOID";
            case OtherAlkaloid x -> "OTHER_ALKALOID";
            case Monoterpene x -> "MONOTERPENE";
            case Sesquiterpene x -> "SESQUITERPENE";
            case Diterpene x -> "DITERPENE";
            case Triterpene x -> "TRITERPENE";
            case Tetraterpene x -> "TETRATERPENE";
            case SimplePhenolic x -> "SIMPLE_PHENOLIC";
            case Flavonoid x -> "FLAVONOID";
            case Anthocyanin x -> "ANTHOCYANIN";
            case Tannin x -> "TANNIN";
            case CardiacGlycoside x -> "CARDIAC_GLYCOSIDE";
            case CyanogenicGlycoside x -> "CYANOGENIC_GLYCOSIDE";
            case Saponin x -> "SAPONIN";
            case OtherGlycoside x -> "OTHER_GLYCOSIDE";
            case Glucosinolate x -> "GLUCOSINOLATE";
            case OrganicAcid x -> "ORGANIC_ACID";
            case FattyAcidLipid x -> "FATTY_ACID_LIPID";
            case Polysaccharide x -> "POLYSACCHARIDE";
            case OtherOrganic x -> "OTHER_ORGANIC";
        };
    }

    /** Reconstructs a permit from its {@link #kind()} discriminator. Inverse of {@code kind()}. */
    static StructuralType ofKind(String kind) {
        return switch (kind) {
            case "ELEMENT" -> new Element();
            case "INORGANIC" -> new Inorganic();
            case "INDOLE_ALKALOID" -> new IndoleAlkaloid();
            case "TROPANE_ALKALOID" -> new TropaneAlkaloid();
            case "PURINE_ALKALOID" -> new PurineAlkaloid();
            case "PYRROLIZIDINE_ALKALOID" -> new PyrrolizidineAlkaloid();
            case "QUINOLINE_ALKALOID" -> new QuinolineAlkaloid();
            case "ISOQUINOLINE_ALKALOID" -> new IsoquinolineAlkaloid();
            case "OTHER_ALKALOID" -> new OtherAlkaloid();
            case "MONOTERPENE" -> new Monoterpene();
            case "SESQUITERPENE" -> new Sesquiterpene();
            case "DITERPENE" -> new Diterpene();
            case "TRITERPENE" -> new Triterpene();
            case "TETRATERPENE" -> new Tetraterpene();
            case "SIMPLE_PHENOLIC" -> new SimplePhenolic();
            case "FLAVONOID" -> new Flavonoid();
            case "ANTHOCYANIN" -> new Anthocyanin();
            case "TANNIN" -> new Tannin();
            case "CARDIAC_GLYCOSIDE" -> new CardiacGlycoside();
            case "CYANOGENIC_GLYCOSIDE" -> new CyanogenicGlycoside();
            case "SAPONIN" -> new Saponin();
            case "OTHER_GLYCOSIDE" -> new OtherGlycoside();
            case "GLUCOSINOLATE" -> new Glucosinolate();
            case "ORGANIC_ACID" -> new OrganicAcid();
            case "FATTY_ACID_LIPID" -> new FattyAcidLipid();
            case "POLYSACCHARIDE" -> new Polysaccharide();
            case "OTHER_ORGANIC" -> new OtherOrganic();
            default -> throw new IllegalArgumentException("Unknown StructuralType kind: " + kind);
        };
    }

    // ── Non-organic — explicit "axis does not apply" permits ───────────

    /**
     * Pure elemental compound — sulfur, copper, boron in unbonded form.
     * Carries no carbon-skeleton family because the compound is monatomic
     * or homonuclear at the molecular level. Use this rather than
     * {@link Inorganic} for compounds with {@code PhysicalForm.ELEMENT}.
     */
    record Element() implements StructuralType {
    }

    /**
     * Inorganic salt, mineral, oxide, or simple inorganic acid — calcium
     * sulfate, calcium carbonate, magnesium sulfate, calcium chloride.
     * The structural-type axis (carbon-skeleton family) has no answer
     * for these compounds; this permit names that absence explicitly so
     * a naturalist never has to infer it from a null value.
     */
    record Inorganic() implements StructuralType {
    }

    // ── Alkaloids ────────────────────────────────────────────────────────

    /**
     * Indole-ring alkaloid — psilocybin, reserpine, vinblastine, strychnine.
     */
    record IndoleAlkaloid() implements StructuralType {
    }

    /**
     * Tropane bicycle — atropine, scopolamine, cocaine.
     */
    record TropaneAlkaloid() implements StructuralType {
    }

    /**
     * Purine-derived methylxanthines — caffeine, theobromine, theophylline.
     */
    record PurineAlkaloid() implements StructuralType {
    }

    /**
     * Pyrrolizidine ring system — senecionine, monocrotaline (Senecio, Crotalaria).
     */
    record PyrrolizidineAlkaloid() implements StructuralType {
    }

    /**
     * Quinoline ring — quinine, camptothecin.
     */
    record QuinolineAlkaloid() implements StructuralType {
    }

    /**
     * Isoquinoline ring — morphine, codeine, berberine.
     */
    record IsoquinolineAlkaloid() implements StructuralType {
    }

    /**
     * Nitrogenous secondary metabolites not in a more specific family — phenanthrenoid alkaloids (e.g. aristolochic acids).
     */
    record OtherAlkaloid() implements StructuralType {
    }

    // ── Terpenoids ──────────────────────────────────────────────────────

    /**
     * C10 isoprenoid — limonene, menthol, thymol, pinene.
     */
    record Monoterpene() implements StructuralType {
    }

    /**
     * C15 isoprenoid — farnesene, gossypol, parthenolide.
     */
    record Sesquiterpene() implements StructuralType {
    }

    /**
     * C20 isoprenoid — taxol, gibberellins, abietic acid.
     */
    record Diterpene() implements StructuralType {
    }

    /**
     * C30 isoprenoid — saponin aglycones, cardenolide aglycones, azadirachtin (tetranortriterpenoid).
     */
    record Triterpene() implements StructuralType {
    }

    /**
     * C40 isoprenoid — carotenoids, lycopene, β-carotene.
     */
    record Tetraterpene() implements StructuralType {
    }

    // ── Phenolic family ─────────────────────────────────────────────────

    /**
     * Single phenol-ring compounds — salicylic acid, vanillin, eugenol.
     */
    record SimplePhenolic() implements StructuralType {
    }

    /**
     * C6-C3-C6 flavonoid skeleton — quercetin, rutin, hesperidin.
     */
    record Flavonoid() implements StructuralType {
    }

    /**
     * Glycosylated flavylium pigments — cyanidin, delphinidin, pelargonidin.
     */
    record Anthocyanin() implements StructuralType {
    }

    /**
     * Polyphenolic astringent — gallic-acid-derived hydrolysable, proanthocyanidin condensed.
     */
    record Tannin() implements StructuralType {
    }

    // ── Glycosides ──────────────────────────────────────────────────────

    /**
     * Steroidal glycosides acting on Na/K-ATPase — digitoxin, oleandrin.
     */
    record CardiacGlycoside() implements StructuralType {
    }

    /**
     * Glycosides releasing HCN on hydrolysis — amygdalin, linamarin, prunasin.
     */
    record CyanogenicGlycoside() implements StructuralType {
    }

    /**
     * Glycosylated triterpene or steroid with surfactant action — glycyrrhizin, ginsenosides.
     */
    record Saponin() implements StructuralType {
    }

    /**
     * Sugar-linked compounds outside the major sub-classes — salicin, arbutin.
     */
    record OtherGlycoside() implements StructuralType {
    }

    // ── Sulfur-containing ──────────────────────────────────────────────

    /**
     * β-thioglucoside-N-hydroxysulfates — sinigrin, glucotropaeolin, glucoraphanin.
     */
    record Glucosinolate() implements StructuralType {
    }

    // ── Other organic ──────────────────────────────────────────────────

    /**
     * Low-molecular-weight organic acids — formic, oxalic, citric, malic, salicylic.
     */
    record OrganicAcid() implements StructuralType {
    }

    /**
     * Fatty acids, fatty-acid salts, glycerolipids — potassium soaps, jojoba waxes.
     */
    record FattyAcidLipid() implements StructuralType {
    }

    /**
     * Sugar polymer chains — pectin, cellulose, gum arabic, plant mucilages.
     */
    record Polysaccharide() implements StructuralType {
    }

    /**
     * Fallback while a new permit is being agreed upon. Never a permanent home.
     */
    record OtherOrganic() implements StructuralType {
    }
}
