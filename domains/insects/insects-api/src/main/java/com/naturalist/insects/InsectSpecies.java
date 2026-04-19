package com.naturalist.insects;

import com.naturalist.fieldnotes.Description;
import com.naturalist.habitat.HabitatProfile;
import com.naturalist.taxonomy.TaxonomicClassification;
import com.naturalist.ddd.AggregateRoot;
import com.naturalist.ddd.CatalogEntity;
import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.Constraints;
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
 * {@link #habitatProfile}, {@link #habitatRequirements}, {@link #gardenConnections},
 * {@link #beneficialProfile}, and {@link #ecologicalSignificance} — are populated
 * incrementally as the catalog matures. {@code beneficialProfile} is additionally
 * constrained by intent: it should only be populated when {@code beneficial} is {@code true}.
 * <p>
 * The species's value-object graph — {@link IdentificationFeatures}, {@link LifeStages}
 * (with nested {@link LifeStages.EggStage}, {@link LifeStages.LarvaStage},
 * {@link LifeStages.AdultStage}), {@link HabitatRequirements}, {@link GardenConnections},
 * {@link BeneficialProfile}, {@link EcologicalSignificance} — is nested here. Each
 * value object is exclusively owned by {@code InsectSpecies}; nesting expresses that
 * ownership structurally and collapses the consumer's import surface to this single type.
 */
@AggregateRoot
public record InsectSpecies(
        InsectSpeciesId id,
        InsectSpeciesName name,
        TaxonomicClassification taxonomy,
        Description description,
        Set<FunctionalGuild> guilds,
        boolean beneficial,
        @Nullable String sightingNotes,
        @Nullable IdentificationFeatures identificationFeatures,
        @Nullable LifeStages lifeStages,
        @Nullable HabitatProfile habitatProfile,
        @Nullable HabitatRequirements habitatRequirements,
        @Nullable GardenConnections gardenConnections,
        @Nullable BeneficialProfile beneficialProfile,
        @Nullable EcologicalSignificance ecologicalSignificance
) implements CatalogEntity<InsectSpeciesId, InsectSpeciesName> {

    @Override
    public InsectSpecies withId(InsectSpeciesId id) {
        return new InsectSpecies(id, name, taxonomy, description, guilds, beneficial, sightingNotes,
                identificationFeatures, lifeStages, habitatProfile, habitatRequirements,
                gardenConnections, beneficialProfile, ecologicalSignificance);
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
        // identificationFeatures, lifeStages, habitatProfile, habitatRequirements,
        // gardenConnections, beneficialProfile, and ecologicalSignificance are intentionally
        // nullable — catalog entries are populated incrementally as field data is documented.
        return i -> i
                .entityId(id, "id")
                .entityName(name, "name")
                .notNull(this, InsectSpecies::taxonomy, "taxonomy")
                .notNull(this, InsectSpecies::description, "description")
                .notNull(this, InsectSpecies::guilds, "guilds");
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
     * The documented life cycle stages of an insect species.
     * <p>
     * {@code adult} is always present — it is the stage at which field identification
     * occurs and the stage that defines the species' ecological guild at Oak Vista.
     * <p>
     * {@code egg} and {@code larva} are nullable for two distinct reasons:
     * <ul>
     *   <li>{@code egg} — egg-stage data may simply not be documented for the species
     *       at catalog level, even if the species is holometabolous.</li>
     *   <li>{@code larva} — hemimetabolous orders (Blattodea, Orthoptera, Hemiptera)
     *       produce nymphs rather than morphologically distinct larvae; their immature
     *       stages are not modelled here. A null {@code larva} on a holometabolous
     *       species means the larval stage has not yet been catalogued, not that it
     *       does not exist.</li>
     * </ul>
     * <p>
     * The model deliberately omits a {@code pupa} stage. Pupal ecology at Oak Vista
     * reduces to substrate requirements (soil depth, plant litter) which belong in
     * {@link HabitatRequirements}. If pupal biology warrants modelling — for example,
     * to express the duration of vulnerability to soil disturbance — a {@code PupaStage}
     * can be added when the first domain entity requires it.
     */
    public record LifeStages(
            @Nullable EggStage egg,
            @Nullable LarvaStage larva,
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
         * {@code preyTargets} is empty for non-predatory larvae (e.g. lepidopteran
         * caterpillars that feed on plant tissue). {@code preyConsumption} gives a
         * quantitative rate where documented — for example, Chrysoperla larvae consume
         * 200+ aphids per week. {@code remarkableBehavior} captures field-notable
         * behaviour not covered by description or prey data (e.g. the debris-carrying
         * camouflage of lacewing larvae).
         */
        public record LarvaStage(
                @Nullable String commonName,
                String description,
                @Nullable String preyConsumption,
                List<String> preyTargets,
                @Nullable String remarkableBehavior
        ) implements ValueObject {

            @Override
            public Consumer<? extends Constraints> invariants() {
                return i -> i
                        .notNull(this, LarvaStage::description, "description")
                        .notNull(this, LarvaStage::preyTargets, "preyTargets");
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
         */
        public record AdultStage(
                String feeding,
                String role,
                @Nullable String attraction,
                List<String> supportedBy
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
     * and not every species will have all four axes characterised at catalog level.
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
     * </ul>
     */
    public record HabitatRequirements(
            @Nullable String nectarSources,
            @Nullable String shelter,
            @Nullable String preyAvailability,
            @Nullable String lighting
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
