package com.naturalist.plants.phytochemistry;

import com.naturalist.chemistry.compound.CompoundName;
import com.naturalist.ddd.NamedEntity;
import com.naturalist.fieldnotes.Description;
import com.naturalist.observability.Constraints;
import com.naturalist.plants.PlantName;
import com.naturalist.plants.phytochemistry.role.PhytochemicalRole;
import org.jspecify.annotations.Nullable;

import java.util.Set;
import java.util.function.Consumer;

/**
 * A named phytochemical constituent — one chemical compound, recorded for
 * one plant species, with the role(s) it plays in <em>that</em> plant.
 * <p>
 * {@code PhytochemicalConstituent} is the link between the
 * {@link com.naturalist.plants.Plant} catalog and the chemistry domain's
 * {@code Compound} catalog. The same compound can play very different roles
 * across plants — caffeine deters insects in {@code coffea-arabica} seeds
 * and (in trace amounts) attracts pollinators to {@code citrus-sinensis}
 * nectar — and the {@code PhytochemicalConstituent} record is where that
 * plant-specific story is told. The chemistry-side {@code Compound}
 * record stays plant-agnostic; the structural classification (carbon
 * skeleton, formula, molecular weight) lives there. The
 * <em>ecological</em> classification — what role, what tissue, induced or
 * constitutive — lives here.
 * <p>
 * <b>Identity.</b> The {@link PhytochemicalConstituentName} slug names the
 * link, not the plant or the compound — e.g.
 * {@code "california-pipevine-aristolochic-acid"}. Following the
 * {@code PlantProgram} convention: a single plant carries many constituents
 * and a single compound appears across many plants, so the slug must encode
 * both sides.
 * <p>
 * <b>Cross-domain references.</b> Both {@code plantName} and
 * {@code compoundName} are soft FKs by slug. The record imports
 * {@link CompoundName} from the {@code identifiers} module — there is no
 * compile-time dependency from {@code plants-api} on {@code chemistry-api}.
 * The chemistry catalog is the authoritative store of compound facts; the
 * referenced compound must exist in {@code compounds.json} (a service-layer
 * rule, not a record invariant — cross-aggregate validation).
 * <p>
 * <b>Components.</b>
 * <ul>
 *   <li>{@code description} — Durrell four-level description of the
 *       constituent in this plant: what the compound is, how the plant uses
 *       it, what makes it interesting in this species. Required.</li>
 *   <li>{@code category} — coarse ecological/use bucket
 *       ({@link PhytochemicalCategory#ALKALOID}, {@link
 *       PhytochemicalCategory#GLUCOSINOLATE}, …). The structural class
 *       (indole alkaloid vs tropane alkaloid, monoterpene vs sesquiterpene)
 *       is a separate axis carried on {@code chemistry.CompoundInfo}.</li>
 *   <li>{@code roles} — non-empty set of {@link PhytochemicalRole} entries
 *       declaring what this compound <em>does</em> in this plant. Multiple
 *       roles are common and expected; an empty set is an invariant
 *       violation (a constituent with no role is data without a story).</li>
 *   <li>{@code tissues} — non-empty set of {@link PlantTissue} locations.
 *       Same compound in seed vs leaf vs latex carries different
 *       ecological meaning; recording the tissue makes the catalog
 *       queryable for "what defenses are in the fruit" or "which compounds
 *       sit in the trichomes".</li>
 *   <li>{@code induction} — when the compound is produced
 *       ({@link InductionMode#CONSTITUTIVE} baseline, {@link
 *       InductionMode#INDUCED} on stress, {@link
 *       InductionMode#DEVELOPMENTAL} stage-tied). A compound expressed in
 *       two modes is recorded as two constituents.</li>
 *   <li>{@code notes} — operational guidance: extraction notes, seasonal
 *       concentration variation, observation cadence. Nullable.</li>
 * </ul>
 *
 * <b>Behavioral predicates.</b> Roles are queryable through first-class
 * methods rather than inline {@code instanceof} chains at call sites — the
 * predicates are stable consumer surface and let the role family grow
 * without rippling through every consumer. {@link #playsRole(PhytochemicalRole)}
 * is the generic check; {@link #isDefensive()} and friends are the
 * axis-level rollups.
 */
