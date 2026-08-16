package com.naturalist.plants.cultivar;

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
 * Behavioral contract for {@link CultivarRepository}.
 * <p>
 * Inherits the {@link EntityRepositoryTest} cases (ADR-002).
 * Supplies Cultivar-specific identity constants and entity construction.
 */
interface CultivarRepositoryTest
        extends EntityRepositoryTest<CultivarName, Cultivar> {

    @Override
    CultivarRepository repository();

    @Override
    default TestEntitySource<CultivarName, Cultivar> source() {
        return db.getNamed(CultivarTestEntitySource.class);
    }

    @Override
    default CultivarName notFoundName() {
        return TestPlantsIdentifiers.Plants.NotFound.cultivarName;
    }

    @Override
    default List<CultivarName> knownEntityNames() {
        return List.of(
                TestPlantsIdentifiers.Plants.Tomato.Cultivars.AmishPaste.name,
                TestPlantsIdentifiers.Plants.Tomato.Cultivars.ItalianPearNicks.name
        );
    }

    @Override
    default Cultivar newEntity() {
        return new Cultivar(
                CultivarName.of("test-" + RandomValue.string()),
                TestPlantsIdentifiers.Plants.Tomato.name,
                "Test Cultivar " + RandomValue.string(),
                description(),
                VarietyType.OPEN_POLLINATED,
                FruitType.PASTE,
                SeedSavingPolicy.SAVE_ANNUALLY,
                null,
                null);
    }

    @Override
    default Cultivar ghostEntity() {
        return new Cultivar(
                CultivarName.of("ghost-" + RandomValue.string()),
                TestPlantsIdentifiers.Plants.Tomato.name,
                "Ghost Cultivar " + RandomValue.string(),
                description(),
                VarietyType.HYBRID_F1,
                FruitType.CHERRY,
                SeedSavingPolicy.DO_NOT_SAVE,
                "fictitious test fixture",
                "ghost notes");
    }

    @Override
    default Cultivar modifiedEntity(Cultivar original) {
        PlantSpeciesName flippedPlant = original.plantName()
                .equals(TestPlantsIdentifiers.Plants.Tomato.name)
                ? TestPlantsIdentifiers.Plants.CaliforniaPipevine.name
                : TestPlantsIdentifiers.Plants.Tomato.name;
        VarietyType flippedVariety = original.varietyType() == VarietyType.OPEN_POLLINATED
                ? VarietyType.HYBRID_F1
                : VarietyType.OPEN_POLLINATED;
        FruitType flippedFruit = original.fruitType() == FruitType.PASTE
                ? FruitType.CHERRY
                : FruitType.PASTE;
        SeedSavingPolicy flippedPolicy = original.seedSavingPolicy() == SeedSavingPolicy.SAVE_ANNUALLY
                ? SeedSavingPolicy.DO_NOT_SAVE
                : SeedSavingPolicy.SAVE_ANNUALLY;
        return new Cultivar(
                original.name(),
                flippedPlant,
                "Modified " + RandomValue.string(),
                description(),
                flippedVariety,
                flippedFruit,
                flippedPolicy,
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
    default void getByPlantName_returnsCultivarsWithMatchingPlantName() {
        var results = repository().getByPlantName(TestPlantsIdentifiers.Plants.Tomato.name);

        assertThat(results)
                .extracting(Cultivar::name)
                .extracting(CultivarName::value)
                .contains("amish-paste", "italian-pear-nicks");
    }

    @Test
    default void getByPlantName_returnsEmptyForUnknownPlant() {
        var results = repository().getByPlantName(TestPlantsIdentifiers.Plants.NotFound.name);

        assertThat(results).isEmpty();
    }
}
