package com.naturalist.insects;

import com.naturalist.ddd.AggregateRoot;
import com.naturalist.ddd.NamedEntity;
import com.naturalist.ddd.ValueObject;
import com.naturalist.fieldnotes.CommonName;
import com.naturalist.fieldnotes.Description;
import com.naturalist.habitat.HabitatProfile;
import com.naturalist.insects.lifestage.AdultStage;
import com.naturalist.insects.lifestage.EggStage;
import com.naturalist.insects.lifestage.LarvaStage;
import com.naturalist.insects.lifestage.PupaStage;
import com.naturalist.observability.Constraints;
import com.naturalist.taxonomy.LinnaeanSpecies;
import com.naturalist.taxonomy.TaxonomicClassification;
import com.naturalist.taxonomy.TaxonomicGenus;
import com.naturalist.taxonomy.TaxonomicSpecies;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * A catalogued insect species (class Insecta) observed or documented at Oak Vista.
 * <p>
 * Each species carries a {@link TaxonomicClassification} from order to species and
 * a four-level {@link Description} embodying Durrell's principle — the same ecological
 * truth rendered at preschool, elementary, secondary, and university resolution.
 * <p>
 * The {@code guilds} set captures all functional ecological roles the species fills
 * at Oak Vista. A hoverfly, for example, holds both {@link FunctionalGuild#PREDATOR}
 * (larva) and {@link FunctionalGuild#POLLINATOR} (adult). Guild assignment governs
 * which application modules incorporate this species in their analyses
 * (ForagingCalendar, PestManagement).
 * <p>
 * {@code beneficial} is a pragmatic garden-management flag — true for species whose
 * net effect on tomato yield and ecosystem health is positive. All parasitoids,
 * predators, pollinators, and decomposers in the Oak Vista catalog are beneficial.
 * The field is retained for completeness: future catalog entries may include pest
 * species for educational or monitoring purposes.
 * <p>
 * {@code sightingNotes} captures field observations specific to Oak Vista —
 * dates of first observation, confirmed breeding, microhabitat, or management
 * implications (e.g. "eggs confirmed on Pipevine April 2026 — never treat Pipevine").
 * <p>
 * {@code habitatProfile} carries the structured habitat classification from
 * {@code kernels/habitat}: the zones the species occupies, its moisture and light
 * regime preferences, and the vertical layers it operates in. This is the cross-domain
 * vocabulary — the same type plants, arachnids, and other organisms will carry when
 * the catalog grows. {@code habitatRequirements} is the complementary insect-specific
 * narrative: nectar sources, shelter substrate, prey density, and phototactic behaviour
 * expressed as ecological field notes rather than structured classification.
 * <p>
 * The four life-cycle stage fields — {@link #egg}, {@link #larva}, {@link #pupa},
 * and {@link #adult} — hold the species's stage entities from
 * {@link com.naturalist.insects.lifestage}. Each stage is a {@link NamedEntity} with
 * its own composite-slug identity ({@code {species-slug}-{stage-kind-slug}}); the
 * species composes them directly rather than referencing them by name because each
 * stage is biologically inseparable from its species.
 * <p>
 * All four stage fields are nullable for two distinct reasons, applied per stage:
 * <ul>
 *   <li>{@link #egg} — egg-stage data may simply not be documented for the species
 *       at catalog level, even if the species is holometabolous.</li>
 *   <li>{@link #larva} — hemimetabolous orders (Blattodea, Orthoptera, Hemiptera)
 *       produce nymphs rather than morphologically distinct larvae; their immature
 *       stages are not modelled here. A null {@code larva} on a holometabolous
 *       species means the larval stage has not yet been catalogued, not that it
 *       does not exist.</li>
 *   <li>{@link #pupa} — hemimetabolous orders do not pupate; on those species
 *       {@code pupa} is semantically absent. On holometabolous species a null
 *       {@code pupa} means the stage has not yet been catalogued.</li>
 *   <li>{@link #adult} — catalogued incrementally like the others. Cross-stage
 *       invariants (e.g. chemistry-story coherence across larva / pupa / adult)
 *       are enforced on this aggregate root rather than on any single stage.</li>
 * </ul>
 * <p>
 * All other nullable fields — {@link #identificationFeatures}, {@link #chemicalDefense},
 * {@link #voltinism}, {@link #habitatProfile}, {@link #habitatRequirements},
 * {@link #gardenConnections}, {@link #beneficialProfile}, and
 * {@link #ecologicalSignificance} — are populated incrementally as the catalog
 * matures. {@code beneficialProfile} is additionally constrained by intent: it should
 * only be populated when {@code beneficial} is {@code true}. {@code chemicalDefense} is
 * populated only for species that sequester, synthesise, or otherwise deploy defensive
 * chemistry — <i>Battus philenor</i> (aristolochic acids from <i>Aristolochia
 * californica</i>), monarchs, pierids, and other aposematic taxa. Non-defended species
 * carry {@code null}. {@code voltinism} is populated when the species' number of
 * generations per year at Oak Vista is known — including the deliberately
 * {@link Voltinism.VoltinismPattern#INDETERMINATE} case for species whose voltinism
 * varies within a site due to split diapause strategies.
 * <p>
 * The species's value-object graph — {@link IdentificationFeatures},
 * {@link ChemicalDefense}, {@link Voltinism}, {@link HabitatRequirements},
 * {@link GardenConnections}, {@link BeneficialProfile}, {@link EcologicalSignificance}
 * — is nested here. Each value object is exclusively owned by {@code InsectSpecies};
 * nesting expresses that ownership structurally and collapses the consumer's import
 * surface to this single type. {@link LifeStageKind} — now the shared vocabulary in
 * {@link com.naturalist.insects.lifestage} — is used by {@link ChemicalDefense} to
 * name one or more stages of the life cycle.
 */
@AggregateRoot
public record InsectSpecies(
        InsectSpeciesName name,
        TaxonomicClassification taxonomy,
        Description description,
        Set<CommonName> commonNames,
        Set<FunctionalGuild> guilds,
        boolean beneficial,
        @Nullable String sightingNotes,
        @Nullable IdentificationFeatures identificationFeatures,
        @Nullable EggStage egg,
        @Nullable LarvaStage larva,
        @Nullable PupaStage pupa,
        @Nullable AdultStage adult,
        @Nullable ChemicalDefense chemicalDefense,
        @Nullable Voltinism voltinism,
        @Nullable HabitatProfile habitatProfile,
        @Nullable HabitatRequirements habitatRequirements,
        @Nullable GardenConnections gardenConnections,
        @Nullable BeneficialProfile beneficialProfile,
        @Nullable EcologicalSignificance ecologicalSignificance
) implements NamedEntity<InsectSpeciesName>, LinnaeanSpecies {

    @Override
    public TaxonomicGenus genus() {
        return taxonomy.genus();
    }

    @Override
    public TaxonomicSpecies species() {
        return taxonomy.species();
    }

    /**
     * Whether this species performs pollination services at Oak Vista.
     * Convenience query over {@code guilds.contains(FunctionalGuild.POLLINATOR)}.
     */
    public boolean isPollinator() {
        return guilds.contains(FunctionalGuild.POLLINATOR);
    }

    /**
     * Whether this species is a keystone species at Oak Vista.
     * Keystone species require heightened management caution — never apply
     * pesticides to host plants of keystone species.
     */
    public boolean isKeystone() {
        return guilds.contains(FunctionalGuild.KEYSTONE);
    }

    /**
     * Whether this species provides direct pest suppression via predation or parasitism.
     */
    public boolean isBiocontrolAgent() {
        return guilds.contains(FunctionalGuild.PARASITOID)
                || guilds.contains(FunctionalGuild.PREDATOR)
                || guilds.contains(FunctionalGuild.APEX_PREDATOR);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        // Stage children are NamedEntity subtypes; Constraints has no nullable
        // descent helper for them, so each non-null stage is descended conditionally.
        // Nullable value-object children continue to use valueObjectOrNull.
        Consumer<Constraints> body = i -> {
            i.entityName(name, "name")
                    .valueObject(taxonomy, "taxonomy")
                    .valueObject(description, "description")
                    .notNull(commonNames, "commonNames")
                    .notNull(this, InsectSpecies::guilds, "guilds")
                    .valueObjectOrNull(this, InsectSpecies::identificationFeatures, "identificationFeatures")
                    .valueObjectOrNull(this, InsectSpecies::chemicalDefense, "chemicalDefense")
                    .valueObjectOrNull(this, InsectSpecies::voltinism, "voltinism")
                    .valueObjectOrNull(this, InsectSpecies::habitatProfile, "habitatProfile")
                    .valueObjectOrNull(this, InsectSpecies::habitatRequirements, "habitatRequirements")
                    .valueObjectOrNull(this, InsectSpecies::gardenConnections, "gardenConnections")
                    .valueObjectOrNull(this, InsectSpecies::beneficialProfile, "beneficialProfile")
                    .valueObjectOrNull(this, InsectSpecies::ecologicalSignificance, "ecologicalSignificance");
            if (egg != null) i.namedEntity(this, InsectSpecies::egg, "egg");
            if (larva != null) i.namedEntity(this, InsectSpecies::larva, "larva");
            if (pupa != null) i.namedEntity(this, InsectSpecies::pupa, "pupa");
            if (adult != null) i.namedEntity(this, InsectSpecies::adult, "adult");
        };
        return body;
    }

    /**
     * The observable physical characteristics by which a species is identified in the field.
     * <p>
     * Features are ordered from most conspicuous to most diagnostic — the same sequence
     * a naturalist would follow when working through a field identification. Each entry
     * is a discrete, observable trait: colour, proportion, posture, structural feature.
     * <p>
     * These are morphological and postural facts, not behavioural ones. Behaviour belongs
     * in the stage entities ({@link EggStage}, {@link LarvaStage}, {@link PupaStage},
     * {@link AdultStage}) or {@link InsectSpecies#sightingNotes()}.
     */
    public record IdentificationFeatures(
            List<String> features
    ) implements ValueObject {

        @Override
        public Consumer<? extends Constraints> invariants() {
            return i -> i.notNull(this, IdentificationFeatures::features, "features");
        }
    }

    /**
     * Chemical defence as a species-level ecological trait — the mechanism by which
     * a species deters or incapacitates vertebrate or invertebrate predators through
     * sequestered, synthesised, or symbiont-derived chemistry, and the stages of the
     * life cycle over which that protection is expressed.
     * <p>
     * Populated only for defended species. Non-defended species carry a {@code null}
     * {@link InsectSpecies#chemicalDefense()}; an empty or default {@code ChemicalDefense}
     * is not a legal alternative encoding.
     * <p>
     * <b>Fields:</b>
     * <ul>
     *   <li>{@code mechanism} — the defence strategy in ecological vocabulary:
     *       sequestration from host plant, de novo synthesis, symbiont production,
     *       reflex bleeding, defensive secretion, stinging apparatus. Required.</li>
     *   <li>{@code sourceCompounds} — the chemical class or named compounds involved
     *       (e.g. aristolochic acids, cardenolides, cyanogenic glycosides, iridoid
     *       glycosides, alkaloids). Nullable when the chemistry is undocumented.</li>
     *   <li>{@code aposematicSignal} — the warning signal coupled to the defence:
     *       bright coloration, patterning, stridulation, odour release. Nullable
     *       for cryptically defended species (e.g. some stinging Hymenoptera
     *       whose adults are not strongly aposematic).</li>
     *   <li>{@code protectedStages} — the subset of {@link LifeStageKind} values
     *       over which the defence is active. <i>Battus philenor</i> protects all
     *       four stages: aristolochic acids are sequestered by the larva, retained
     *       through pupation, carried by the adult, and passed maternally to the
     *       conspicuous brick-red egg clusters. An empty set is invalid — a species
     *       with no protected stage should carry {@code null} at the parent field.
     *       This field is redundant with each stage's own
     *       {@link com.naturalist.insects.lifestage.StageChemistryRole} and is a
     *       candidate for removal once every stage carrying a chemistry role is
     *       populated in the catalog.</li>
     * </ul>
     */
    public record ChemicalDefense(
            String mechanism,
            @Nullable String sourceCompounds,
            @Nullable String aposematicSignal,
            Set<LifeStageKind> protectedStages
    ) implements ValueObject {

        @Override
        public Consumer<? extends Constraints> invariants() {
            return i -> i
                    .notBlank(mechanism, "mechanism")
                    .notNull(this, ChemicalDefense::protectedStages, "protectedStages");
        }
    }

    /**
     * The number of generations a species completes per year at Oak Vista.
     * <p>
     * A structured value rather than an integer because voltinism is not always a
     * determinate count. <i>Battus philenor</i> produces both direct-developing and
     * diapausing pupae from a single clutch, with some diapausers eclosing later
     * in the same year rather than overwintering — the resulting site-level voltinism
     * is genuinely indeterminate. {@link VoltinismPattern#INDETERMINATE} preserves
     * that fact rather than forcing it into a false integer.
     * <p>
     * {@code notes} is the narrative field where the reason for the pattern belongs —
     * split diapause strategy, climate-dependent generation counts, host-plant
     * availability driving additional broods, and so on. Strongly recommended when
     * {@code pattern} is {@link VoltinismPattern#INDETERMINATE}, where the label alone
     * is uninformative.
     */
    public record Voltinism(
            VoltinismPattern pattern,
            @Nullable String notes
    ) implements ValueObject {

        @Override
        public Consumer<? extends Constraints> invariants() {
            return i -> i.notNull(this, Voltinism::pattern, "pattern");
        }

        /**
         * The qualitative pattern of annual generation count.
         */
        public enum VoltinismPattern {
            /**
             * One generation per year.
             */
            UNIVOLTINE,
            /**
             * Two generations per year.
             */
            BIVOLTINE,
            /**
             * Three or more generations per year.
             */
            MULTIVOLTINE,
            /**
             * Generation count varies within a site and cannot be reduced to a
             * single integer — typically due to split diapause strategies or
             * climate-dependent extra broods. See {@link Voltinism#notes()}.
             */
            INDETERMINATE
        }
    }

    /**
     * The habitat conditions an insect species requires to persist at Oak Vista.
     * <p>
     * These are the insect's requirements — what it needs from the environment —
     * not a description of the habitat itself. The habitat as a place belongs to
     * another domain; these fields describe the functional relationship between
     * the insect and its environment from the insect's perspective.
     * <p>
     * All fields are nullable: requirements are documented as they become known
     * and not every species will have all five axes characterised at catalog level.
     * <p>
     * <b>Fields:</b>
     * <ul>
     *   <li>{@code nectarSources} — floral resources required by adults; particularly
     *       relevant for parasitoids and pollinators whose adult longevity and
     *       fecundity depend on accessible nectar and pollen.</li>
     *   <li>{@code shelter} — daytime refuge, nesting substrate, or overwintering
     *       site. Examples: coarse mulch (ground beetles, field roaches), dead wood
     *       (carpenter bees), bare soil patches (Andrena, Halictus).</li>
     *   <li>{@code preyAvailability} — prey or host density threshold required to
     *       sustain a breeding population; relevant for predators and parasitoids.
     *       Guides decisions about maintaining aphid refugia on border plants.</li>
     *   <li>{@code lighting} — response to artificial light at night. Lacewings and
     *       crane flies are positively phototactic; this affects where adults
     *       aggregate and what microhabitats support dispersal.</li>
     *   <li>{@code elevationRange} — the elevational band over which the species
     *       persists as a breeding population, and how far it strays beyond that
     *       band. <i>Battus philenor</i>, for example, breeds in foothill canyons
     *       and on the Central Valley floor and strays to mid-elevation along
     *       river bottoms but is absent from the high country. Captured as
     *       narrative rather than a structured range because the breeding band
     *       and the straying band are ecologically distinct and rarely reducible
     *       to a single min/max pair.</li>
     * </ul>
     */
    public record HabitatRequirements(
            @Nullable String nectarSources,
            @Nullable String shelter,
            @Nullable String preyAvailability,
            @Nullable String lighting,
            @Nullable String elevationRange
    ) implements ValueObject {

        @Override
        public Consumer<? extends Constraints> invariants() {
            return i -> {
            };
        }
    }

    /**
     * The ecological relationships connecting an insect species to the broader Oak Vista
     * garden community.
     * <p>
     * {@code supportingPlants} lists the plant species that provide nectar, pollen,
     * nesting substrate, or prey resources that sustain this insect. These are currently
     * modelled as descriptive strings. Once the plants catalog is established they will
     * become typed cross-domain references — the string values here should be treated as
     * future {@code PlantName} slugs pending that alignment.
     * <p>
     * {@code relationshipToOtherBeneficials} describes how this species interacts with
     * other beneficial insects at Oak Vista — competitive overlap, complementary
     * microhabitat partitioning, or prey sharing. Predicts management tensions or
     * synergies (e.g. lacewing larvae and ladybug larvae share aphid colonies but occupy
     * different microhabitats).
     * <p>
     * {@code naturalEnemies} documents what preys on this species at Oak Vista —
     * relevant to understanding why beneficial populations fluctuate and what habitat
     * structures support population persistence despite predation pressure.
     */
    public record GardenConnections(
            List<String> supportingPlants,
            @Nullable String relationshipToOtherBeneficials,
            @Nullable String naturalEnemies
    ) implements ValueObject {

        @Override
        public Consumer<? extends Constraints> invariants() {
            return i -> i.notNull(this, GardenConnections::supportingPlants, "supportingPlants");
        }
    }

    /**
     * Structured pest-management and ecological significance data for species whose
     * {@link InsectSpecies#beneficial()} flag is {@code true}.
     * <p>
     * This is a nullable field on {@link InsectSpecies} — it is only populated for
     * documented beneficial species. Future catalog entries for pest species or
     * ecologically neutral species will carry {@code null} here.
     * <p>
     * {@code significance} is the primary field — a concise statement of why this
     * species matters at Oak Vista, oriented toward the naturalist rather than the
     * pest manager.
     * <p>
     * {@code pestManagementValue} describes the IPM role: whether the species is
     * commercially available for release, whether it arrived naturally, and the
     * practical pest suppression contribution at the garden scale.
     * <p>
     * {@code ipmNotes} captures management guidance specific to this species —
     * conditions for purchase and release, habitat setup required to support
     * establishment, or reasons why commercial release is or is not appropriate
     * (e.g. purchased overwintered ladybugs disperse immediately and provide no
     * pest control value; captive on-site rearing is the correct strategy).
     */
    public record BeneficialProfile(
            String significance,
            @Nullable String pestManagementValue,
            @Nullable String ipmNotes
    ) implements ValueObject {

        @Override
        public Consumer<? extends Constraints> invariants() {
            return i -> i.notNull(this, BeneficialProfile::significance, "significance");
        }
    }

    /**
     * The ecological significance of an insect species at the Oak Vista garden and
     * broader Chico Central Valley scale.
     * <p>
     * All fields are nullable — significance is documented as it is understood, and
     * not every species will have all three axes characterised at catalog level.
     * <p>
     * <b>Fields:</b>
     * <ul>
     *   <li>{@code indicatorValue} — what the presence of this species signals about
     *       garden ecosystem health. Species that require specific habitat conditions
     *       to persist are reliable indicators that those conditions are met.
     *       Example: natural lacewing colonisation indicates a diverse flowering plant
     *       community providing adult nectar resources without augmentation.</li>
     *   <li>{@code foodWebPosition} — where this species sits in the Oak Vista food web.
     *       Primary consumer, secondary predator, apex invertebrate predator, basal prey.
     *       Describes the trophic cascade effects of population change.</li>
     *   <li>{@code regionalContext} — landscape-scale or biogeographic context specific
     *       to Chico and the Central Valley: seasonal phenology, migration patterns,
     *       voltinism under the warm Mediterranean climate, regional population dynamics.</li>
     * </ul>
     */
    public record EcologicalSignificance(
            @Nullable String indicatorValue,
            @Nullable String foodWebPosition,
            @Nullable String regionalContext
    ) implements ValueObject {

        @Override
        public Consumer<? extends Constraints> invariants() {
            return i -> {
            };
        }
    }
}
