package com.naturalist.insects.lifestage;

import com.naturalist.RandomValue;
import com.naturalist.data.EntityRepositoryTest;
import com.naturalist.data.TestEntitySource;
import com.naturalist.fieldnotes.Description;
import com.naturalist.habitat.HabitatProfile;
import com.naturalist.habitat.HabitatZone;
import com.naturalist.habitat.LightRegime;
import com.naturalist.habitat.MoistureRegime;
import com.naturalist.insects.InsectSpeciesName;
import com.naturalist.insects.LifeStageKind;
import com.naturalist.insects.LifeStageName;
import com.naturalist.insects.TestInsectsIdentifiers;

import java.time.MonthDay;
import java.util.List;
import java.util.Set;

/**
 * Behavioral contract for {@link LifeStageRepository.LifeStageEntityRepository}.
 * <p>
 * Inherits the {@link EntityRepositoryTest} cases (ADR-002).
 * Supplies LifeStage-specific identity constants and entity construction.
 */
interface LifeStageEntityRepositoryTest
        extends EntityRepositoryTest<LifeStageName, LifeStage> {

    @Override
    LifeStageRepository.LifeStageEntityRepository repository();

    @Override
    default TestEntitySource<LifeStageName, LifeStage> source() {
        return db.getNamed(LifeStageTestEntitySource.class);
    }

    @Override
    default LifeStageName notFoundName() {
        return TestInsectsIdentifiers.InsectSpecies.NotFound.lifeStageName;
    }

    @Override
    default List<LifeStageName> knownEntityNames() {
        return List.of(
                TestInsectsIdentifiers.InsectFamily.Tachinidae.LifeStages.Egg,
                TestInsectsIdentifiers.InsectFamily.Braconidae.LifeStages.Larva);
    }

    @Override
    default LifeStage newEntity() {
        return new EggStage(
                LifeStageName.of(InsectSpeciesName.of("test-species-xx"), LifeStageKind.EGG),
                phenology(),
                habitat(),
                null,
                description(),
                null, null, null);
    }

    @Override
    default LifeStage ghostEntity() {
        return new EggStage(
                LifeStageName.of(InsectSpeciesName.of("test-ghost-xx"), LifeStageKind.EGG),
                phenology(),
                habitat(),
                null,
                description(),
                null, null, null);
    }

    @Override
    default LifeStage modifiedEntity(LifeStage original) {
        return new EggStage(
                original.name(),
                phenology(),
                habitat(),
                new StageChemistryRole(StageChemistryRole.Role.ACQUISITION, RandomValue.string()),
                description(),
                RandomValue.string(),
                RandomValue.string(),
                RandomValue.string());
    }

    private static StagePhenology phenology() {
        return new StagePhenology(
                List.of(new StagePhenology.ActivityWindow(
                        MonthDay.of(4, 1), MonthDay.of(6, 15), MonthDay.of(10, 15), null)),
                null);
    }

    private static StageHabitat habitat() {
        return new StageHabitat(
                new HabitatProfile(
                        Set.of(HabitatZone.CULTIVATED),
                        MoistureRegime.MESIC,
                        LightRegime.PARTIAL_SUN,
                        null),
                null, null, null);
    }

    private static Description description() {
        return new Description(
                RandomValue.string(), RandomValue.string(),
                RandomValue.string(), RandomValue.string());
    }
}
