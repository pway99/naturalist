package com.naturalist.insects.lifestage;

import com.naturalist.fieldnotes.Description;
import com.naturalist.habitat.HabitatProfile;
import com.naturalist.habitat.LightRegime;
import com.naturalist.habitat.MoistureRegime;
import com.naturalist.habitat.VerticalLayer;
import com.naturalist.insects.InsectFamilyName;
import com.naturalist.insects.InsectGenusName;
import com.naturalist.insects.InsectOrderName;
import com.naturalist.insects.InsectRankName;
import com.naturalist.insects.InsectSpeciesName;
import com.naturalist.insects.InsectSubspeciesName;
import com.naturalist.insects.LifeStageName;
import com.naturalist.ddd.Enums;
import com.naturalist.observability.Constraints;
import com.naturalist.observability.Level;
import com.naturalist.observability.MethodObserver;
import com.naturalist.observability.Observer;
import com.naturalist.persistence.Dbo;
import com.naturalist.persistence.DboSchema;
import com.naturalist.plants.PlantSpeciesName;
import com.naturalist.taxonomy.LinealRank;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Single-table persistence view of the sealed {@link LifeStage} — the four permits (egg/larva/pupa/adult)
 * share one {@code insect_life_stage} table discriminated by {@code stage_kind}; permit-specific columns are
 * null for the other kinds. The polymorphic {@link InsectRankName} parent stores as {@code parent_rank} +
 * {@code parent_name} (no FK). The owned {@code StagePhenology} windows, {@code StageHabitat} zones/layers,
 * and the larva/adult plant lists live in child tables passed to {@link #toEntity}. {@code PupaStage}'s
 * sealed {@link PupaStage.DiapauseRegulation} flattens to {@code diapause_kind} + two generic permit fields.
 */
@DboSchema(table = "insect_life_stage", primaryKey = "id", unique = {"name"}, entity = LifeStage.class)
final class InsectLifeStageDbo implements Dbo {
    Long id;
    String name;
    String stageKind;      // EGG|LARVA|PUPA|ADULT
    String parentRank;
    String parentName;
    // StagePhenology
    String phenologyNotes;
    // StageHabitat (+ HabitatProfile scalars; zones/layers in child tables)
    String habitatMoisture;
    String habitatLight;
    String habitatSubstrate;
    String habitatMicroclimate;
    String habitatSpatialNotes;
    // StageChemistryRole (nullable; present iff chemistryRole non-null)
    String chemistryRole;
    String chemistryNotes;
    // EggStage
    String eggColorProgression;
    String eggLayingPattern;
    String eggAdaptiveSignificance;
    // LarvaStage (hostPlants/parasitoidHosts in child tables)
    String larvaFeedingStrategy;
    String larvaRemarkableBehavior;
    String larvaInstarProgression;
    // PupaStage (diapause flattened)
    String pupaAppearance;
    String pupaAdaptiveSignificance;
    String diapauseKind;   // PHOTOPERIOD|FOOD_WATER|TEMPERATURE|NON_DIAPAUSING
    String diapauseFieldA;
    String diapauseFieldB;
    // AdultStage (nectarSources in child table)
    String adultFeedingHabit;
    String adultEcologicalRole;
    String adultLifespan;
    String descriptionPreschool;
    String descriptionElementary;
    String descriptionSecondary;
    String descriptionUniversity;

    private static final Observer observer = Observer.forClass(InsectLifeStageDbo.class);

    static InsectLifeStageDbo from(LifeStage stage) {
        observer.arguments("from", i -> i
                .namedEntity(stage, "stage"))
                .throwWhenInvalid();
        InsectLifeStageDbo d = new InsectLifeStageDbo();
        d.name = stage.name().value();
        d.stageKind = stage.kind().name();
        d.parentRank = stage.parentName().rank().name();
        d.parentName = stage.parentName().value();
        d.phenologyNotes = stage.phenology().notes();
        StageHabitat habitat = stage.habitat();
        HabitatProfile profile = habitat.profile();
        d.habitatMoisture = profile.moisture() == null ? null : profile.moisture().name();
        d.habitatLight = profile.light() == null ? null : profile.light().name();
        d.habitatSubstrate = habitat.substrate();
        d.habitatMicroclimate = habitat.microclimate();
        d.habitatSpatialNotes = habitat.spatialNotes();
        StageChemistryRole chem = stage.chemistryRole();
        if (chem != null) {
            d.chemistryRole = chem.role().name();
            d.chemistryNotes = chem.notes();
        }
        Description desc = stage.description();
        d.descriptionPreschool = desc.preschool();
        d.descriptionElementary = desc.elementary();
        d.descriptionSecondary = desc.secondary();
        d.descriptionUniversity = desc.university();
        switch (stage) {
            case EggStage e -> {
                d.eggColorProgression = e.colorProgression();
                d.eggLayingPattern = e.layingPattern();
                d.eggAdaptiveSignificance = e.adaptiveSignificance();
            }
            case LarvaStage l -> {
                d.larvaFeedingStrategy = l.feedingStrategy() == null ? null : l.feedingStrategy().name();
                d.larvaRemarkableBehavior = l.remarkableBehavior();
                d.larvaInstarProgression = l.instarProgression();
            }
            case PupaStage p -> {
                d.pupaAppearance = p.appearance();
                d.pupaAdaptiveSignificance = p.adaptiveSignificance();
                switch (p.diapauseRegulation()) {
                    case null -> {
                    }
                    case PupaStage.PhotoperiodRegulated x -> {
                        d.diapauseKind = "PHOTOPERIOD";
                        d.diapauseFieldA = x.criticalDaylength();
                        d.diapauseFieldB = x.chillRequirement();
                    }
                    case PupaStage.FoodWaterContentRegulated x -> {
                        d.diapauseKind = "FOOD_WATER";
                        d.diapauseFieldA = x.mechanism();
                        d.diapauseFieldB = x.cohortSplitNotes();
                    }
                    case PupaStage.TemperatureRegulated x -> {
                        d.diapauseKind = "TEMPERATURE";
                        d.diapauseFieldA = x.entryThreshold();
                        d.diapauseFieldB = x.exitThreshold();
                    }
                    case PupaStage.NonDiapausing x -> d.diapauseKind = "NON_DIAPAUSING";
                }
            }
            case AdultStage a -> {
                d.adultFeedingHabit = a.feedingHabit() == null ? null : a.feedingHabit().name();
                d.adultEcologicalRole = a.ecologicalRole();
                d.adultLifespan = a.lifespan();
            }
        }
        return d;
    }

    LifeStage toEntity(List<StagePhenology.ActivityWindow> windows,
                       Set<com.naturalist.habitat.HabitatZone> zones,
                       Set<VerticalLayer> layers,
                       List<PlantSpeciesName> hostPlants,
                       List<InsectSpeciesName> parasitoidHosts,
                       List<PlantSpeciesName> nectarSources) {
        MethodObserver methodObserver = observer.forMethod("toEntity");
        methodObserver.arguments(s -> s
                        .notNull(windows, "windows")
                        .notNull(zones, "zones")
                        .notNull(layers, "layers")
                        .notNull(hostPlants, "hostPlants")
                        .notNull(parasitoidHosts, "parasitoidHosts")
                        .notNull(nectarSources, "nectarSources")
        ).throwWhenInvalid();

        methodObserver.state(s -> s
                .isTrue(isStageKind(stageKind), "isStageKind"))
                .throwWhenInvalid();
        methodObserver.state(s -> s.observable(this, "row")).observe(Level.WARN);

        LifeStageName lsName = LifeStageName.of(name);
        InsectRankName parent = parentRankName();
        StagePhenology phenology = new StagePhenology(windows, phenologyNotes);
        HabitatProfile profile = new HabitatProfile(
                zones,
                Enums.named(MoistureRegime.class, habitatMoisture),
                Enums.named(LightRegime.class, habitatLight),
                layers.isEmpty() ? null : layers);
        StageHabitat habitat = new StageHabitat(profile, habitatSubstrate, habitatMicroclimate, habitatSpatialNotes);
        StageChemistryRole chem = chemistryRole == null ? null
                : new StageChemistryRole(Enums.named(StageChemistryRole.Role.class, chemistryRole), chemistryNotes);
        Description description = new Description(
                descriptionPreschool, descriptionElementary, descriptionSecondary, descriptionUniversity);

        return switch (stageKind) {
            case "EGG" -> new EggStage(lsName, parent, phenology, habitat, chem, description,
                    eggColorProgression, eggLayingPattern, eggAdaptiveSignificance);
            case "LARVA" -> new LarvaStage(lsName, parent, phenology, habitat, chem, description,
                    Enums.named(LarvaStage.FeedingStrategy.class, larvaFeedingStrategy),
                    hostPlants, parasitoidHosts, larvaRemarkableBehavior, larvaInstarProgression);
            case "PUPA" -> new PupaStage(lsName, parent, phenology, habitat, chem, description,
                    pupaAppearance, diapauseRegulation(), pupaAdaptiveSignificance);
            case "ADULT" -> new AdultStage(lsName, parent, phenology, habitat, chem, description,
                    Enums.named(AdultStage.FeedingHabit.class, adultFeedingHabit),
                    nectarSources, adultEcologicalRole, adultLifespan);
            default -> throw new IllegalStateException("unreachable: " + stageKind);
        };
    }

    /**
     * The parent's typed name, or null when {@code parentRank} names no rank {@link InsectRankName} accepts.
     * Never throws: an invalid slug is handed on inside the name for the consumer to observe.
     */
    private InsectRankName parentRankName() {
        LinealRank rank = Enums.named(LinealRank.class, parentRank);
        if (rank == null) return null;
        return switch (rank) {
            case ORDER -> InsectOrderName.of(parentName);
            case FAMILY -> InsectFamilyName.of(parentName);
            case GENUS -> InsectGenusName.of(parentName);
            case SPECIES -> InsectSpeciesName.of(parentName);
            case SUBSPECIES -> InsectSubspeciesName.of(parentName);
            case KINGDOM, PHYLUM, CLASS -> null;
        };
    }

    private PupaStage.DiapauseRegulation diapauseRegulation() {
        if (!isDiapauseKind(diapauseKind)) return null;
        return switch (diapauseKind) {
            case "PHOTOPERIOD" -> new PupaStage.PhotoperiodRegulated(diapauseFieldA, diapauseFieldB);
            case "FOOD_WATER" -> new PupaStage.FoodWaterContentRegulated(diapauseFieldA, diapauseFieldB);
            case "TEMPERATURE" -> new PupaStage.TemperatureRegulated(diapauseFieldA, diapauseFieldB);
            case "NON_DIAPAUSING" -> new PupaStage.NonDiapausing();
            default -> null;
        };
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return c -> c
                .kebabFormat(name, "name")
                .maxLength(name, 80, "name")
                .notBlank(stageKind, "stageKind")
                .maxLength(stageKind, 16, "stageKind")
                .isTrue(isStageKind(stageKind), "isStageKind")
                .isTrue(permitColumnsMatchStageKind(), "permitColumnsMatchStageKind")
                .isTrue(chemistryNotesHaveRole(), "chemistryNotesHaveRole")
                .isTrue(diapauseFieldsMatchKind(), "diapauseFieldsMatchKind")
                .whenNotNull(diapauseKind, k -> k.isTrue(isDiapauseKind(diapauseKind), "isDiapauseKind"))
                .notBlank(parentRank, "parentRank")
                .maxLength(parentRank, 16, "parentRank")
                .isTrue(isInsectRank(parentRank), "isInsectRank")
                .enumValue(habitatMoisture, MoistureRegime.class, "habitatMoisture")
                .enumValue(habitatLight, LightRegime.class, "habitatLight")
                .enumValue(chemistryRole, StageChemistryRole.Role.class, "chemistryRole")
                .enumValue(larvaFeedingStrategy, LarvaStage.FeedingStrategy.class, "larvaFeedingStrategy")
                .enumValue(adultFeedingHabit, AdultStage.FeedingHabit.class, "adultFeedingHabit")
                .kebabFormat(parentName, "parentName")
                .maxLength(parentName, 96, "parentName")
                .notBlank(descriptionPreschool, "descriptionPreschool")
                .notBlank(descriptionElementary, "descriptionElementary")
                .notBlank(descriptionSecondary, "descriptionSecondary")
                .notBlank(descriptionUniversity, "descriptionUniversity");
    }

    /** Whether {@code kind} names one of the four life stage kinds this row can hold; null does not. */
    private static boolean isStageKind(String kind) {
        return kind != null && switch (kind) {
            case "EGG", "LARVA", "PUPA", "ADULT" -> true;
            default -> false;
        };
    }

    /** Whether {@code kind} names one of the four diapause regulations a pupa row can hold; null does not. */
    private static boolean isDiapauseKind(String kind) {
        return kind != null && switch (kind) {
            case "PHOTOPERIOD", "FOOD_WATER", "TEMPERATURE", "NON_DIAPAUSING" -> true;
            default -> false;
        };
    }

    /** Whether {@code value} names one of the five ranks {@link InsectRankName} accepts; null does not. */
    private static boolean isInsectRank(String value) {
        return InsectRankName.isInsectRank(Enums.named(LinealRank.class, value));
    }

    /**
     * Whether every permit-specific column that is set belongs to this row's {@code stageKind}: the
     * single table holds all four permits, and a column of another kind would be silently dropped by
     * {@link #toEntity}.
     */
    private boolean permitColumnsMatchStageKind() {
        boolean egg = anySet(eggColorProgression, eggLayingPattern, eggAdaptiveSignificance);
        boolean larva = anySet(larvaFeedingStrategy, larvaRemarkableBehavior, larvaInstarProgression);
        boolean pupa = anySet(pupaAppearance, pupaAdaptiveSignificance, diapauseKind, diapauseFieldA,
                diapauseFieldB);
        boolean adult = anySet(adultFeedingHabit, adultEcologicalRole, adultLifespan);
        return (!egg || "EGG".equals(stageKind))
                && (!larva || "LARVA".equals(stageKind))
                && (!pupa || "PUPA".equals(stageKind))
                && (!adult || "ADULT".equals(stageKind));
    }

    /**
     * Whether {@code chemistryNotes} has the {@code chemistryRole} it describes: {@link #toEntity}
     * builds no {@link StageChemistryRole} without a role, so notes without one would be dropped.
     */
    private boolean chemistryNotesHaveRole() {
        return chemistryNotes == null || chemistryRole != null;
    }

    /**
     * Whether the generic diapause columns are set only for a kind that carries them: every kind but
     * {@code NON_DIAPAUSING}. {@link #diapauseRegulation} ignores them for any other row.
     */
    private boolean diapauseFieldsMatchKind() {
        return !anySet(diapauseFieldA, diapauseFieldB)
                || (isDiapauseKind(diapauseKind) && !"NON_DIAPAUSING".equals(diapauseKind));
    }

    private static boolean anySet(String... columns) {
        return Arrays.stream(columns).anyMatch(Objects::nonNull);
    }


}
