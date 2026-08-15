package com.naturalist.plants.cultivar;

import com.naturalist.RandomValue;
import com.naturalist.fieldnotes.Description;
import com.naturalist.observability.InvariantObservation;
import com.naturalist.observability.MethodObserver;
import com.naturalist.observability.Observer;
import com.naturalist.plants.PlantName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CultivarTest {

    private static final Observer observer = Observer.forClass(CultivarTest.class);

    @Test
    void fullyPopulatedCultivarIsValid() {
        MethodObserver mo = observer.forMethod("fullyPopulatedCultivarIsValid");
        Cultivar cultivar = new Cultivar(
                CultivarName.of("italian-pear-nicks"),
                PlantName.of("solanum-lycopersicum"),
                "Italian Pear (Nick's)",
                description(),
                VarietyType.OPEN_POLLINATED,
                FruitType.PASTE,
                SeedSavingPolicy.SAVE_ANNUALLY,
                "50+ year family lineage",
                "founding generation of the Chico adaptation program");

        InvariantObservation result = mo.namedEntity(cultivar, "cultivar");

        assertThat(result.violations()).isEmpty();
    }

    @Test
    void nullFruitTypeIsValidForNonFruitingCultivars() {
        // Leaf herbs (basil, parsley) carry a null fruitType — the enum is
        // tomato-specific and a non-fruiting cultivar has nothing to say here.
        MethodObserver mo = observer.forMethod("nullFruitTypeIsValidForNonFruitingCultivars");
        Cultivar cultivar = new Cultivar(
                CultivarName.of("genovese-basil"),
                PlantName.of("ocimum-basilicum"),
                "Genovese Basil",
                description(),
                VarietyType.OPEN_POLLINATED,
                null,
                SeedSavingPolicy.SAVE_ANNUALLY,
                null,
                null);

        InvariantObservation result = mo.namedEntity(cultivar, "cultivar");

        assertThat(result.violations()).isEmpty();
    }

    @Test
    void blankCommonNameIsInvalid() {
        MethodObserver mo = observer.forMethod("blankCommonNameIsInvalid");
        Cultivar cultivar = new Cultivar(
                CultivarName.of("amish-paste"),
                PlantName.of("solanum-lycopersicum"),
                "   ",
                description(),
                VarietyType.UNKNOWN,
                FruitType.PASTE,
                SeedSavingPolicy.CONDITIONAL,
                null,
                null);

        InvariantObservation result = mo.namedEntity(cultivar, "cultivar");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .containsExactly(".cultivar.commonName");
    }

    @Test
    void allNullComponentsReportEveryViolation() {
        MethodObserver mo = observer.forMethod("allNullComponentsReportEveryViolation");
        Cultivar cultivar = new Cultivar(null, null, null, null, null, null, null, null, null);

        InvariantObservation result = mo.namedEntity(cultivar, "cultivar");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .containsExactlyInAnyOrder(
                        ".cultivar.name",
                        ".cultivar.plantName",
                        ".cultivar.commonName",
                        ".cultivar.description",
                        ".cultivar.varietyType",
                        ".cultivar.seedSavingPolicy");
    }

    @Test
    void seedSavingPredicatesReadTheirEnums() {
        Cultivar openPollinated = new Cultivar(
                CultivarName.of("italian-pear-nicks"),
                PlantName.of("solanum-lycopersicum"),
                "Italian Pear (Nick's)",
                description(),
                VarietyType.OPEN_POLLINATED,
                FruitType.PASTE,
                SeedSavingPolicy.SAVE_ANNUALLY,
                null,
                null);
        Cultivar hybrid = new Cultivar(
                CultivarName.of("sungold-cherry"),
                PlantName.of("solanum-lycopersicum"),
                "Sungold Cherry",
                description(),
                VarietyType.HYBRID_F1,
                FruitType.CHERRY,
                SeedSavingPolicy.DO_NOT_SAVE,
                null,
                null);

        assertThat(openPollinated.breedsTrueFromSeed()).isTrue();
        assertThat(openPollinated.requiresSeedSaving()).isTrue();
        assertThat(openPollinated.isSauceVariety()).isTrue();
        assertThat(hybrid.breedsTrueFromSeed()).isFalse();
        assertThat(hybrid.requiresSeedSaving()).isFalse();
        assertThat(hybrid.isSauceVariety()).isFalse();
    }

    private static Description description() {
        return new Description(
                RandomValue.string(), RandomValue.string(),
                RandomValue.string(), RandomValue.string());
    }
}