public record PhytochemicalConstituent(
        PhytochemicalConstituentName name,
        PlantName plantName,
        CompoundName compoundName,
        Description description,
        PhytochemicalCategory category,
        Set<PhytochemicalRole> roles,
        Set<PlantTissue> tissues,
        InductionMode induction,
        @Nullable String notes
) implements NamedEntity<PhytochemicalConstituentName> {

    // ── Role queries ────────────────────────────────────────────────────

    /** Whether the compound plays the given role in this plant. */
    public boolean playsRole(PhytochemicalRole role) {
        return roles.contains(role);
    }

    /**
     * Whether any role on this constituent falls on the defense axis —
     * deterrence of herbivores, insects, fungi, microbes, or competing
     * plants.
     */
    public boolean isDefensive() {
        return roles.stream().anyMatch(r ->
                r instanceof PhytochemicalRole.HerbivoreDeterrent ||
                r instanceof PhytochemicalRole.InsectDeterrent ||
                r instanceof PhytochemicalRole.AntiFungal ||
                r instanceof PhytochemicalRole.AntiMicrobial ||
                r instanceof PhytochemicalRole.Allelopathic);
    }

    /**
     * Whether any role on this constituent falls on the signaling axis —
     * attraction of pollinators, seed dispersers, mycorrhizae, or induced
     * inter-tissue / inter-plant volatile signaling.
     */
    public boolean isSignaling() {
        return roles.stream().anyMatch(r ->
                r instanceof PhytochemicalRole.InducedVolatileSignal ||
                r instanceof PhytochemicalRole.PollinatorAttractant ||
                r instanceof PhytochemicalRole.SeedDisperserAttractant ||
                r instanceof PhytochemicalRole.MycorrhizalSignal);
    }

    /**
     * Whether any role on this constituent falls on the environmental
     * interaction axis — UV protection, osmotic / thermal stress
     * tolerance, heavy-metal tolerance.
     */
    public boolean mediatesEnvironmentalStress() {
        return roles.stream().anyMatch(r ->
                r instanceof PhytochemicalRole.UVProtectant ||
                r instanceof PhytochemicalRole.StressTolerance ||
                r instanceof PhytochemicalRole.HeavyMetalChelator);
    }

    /**
     * Whether this compound has documented medicinal application in this
     * plant — pharmaceutical or nutraceutical use.
     */
    public boolean hasMedicinalApplication() {
        return roles.stream().anyMatch(r ->
                r instanceof PhytochemicalRole.Pharmaceutical ||
                r instanceof PhytochemicalRole.Nutraceutical);
    }

    /**
     * Whether this compound has documented commercial application beyond
     * direct medicinal use — dye, fragrance, flavour, fibre, insecticide,
     * industrial feedstock.
     */
    public boolean hasCommercialApplication() {
        return roles.stream().anyMatch(r ->
                r instanceof PhytochemicalRole.DyeSource ||
                r instanceof PhytochemicalRole.FragranceSource ||
                r instanceof PhytochemicalRole.FlavorSource ||
                r instanceof PhytochemicalRole.FiberSource ||
                r instanceof PhytochemicalRole.InsecticideSource ||
                r instanceof PhytochemicalRole.IndustrialFeedstock);
    }

    /**
     * Whether this compound is recorded as toxic to humans or livestock in
     * this plant. Drives safety constraints in higher application modules.
     */
    public boolean isToxicToMammals() {
        return roles.stream().anyMatch(r ->
                r instanceof PhytochemicalRole.HumanToxin ||
                r instanceof PhytochemicalRole.LivestockToxin);
    }

    // ── Tissue / induction queries ──────────────────────────────────────

    /** Whether this constituent is recorded as present in the given tissue. */
    public boolean isPresentIn(PlantTissue tissue) {
        return tissues.contains(tissue);
    }

    /** Whether the compound is produced only on stress / damage / pathogen exposure. */
    public boolean isInduced() {
        return induction == InductionMode.INDUCED;
    }

    /** Whether the compound is tied to a developmental stage (e.g. ripening). */
    public boolean isDevelopmental() {
        return induction == InductionMode.DEVELOPMENTAL;
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityName(name, "name")
                .notNull(this, PhytochemicalConstituent::plantName, "plantName")
                .notNull(this, PhytochemicalConstituent::compoundName, "compoundName")
                .valueObject(this, PhytochemicalConstituent::description, "description")
                .notNull(this, PhytochemicalConstituent::category, "category")
                .notEmpty(this, PhytochemicalConstituent::roles, "roles")
                .notEmpty(this, PhytochemicalConstituent::tissues, "tissues")
                .notNull(this, PhytochemicalConstituent::induction, "induction");
    }
}
