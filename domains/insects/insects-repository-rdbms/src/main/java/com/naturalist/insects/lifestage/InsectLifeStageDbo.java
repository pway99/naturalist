package com.naturalist.insects.lifestage;

import com.naturalist.fieldnotes.Description;
import com.naturalist.habitat.HabitatProfile;
import com.naturalist.habitat.LightRegime;
import com.naturalist.habitat.MoistureRegime;
import com.naturalist.habitat.VerticalLayer;
import com.naturalist.insects.InsectRankName;
import com.naturalist.insects.LifeStageName;
import com.naturalist.observability.Constraints;
import com.naturalist.observability.Observer;
import com.naturalist.persistence.Dbo;
import com.naturalist.persistence.DboSchema;
import com.naturalist.plants.PlantSpeciesName;
import com.naturalist.taxonomy.LinealRank;

import java.util.List;
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

    static InsectLifeStageDbo from(LifeStage s) {
        InsectLifeStageDbo d = new InsectLifeStageDbo();
        d.name = s.name().value();
        d.stageKind = s.kind().name();
        d.parentRank = s.parentName().rank().name();
        d.parentName = s.parentName().value();
        d.phenologyNotes = s.phenology().notes();
        StageHabitat habitat = s.habitat();
        HabitatProfile profile = habitat.profile();
        d.habitatMoisture = profile.moisture() == null ? null : profile.moisture().name();
        d.habitatLight = profile.light() == null ? null : profile.light().name();
        d.habitatSubstrate = habitat.substrate();
        d.habitatMicroclimate = habitat.microclimate();
        d.habitatSpatialNotes = habitat.spatialNotes();
        StageChemistryRole chem = s.chemistryRole();
        if (chem != null) {
            d.chemistryRole = chem.role().name();
            d.chemistryNotes = chem.notes();
        }
        Description desc = s.description();
        d.descriptionPreschool = desc.preschool();
        d.descriptionElementary = desc.elementary();
        d.descriptionSecondary = desc.secondary();
        d.descriptionUniversity = desc.university();
        switch (s) {
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
                    case null -> { }
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
        Observer.forClass(InsectLifeStageDbo.class)
                .arguments("from", i -> i.observable(d, "dbo"))
                .throwWhenInvalid();
        return d;
    }

    LifeStage toEntity(List<StagePhenology.ActivityWindow> windows,
                       Set<com.naturalist.habitat.HabitatZone> zones,
                       Set<VerticalLayer> layers,
                       List<PlantSpeciesName> hostPlants,
                       List<com.naturalist.insects.InsectSpeciesName> parasitoidHosts,
                       List<PlantSpeciesName> nectarSources) {
        LifeStageName lsName = LifeStageName.of(name);
        InsectRankName parent = InsectRankName.of(parentName, LinealRank.valueOf(parentRank));
        StagePhenology phenology = new StagePhenology(windows, phenologyNotes);
        HabitatProfile profile = new HabitatProfile(
                zones,
                habitatMoisture == null ? null : MoistureRegime.valueOf(habitatMoisture),
                habitatLight == null ? null : LightRegime.valueOf(habitatLight),
                layers.isEmpty() ? null : layers);
        StageHabitat habitat = new StageHabitat(profile, habitatSubstrate, habitatMicroclimate, habitatSpatialNotes);
        StageChemistryRole chem = chemistryRole == null ? null
                : new StageChemistryRole(StageChemistryRole.Role.valueOf(chemistryRole), chemistryNotes);
        Description description = new Description(
                descriptionPreschool, descriptionElementary, descriptionSecondary, descriptionUniversity);

        return switch (stageKind) {
            case "EGG" -> new EggStage(lsName, parent, phenology, habitat, chem, description,
                    eggColorProgression, eggLayingPattern, eggAdaptiveSignificance);
            case "LARVA" -> new LarvaStage(lsName, parent, phenology, habitat, chem, description,
                    larvaFeedingStrategy == null ? null : LarvaStage.FeedingStrategy.valueOf(larvaFeedingStrategy),
                    hostPlants, parasitoidHosts, larvaRemarkableBehavior, larvaInstarProgression);
            case "PUPA" -> new PupaStage(lsName, parent, phenology, habitat, chem, description,
                    pupaAppearance, diapauseRegulation(), pupaAdaptiveSignificance);
            case "ADULT" -> new AdultStage(lsName, parent, phenology, habitat, chem, description,
                    adultFeedingHabit == null ? null : AdultStage.FeedingHabit.valueOf(adultFeedingHabit),
                    nectarSources, adultEcologicalRole, adultLifespan);
            default -> throw new IllegalStateException("Unknown life stage kind: " + stageKind);
        };
    }

    private PupaStage.DiapauseRegulation diapauseRegulation() {
        if (diapauseKind == null) return null;
        return switch (diapauseKind) {
            case "PHOTOPERIOD" -> new PupaStage.PhotoperiodRegulated(diapauseFieldA, diapauseFieldB);
            case "FOOD_WATER" -> new PupaStage.FoodWaterContentRegulated(diapauseFieldA, diapauseFieldB);
            case "TEMPERATURE" -> new PupaStage.TemperatureRegulated(diapauseFieldA, diapauseFieldB);
            case "NON_DIAPAUSING" -> new PupaStage.NonDiapausing();
            default -> throw new IllegalStateException("Unknown diapause kind: " + diapauseKind);
        };
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return c -> c
                .notNull(name, "name").kebabFormat(name, "name").maxLength(name, 80, "name")
                .notBlank(stageKind, "stageKind").maxLength(stageKind, 16, "stageKind")
                .notBlank(parentRank, "parentRank").maxLength(parentRank, 16, "parentRank")
                .notNull(parentName, "parentName").kebabFormat(parentName, "parentName")
                    .maxLength(parentName, 96, "parentName")
                .notBlank(descriptionPreschool, "descriptionPreschool")
                .notBlank(descriptionElementary, "descriptionElementary")
                .notBlank(descriptionSecondary, "descriptionSecondary")
                .notBlank(descriptionUniversity, "descriptionUniversity");
    }
}
