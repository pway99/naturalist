package com.naturalist.plants.phytochemistry.role;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.Constraints;

import java.util.function.Consumer;

/**
 * Functional role a phytochemical constituent plays within (or because of) a
 * plant — what it <em>does</em> ecologically, medicinally, or commercially.
 * <p>
 * Sealed so individual roles can carry attached state if and when they need
 * it. Initial permits are stateless record types; treat them as the
 * sealed-interface equivalent of enum constants until a concrete role
 * acquires data. If a role acquires state and behavior it should be promoted
 * to a {@code ValueObject} and added to the {@code PhytochemicalConstituent}
 * graph walk.
 * <p>
 * Roles are organised informally along four axes that match the
 * phytochemistry literature and the priorities laid out in the plants
 * sub-context:
 * <ul>
 *   <li><b>Defense</b> — suppression of herbivores, insects, fungi,
 *       microbes, and competing plants.</li>
 *   <li><b>Signaling</b> — attraction (pollinators, seed dispersers,
 *       mycorrhizae) and induced inter-tissue / inter-plant communication.</li>
 *   <li><b>Environmental interaction</b> — UV protection, osmotic and
 *       thermal stress tolerance, heavy-metal tolerance.</li>
 *   <li><b>Medicinal and commercial</b> — pharmaceutical, nutraceutical,
 *       dye, fragrance, fibre, insecticide, and industrial-feedstock uses,
 *       plus toxin labels relevant to humans and livestock.</li>
 * </ul>
 * The axes are documentation, not type structure. A single
 * {@code PhytochemicalConstituent} typically declares several roles across
 * multiple axes — caffeine in {@code coffea-arabica} seeds is at once an
 * {@link InsectDeterrent}, a {@link Pharmaceutical}, and a
 * {@link Nutraceutical}.
 * <p>
 * <b>Adding roles.</b> When real catalog entries call for a role not
 * represented here, add a permit. The list is intentionally non-exhaustive
 * — an over-eager closed taxonomy would force misclassification and erode
 * the literature value of the catalog.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
@JsonSubTypes({
        // ── Defense ──────────────────────────────────────────────────────
        @JsonSubTypes.Type(value = PhytochemicalRole.HerbivoreDeterrent.class, name = "HERBIVORE_DETERRENT"),
        @JsonSubTypes.Type(value = PhytochemicalRole.InsectDeterrent.class, name = "INSECT_DETERRENT"),
        @JsonSubTypes.Type(value = PhytochemicalRole.AntiFungal.class, name = "ANTI_FUNGAL"),
        @JsonSubTypes.Type(value = PhytochemicalRole.AntiMicrobial.class, name = "ANTI_MICROBIAL"),
        @JsonSubTypes.Type(value = PhytochemicalRole.Allelopathic.class, name = "ALLELOPATHIC"),

        // ── Signaling ────────────────────────────────────────────────────
        @JsonSubTypes.Type(value = PhytochemicalRole.InducedVolatileSignal.class, name = "INDUCED_VOLATILE_SIGNAL"),
        @JsonSubTypes.Type(value = PhytochemicalRole.PollinatorAttractant.class, name = "POLLINATOR_ATTRACTANT"),
        @JsonSubTypes.Type(value = PhytochemicalRole.SeedDisperserAttractant.class, name = "SEED_DISPERSER_ATTRACTANT"),
        @JsonSubTypes.Type(value = PhytochemicalRole.MycorrhizalSignal.class, name = "MYCORRHIZAL_SIGNAL"),

        // ── Environmental interaction ───────────────────────────────────
        @JsonSubTypes.Type(value = PhytochemicalRole.UVProtectant.class, name = "UV_PROTECTANT"),
        @JsonSubTypes.Type(value = PhytochemicalRole.StressTolerance.class, name = "STRESS_TOLERANCE"),
        @JsonSubTypes.Type(value = PhytochemicalRole.HeavyMetalChelator.class, name = "HEAVY_METAL_CHELATOR"),

        // ── Medicinal and commercial ─────────────────────────────────────
        @JsonSubTypes.Type(value = PhytochemicalRole.Pharmaceutical.class, name = "PHARMACEUTICAL"),
        @JsonSubTypes.Type(value = PhytochemicalRole.Nutraceutical.class, name = "NUTRACEUTICAL"),
        @JsonSubTypes.Type(value = PhytochemicalRole.DyeSource.class, name = "DYE_SOURCE"),
        @JsonSubTypes.Type(value = PhytochemicalRole.FragranceSource.class, name = "FRAGRANCE_SOURCE"),
        @JsonSubTypes.Type(value = PhytochemicalRole.FlavorSource.class, name = "FLAVOR_SOURCE"),
        @JsonSubTypes.Type(value = PhytochemicalRole.FiberSource.class, name = "FIBER_SOURCE"),
        @JsonSubTypes.Type(value = PhytochemicalRole.InsecticideSource.class, name = "INSECTICIDE_SOURCE"),
        @JsonSubTypes.Type(value = PhytochemicalRole.IndustrialFeedstock.class, name = "INDUSTRIAL_FEEDSTOCK"),
        @JsonSubTypes.Type(value = PhytochemicalRole.HumanToxin.class, name = "HUMAN_TOXIN"),
        @JsonSubTypes.Type(value = PhytochemicalRole.LivestockToxin.class, name = "LIVESTOCK_TOXIN")
})
public sealed interface PhytochemicalRole extends ValueObject {

    /**
     * Default no-op {@code invariants()} — stateless permits have no constraints
     * of their own. Permits that acquire state override this to declare their
     * own validation. {@code PhytochemicalConstituent} validates the role set
     * via {@code notEmpty} plus {@code valueObjectCollection}, so a future
     * stateful permit is picked up automatically without call-site updates.
     */
    @Override
    default Consumer<? extends Constraints> invariants() {
        return i -> {
        };
    }

    /**
     * The stable discriminator name for this permit — the same token used by the Jackson
     * {@code @JsonSubTypes} registration. Each permit maps to its case exhaustively;
     * {@link #ofKind(String)} is its inverse. Persistence adapters use the pair to store a role
     * set as a child table of discriminator strings, mirroring {@code chemistry.StructuralType}.
     */
    default String kind() {
        return switch (this) {
            case HerbivoreDeterrent x -> "HERBIVORE_DETERRENT";
            case InsectDeterrent x -> "INSECT_DETERRENT";
            case AntiFungal x -> "ANTI_FUNGAL";
            case AntiMicrobial x -> "ANTI_MICROBIAL";
            case Allelopathic x -> "ALLELOPATHIC";
            case InducedVolatileSignal x -> "INDUCED_VOLATILE_SIGNAL";
            case PollinatorAttractant x -> "POLLINATOR_ATTRACTANT";
            case SeedDisperserAttractant x -> "SEED_DISPERSER_ATTRACTANT";
            case MycorrhizalSignal x -> "MYCORRHIZAL_SIGNAL";
            case UVProtectant x -> "UV_PROTECTANT";
            case StressTolerance x -> "STRESS_TOLERANCE";
            case HeavyMetalChelator x -> "HEAVY_METAL_CHELATOR";
            case Pharmaceutical x -> "PHARMACEUTICAL";
            case Nutraceutical x -> "NUTRACEUTICAL";
            case DyeSource x -> "DYE_SOURCE";
            case FragranceSource x -> "FRAGRANCE_SOURCE";
            case FlavorSource x -> "FLAVOR_SOURCE";
            case FiberSource x -> "FIBER_SOURCE";
            case InsecticideSource x -> "INSECTICIDE_SOURCE";
            case IndustrialFeedstock x -> "INDUSTRIAL_FEEDSTOCK";
            case HumanToxin x -> "HUMAN_TOXIN";
            case LivestockToxin x -> "LIVESTOCK_TOXIN";
        };
    }

    /** Inverse of {@link #kind()} — reconstructs the stateless permit from its discriminator. */
    static PhytochemicalRole ofKind(String kind) {
        return switch (kind) {
            case "HERBIVORE_DETERRENT" -> new HerbivoreDeterrent();
            case "INSECT_DETERRENT" -> new InsectDeterrent();
            case "ANTI_FUNGAL" -> new AntiFungal();
            case "ANTI_MICROBIAL" -> new AntiMicrobial();
            case "ALLELOPATHIC" -> new Allelopathic();
            case "INDUCED_VOLATILE_SIGNAL" -> new InducedVolatileSignal();
            case "POLLINATOR_ATTRACTANT" -> new PollinatorAttractant();
            case "SEED_DISPERSER_ATTRACTANT" -> new SeedDisperserAttractant();
            case "MYCORRHIZAL_SIGNAL" -> new MycorrhizalSignal();
            case "UV_PROTECTANT" -> new UVProtectant();
            case "STRESS_TOLERANCE" -> new StressTolerance();
            case "HEAVY_METAL_CHELATOR" -> new HeavyMetalChelator();
            case "PHARMACEUTICAL" -> new Pharmaceutical();
            case "NUTRACEUTICAL" -> new Nutraceutical();
            case "DYE_SOURCE" -> new DyeSource();
            case "FRAGRANCE_SOURCE" -> new FragranceSource();
            case "FLAVOR_SOURCE" -> new FlavorSource();
            case "FIBER_SOURCE" -> new FiberSource();
            case "INSECTICIDE_SOURCE" -> new InsecticideSource();
            case "INDUSTRIAL_FEEDSTOCK" -> new IndustrialFeedstock();
            case "HUMAN_TOXIN" -> new HumanToxin();
            case "LIVESTOCK_TOXIN" -> new LivestockToxin();
            default -> throw new IllegalArgumentException("Unknown phytochemical role kind: " + kind);
        };
    }

    // ── Defense ──────────────────────────────────────────────────────────

    /**
     * Deters mammalian or vertebrate herbivores — bitter alkaloids, tannins.
     */
    record HerbivoreDeterrent() implements PhytochemicalRole {
    }

    /**
     * Deters insect herbivores — pyrethrins, nicotinoids, glucosinolate breakdown products.
     */
    record InsectDeterrent() implements PhytochemicalRole {
    }

    /**
     * Suppresses fungal pathogens or epiphytes.
     */
    record AntiFungal() implements PhytochemicalRole {
    }

    /**
     * Suppresses bacterial or other microbial pathogens.
     */
    record AntiMicrobial() implements PhytochemicalRole {
    }

    /**
     * Released to soil or air to suppress competing plants — juglone, sorgoleone.
     */
    record Allelopathic() implements PhytochemicalRole {
    }

    // ── Signaling ────────────────────────────────────────────────────────

    /**
     * Volatile organic signal released on damage, recruiting parasitoids or alerting neighbours.
     */
    record InducedVolatileSignal() implements PhytochemicalRole {
    }

    /**
     * Attracts pollinators — floral pigments, fragrance volatiles, nectar secondary metabolites.
     */
    record PollinatorAttractant() implements PhytochemicalRole {
    }

    /**
     * Attracts seed dispersers — fruit pigments, fruit volatiles.
     */
    record SeedDisperserAttractant() implements PhytochemicalRole {
    }

    /**
     * Recruits mycorrhizal partners — strigolactones, root flavonoids.
     */
    record MycorrhizalSignal() implements PhytochemicalRole {
    }

    // ── Environmental interaction ───────────────────────────────────────

    /**
     * Absorbs or quenches UV radiation — flavonoids in epidermal layers.
     */
    record UVProtectant() implements PhytochemicalRole {
    }

    /**
     * Protects against drought, heat, or cold — proline, glycine betaine, sugars.
     */
    record StressTolerance() implements PhytochemicalRole {
    }

    /**
     * Sequesters heavy metals — phytochelatins, metallothioneins.
     */
    record HeavyMetalChelator() implements PhytochemicalRole {
    }

    // ── Medicinal and commercial ────────────────────────────────────────

    /**
     * Documented pharmaceutical activity — taxol, salicin, artemisinin, digitalin.
     */
    record Pharmaceutical() implements PhytochemicalRole {
    }

    /**
     * Health-promoting compound consumed in food — resveratrol, lycopene.
     */
    record Nutraceutical() implements PhytochemicalRole {
    }

    /**
     * Source of textile or food-grade dye — indigo, madder anthraquinones.
     */
    record DyeSource() implements PhytochemicalRole {
    }

    /**
     * Source of perfumery or cosmetic fragrance — rose otto, lavender oil.
     */
    record FragranceSource() implements PhytochemicalRole {
    }

    /**
     * Source of culinary flavour — vanilla vanillin, citrus limonene.
     */
    record FlavorSource() implements PhytochemicalRole {
    }

    /**
     * Source of textile or paper fibre — flax lignin, hemp bast.
     */
    record FiberSource() implements PhytochemicalRole {
    }

    /**
     * Source of commercial insecticide — pyrethrum daisies, neem azadirachtin.
     */
    record InsecticideSource() implements PhytochemicalRole {
    }

    /**
     * Industrial feedstock — gum arabic, rubber latex, taxol biosynthesis precursors.
     */
    record IndustrialFeedstock() implements PhytochemicalRole {
    }

    /**
     * Toxic to humans on ingestion or contact — aristolochic acid, ricin.
     */
    record HumanToxin() implements PhytochemicalRole {
    }

    /**
     * Toxic to livestock — pyrrolizidine alkaloids in Senecio, taxine in Taxus.
     */
    record LivestockToxin() implements PhytochemicalRole {
    }
}
