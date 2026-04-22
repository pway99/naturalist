package com.naturalist.insects;

import com.naturalist.ddd.AggregateRoot;
import com.naturalist.ddd.NamedEntity;
import com.naturalist.ddd.ValueObject;
import com.naturalist.fieldnotes.Description;
import com.naturalist.habitat.HabitatProfile;
import com.naturalist.observability.Constraints;
import com.naturalist.taxonomy.TaxonomicClassification;
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
 * All nullable fields — {@link #identificationFeatures}, {@link #lifeStages},
 * {@link #chemicalDefense}, {@link #voltinism}, {@link #habitatProfile},
 * {@link #habitatRequirements}, {@link #gardenConnections}, {@link #beneficialProfile},
 * and {@link #ecologicalSignificance} — are populated incrementally as the catalog
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
 * The species's value-object graph — {@link IdentificationFeatures}, {@link LifeStages}
 * (with nested {@link LifeStages.EggStage}, {@link LifeStages.LarvaStage},
 * {@link LifeStages.PupaStage}, {@link LifeStages.AdultStage}), {@link ChemicalDefense},
 * {@link Voltinism}, {@link HabitatRequirements}, {@link GardenConnections},
 * {@link BeneficialProfile}, {@link EcologicalSignificance} — is nested here. Each
 * value object is exclusively owned by {@code InsectSpecies}; nesting expresses that
 * ownership structurally and collapses the consumer's import surface to this single type.
 * {@link LifeStageKind} is the shared vocabulary used by value objects that need to
 * name one or more stages of the life cycle.
 */
@AggregateRoot
public record InsectSpecies(
        InsectSpeciesName name,
        TaxonomicClassification taxonomy,
        Description description,
        Set<FunctionalGuild> guilds,
        boolean beneficial,
        @Nullable String sightingNotes,
        @Nullable IdentificationFeatures identificationFeatures,
        @Nullable LifeStages lifeStages,
        @Nullable ChemicalDefense chemicalDefense,
        @Nullable Voltinism voltinism,
        @Nullable HabitatProfile habitatProfile,
        @Nullable HabitatRequirements habitatRequirements,
        @Nullable GardenConnections gardenConnections,
        @Nullable BeneficialProfile beneficialProfile,
        @Nullable EcologicalSignificance ecologicalSignificance
) implements NamedEntity<InsectSpeciesName> {

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
        // Nullable value-object children are registered via valueObjectOrNull so the
        // graph walker descends into each child's own invariants when non-null.
        // Meaningfulness of any present child is that child's responsibility — a
        // vacuous-but-present value object must be rejected by its own invariants()
        // rather than tolerated here.
        return i -> i
                .entityName(name, "name")
                .valueObject(taxonomy, "taxonomy")
                .valueObject(description, "description")
                .notNull(this, InsectSpecies::guilds, "guilds")
                .valueObjectOrNull(this, InsectSpecies::identificationFeatures, "identificationFeatures")
                .valueObjectOrNull(this, InsectSpecies::lifeStages, "lifeStages")
                .valueObjectOrNull(this, InsectSpecies::chemicalDefense, "chemicalDefense")
                .valueObjectOrNull(this, InsectSpecies::voltinism, "voltinism")
                .valueObjectOrNull(this, InsectSpecies::habitatProfile, "habitatProfile")
                .valueObjectOrNull(this, InsectSpecies::habitatRequirements, "habitatRequirements")
                .valueObjectOrNull(this, InsectSpecies::gardenConnections, "gardenConnections")
                .valueObjectOrNull(this, InsectSpecies::beneficialProfile, "beneficialProfile")
                .valueObjectOrNull(this, InsectSpecies::ecologicalSignificance, "ecologicalSignificance");
    }

    /**
     * The stages of an insect's life cycle, as a discrete vocabulary. Used by value
     * objects that need to name one or more stages — most notably
     * {@link ChemicalDefense#protectedStages()}, which records exactly which stages
     * inherit a species' chemical defences.
     * <p>
     * {@link #PUPA} is semantically absent on hemimetabolous species (Blattodea,
     * Orthoptera, Hemiptera), which produce nymphs rather than pupae. Consumers
     * interpreting {@link LifeStageKind} sets on those species should treat the pupal
     * slot as "does not exist" rather than "unknown".
     */
    public enum LifeStageKind {
        EGG,
        LARVA,
        PUPA,
        ADULT
    }

    /**
     * The observable physical characteristics by which a species is identified in the field.
     * <p>
     * Features are ordered from most conspicuous to most diagnostic — the same sequence
     * a naturalist would follow when working through a field identification. Each entry
     * is a discrete, observable trait: colour, proportion, posture, structural feature.
     * <p>
     * These are morphological and postural facts, not behavioural ones. Behaviour belongs
     * in {@link LifeStages} or {@link InsectSpecies#sightingNotes()}.
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
     *       with no protected stage should carry {@code null} at the parent field.</li>
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
            /** One generation per year. */
            UNIVOLTINE,
            /** Two generations per year. */
            BIVOLTINE,
            /** Three or more generations per year. */
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
     * The documented life cycle stages of an insect species.
     * <p>
     * {@code adult} is always present — it is the stage at which field identification
     * occurs and the stage that defines the species' ecological guild at Oak Vista.
     * <p>
     * {@code egg}, {@code larva}, and {@code pupa} are nullable for two distinct reasons,
     * applied per stage:
     * <ul>
     *   <li>{@code egg} — egg-stage data may simply not be documented for the species
     *       at catalog level, even if the species is holometabolous.</li>
     *   <li>{@code larva} — hemimetabolous orders (Blattodea, Orthoptera, Hemiptera)
     *       produce nymphs rather than morphologically distinct larvae; their immature
     *       stages are not modelled here. A null {@code larva} on a holometabolous
     *       species means the larval stage has not yet been catalogued, not that it
     *       does not exist.</li>
     *   <li>{@code pupa} — hemimetabolous orders do not pupate; on those species
     *       {@code pupa} is semantically absent. On holometabolous species a null
     *       {@code pupa} means the stage has not yet been catalogued. Modelled because
     *       pupal biology carries species-level ecological facts — chrysalis crypsis,
     *       diapause regulation, and voltinism variability — that no other stage captures.
     *       The <i>Battus philenor</i> chrysalis (brown/green polymorphism, non-photoperiodic
     *       diapause regulated by larval-food water content) is the motivating case.</li>
     * </ul>
     */
    public record LifeStages(
            @Nullable EggStage egg,
            @Nullable LarvaStage larva,
            @Nullable PupaStage pupa,
            AdultStage adult
    ) implements ValueObject {

        @Override
        public Consumer<? extends Constraints> invariants() {
            return i -> i.notNull(this, LifeStages::adult, "adult");
        }

        /**
         * The egg stage of an insect's life cycle.
         * <p>
         * {@code description} covers morphology and oviposition site — the minimum field
         * record. All remaining fields are nullable: not every taxon exhibits a documented
         * colour progression, a distinctive laying pattern, or an adaptive significance
         * worth recording at catalog level.
         * <p>
         * Notable example: Chrysoperla (green lacewing) eggs are laid on individual silk
         * stalks 10–15 mm tall — a structural adaptation that prevents newly hatched
         * predatory larvae from consuming unhatched siblings before they disperse.
         * The {@code adaptiveSignificance} field captures exactly this class of domain knowledge.
         */
        public record EggStage(
                String description,
                @Nullable String colorProgression,
                @Nullable String layingPattern,
                @Nullable String adaptiveSignificance
        ) implements ValueObject {

            @Override
            public Consumer<? extends Constraints> invariants() {
                return i -> i.notNull(this, EggStage::description, "description");
            }
        }

        /**
         * The larval stage of a holometabolous insect's life cycle.
         * <p>
         * Only present on species undergoing complete metamorphosis (Holometabola):
         * Neuroptera, Coleoptera, Diptera, Hymenoptera, Lepidoptera. Hemimetabolous
         * orders (Orthoptera, Hemiptera, Blattodea) produce nymphs, not larvae — their
         * immature stages are not modelled here.
         * <p>
         * {@code preyTargets} and {@code hostPlants} carry the two mutually characteristic
         * modes of larval feeding, and are kept as separate lists rather than collapsed
         * into a single "food sources" field because they describe ecologically distinct
         * relationships:
         * <ul>
         *   <li>{@code preyTargets} — prey species consumed by predatory or parasitoid
         *       larvae. Empty for phytophagous larvae. {@code preyConsumption} gives a
         *       quantitative rate where documented — for example, <i>Chrysoperla</i>
         *       larvae consume 200+ aphids per week.</li>
         *   <li>{@code hostPlants} — plant species whose tissue sustains phytophagous
         *       larvae (caterpillars, leaf beetle grubs, gall-forming larvae). Empty
         *       for predatory and parasitoid larvae. The distinction between monophagy
         *       (one host, e.g. <i>Battus philenor</i> on <i>Aristolochia californica</i>),
         *       oligophagy (a few related hosts), and polyphagy (many unrelated hosts)
         *       is expressed by the list's size and composition. Entries are currently
         *       descriptive strings pending the plants catalog, after which they become
         *       typed cross-domain {@code PlantName} references.</li>
         * </ul>
         * <p>
         * {@code remarkableBehavior} captures field-notable behaviour not covered by
         * description or food data (e.g. the debris-carrying camouflage of lacewing
         * larvae, or the induced-defence avoidance behaviour of <i>Battus philenor</i>
         * caterpillars, which rotate between leaves as each feeding site becomes
         * more toxic).
         */
        public record LarvaStage(
                @Nullable String commonName,
                String description,
                @Nullable String preyConsumption,
                List<String> preyTargets,
                List<String> hostPlants,
                @Nullable String remarkableBehavior
        ) implements ValueObject {

            @Override
            public Consumer<? extends Constraints> invariants() {
                return i -> i
                        .notNull(this, LarvaStage::description, "description")
                        .notNull(this, LarvaStage::preyTargets, "preyTargets")
                        .notNull(this, LarvaStage::hostPlants, "hostPlants");
            }
        }

        /**
         * The pupal stage of a holometabolous insect's life cycle — the immobile
         * transformative stage between larva and adult.
         * <p>
         * Only present on species undergoing complete metamorphosis (Holometabola):
         * Neuroptera, Coleoptera, Diptera, Hymenoptera, Lepidoptera. Hemimetabolous
         * orders do not pupate.
         * <p>
         * {@code description} covers form, substrate, and attachment — the minimum field
         * record (e.g. "chrysalis suspended by cremaster and silk girdle from host
         * vegetation" for Lepidoptera). {@code appearance} captures colour, pattern,
         * and any polymorphism — <i>Battus philenor</i> pupae are dimorphic, brown or
         * green, with a golden filigree.
         * <p>
         * {@code diapauseRegulation} records what governs the pupa's entry into and
         * exit from dormancy: photoperiod (the default across temperate Lepidoptera),
         * temperature, humidity, or — in the <i>Battus philenor</i> case — the water
         * content of the larval food, uniquely decoupling diapause from day length
         * and producing mixed direct-developer / diapauser cohorts within a single clutch.
         * <p>
         * {@code adaptiveSignificance} mirrors the field on {@link EggStage}: species-
         * level pupal adaptations worth recording at catalog level, such as split
         * diapause strategies that make site-level voltinism indeterminate.
         */
        public record PupaStage(
                String description,
                @Nullable String appearance,
                @Nullable String diapauseRegulation,
                @Nullable String adaptiveSignificance
        ) implements ValueObject {

            @Override
            public Consumer<? extends Constraints> invariants() {
                return i -> i.notNull(this, PupaStage::description, "description");
            }
        }

        /**
         * The adult stage of an insect's life cycle — the reproductive and (in many
         * species) the dispersal stage.
         * <p>
         * {@code feeding} describes the adult diet: nectarivore, predator, non-feeding
         * (many adult Ephemeroptera and some Lepidoptera are essentially non-feeding).
         * {@code role} is the adult's primary ecological function at Oak Vista —
         * pollinator, dispersal agent, reproductive stage only.
         * <p>
         * {@code attraction} captures stimuli that draw adults to specific microhabitats
         * or structures — artificial lighting (lacewings, crane flies), floral volatiles
         * (bees), or pheromone plumes. Relevant to siting habitat plantings and
         * managing light pollution effects on beneficial populations.
         * <p>
         * {@code supportedBy} lists the plant species or resource types that sustain
         * adult populations at Oak Vista. These will become typed cross-domain
         * {@code PlantName} references once the plants catalog is established.
         * <p>
         * {@code lifespan} records the typical duration of the adult stage — "a few
         * days" for mayflies, "a month or so" for <i>Battus philenor</i>, "several
         * months" for overwintering Vanessa. Nullable where undocumented; it is not
         * a strict invariant of the species but a field-relevant ecological fact that
         * shapes expectations for repeat observation of a marked individual.
         * <p>
         * {@code flightPeriod} is the seasonal phenology of adult activity at Oak Vista:
         * onset, peak(s), and end-of-season tail. Captured as narrative rather than a
         * structured month range because many species have multi-peak flights with
         * species-specific shape — <i>Battus philenor</i>, for example, flies February
         * through October with two dominant flights before July, a mid-summer lull,
         * and an August "blip" driven by diapause breakage. Distinct from
         * {@link EcologicalSignificance#regionalContext()}, which carries landscape-
         * scale dynamics rather than stage-specific phenology.
         */
        public record AdultStage(
                String feeding,
                String role,
                @Nullable String attraction,
                List<String> supportedBy,
                @Nullable String lifespan,
                @Nullable String flightPeriod
        ) implements ValueObject {

            @Override
            public Consumer<? extends Constraints> invariants() {
                return i -> i
                        .notBlank(feeding, "feeding")
                        .notBlank(role, "role")
                        .notNull(supportedBy, "supportedBy");
            }
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
            return i -> {};
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
            return i -> {};
        }
    }
}
