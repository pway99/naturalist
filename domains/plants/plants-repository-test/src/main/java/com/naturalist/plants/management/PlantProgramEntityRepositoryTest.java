package com.naturalist.plants.management;

import com.naturalist.RandomValue;
import com.naturalist.data.EntityRepositoryTest;
import com.naturalist.data.TestEntitySource;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.fieldnotes.Description;
import com.naturalist.plants.PlantSpeciesName;
import com.naturalist.plants.TestPlantsIdentifiers;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Behavioral contract for {@link PlantProgramRepository.PlantProgramEntityRepository}.
 * <p>
 * Inherits the {@link EntityRepositoryTest} cases (ADR-002).
 * Supplies PlantProgram-specific identity constants and entity construction.
 */
interface PlantProgramEntityRepositoryTest
        extends EntityRepositoryTest<PlantProgramName, PlantProgram> {

    @Override
    PlantProgramRepository.PlantProgramEntityRepository repository();

    @Override
    default TestEntitySource<PlantProgramName, PlantProgram> source() {
        return db.getNamed(PlantProgramTestEntitySource.class);
    }

    @Override
    default PlantProgramName notFoundName() {
        return TestPlantsIdentifiers.Plants.NotFound.programName;
    }

    @Override
    default List<PlantProgramName> knownEntityNames() {
        return List.of(
                TestPlantsIdentifiers.Plants.CaliforniaPipevine.Programs.PesticideExclusion,
                TestPlantsIdentifiers.Plants.Borage.Programs.VolunteerThinning
        );
    }

    @Override
    default PlantProgram newEntity() {
        return new PlantProgram(
                PlantProgramName.of(RandomValue.string()),
                TestPlantsIdentifiers.Plants.CaliforniaPipevine.name,
                description(),
                null,
                null);
    }

    @Override
    default PlantProgram ghostEntity() {
        return new PlantProgram(
                PlantProgramName.of(RandomValue.string()),
                TestPlantsIdentifiers.Plants.Borage.name,
                description(),
                null,
                null);
    }

    @Override
    default PlantProgram modifiedEntity(PlantProgram original) {
        PlantSpeciesName flippedPlant = original.plantName()
                .equals(TestPlantsIdentifiers.Plants.CaliforniaPipevine.name)
                ? TestPlantsIdentifiers.Plants.Borage.name
                : TestPlantsIdentifiers.Plants.CaliforniaPipevine.name;
        return new PlantProgram(
                original.name(),
                flippedPlant,
                description(),
                RandomValue.string(),
                RandomValue.string());
    }

    private static Description description() {
        return new Description(
                RandomValue.string(), RandomValue.string(),
                RandomValue.string(), RandomValue.string());
    }

    @Test
    default void getByPlantName_rejectsNull() {
        assertThatThrownBy(() -> repository().getByPlantName(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("plantName");
    }

    @Test
    default void getByPlantName_returnsProgramsWithMatchingPlantName() {
        var results = repository().getByPlantName(
                TestPlantsIdentifiers.Plants.CaliforniaPipevine.name);

        assertThat(results)
                .extracting(PlantProgram::name)
                .extracting(PlantProgramName::value)
                .contains("pipevine-pesticide-exclusion", "pipevine-larval-monitoring");
    }

    @Test
    default void getByPlantName_returnsEmptyForUnknownPlant() {
        var results = repository().getByPlantName(TestPlantsIdentifiers.Plants.NotFound.name);

        assertThat(results).isEmpty();
    }
}
