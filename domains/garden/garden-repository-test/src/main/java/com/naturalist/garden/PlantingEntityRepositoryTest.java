package com.naturalist.garden;

import com.naturalist.RandomValue;
import com.naturalist.data.EntityRepositoryTest;
import com.naturalist.data.TestEntitySource;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.plants.cultivar.CultivarName;
import com.naturalist.zone.ZoneName;
import com.naturalist.zone.subzone.SubZoneName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Behavioral contract for {@link PlantingRepository}, plus the {@code getByCropTypeName} (what did
 * we grow of this) and {@code getByZoneName} (what is in this bed) reverse lookups.
 */
interface PlantingEntityRepositoryTest extends EntityRepositoryTest<PlantingId, Planting> {

    @Override
    PlantingRepository repository();

    @Override
    default TestEntitySource<PlantingId, Planting> source() {
        return db.getNamed(PlantingTestEntitySource.class);
    }

    @Override
    default PlantingId notFoundName() {
        return TestGardenIdentifiers.CropTypes.NotFound.planting;
    }

    @Override
    default List<PlantingId> knownEntityNames() {
        return List.of(
                TestGardenIdentifiers.CropTypes.Tomato.Plantings.amishPasteBackyard,
                TestGardenIdentifiers.CropTypes.Basil.Plantings.genoveseBox1);
    }

    @Override
    default Planting newEntity() {
        return sample(PlantingId.create());
    }

    @Override
    default Planting ghostEntity() {
        return sample(PlantingId.create());
    }

    /**
     * Every mutable field changed, including the two that record a planting ending: a removed date
     * where there was none, and the variety becoming known where it was not.
     */
    @Override
    default Planting modifiedEntity(Planting original) {
        return new Planting(
                original.id(),
                TestGardenIdentifiers.CropTypes.Basil.name,
                CultivarName.of("thai-basil"),
                ZoneName.of(RandomValue.string()),
                SubZoneName.of(RandomValue.string()),
                7,
                LocalDate.of(2025, 3, 1),
                LocalDate.of(2025, 9, 1),
                RandomValue.string());
    }

    @Test
    default void getByCropTypeName_nullArgument() {
        assertThatThrownBy(() -> repository().getByCropTypeName(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("cropTypeName");
    }

    @Test
    default void getByCropTypeName_unknown_returnsEmpty() {
        assertThat(repository().getByCropTypeName(TestGardenIdentifiers.CropTypes.NotFound.cropType))
                .isEmpty();
    }

    @Test
    default void getByCropTypeName_known_returnsEveryVarietyGrownAsThatType() {
        List<Planting> result =
                repository().getByCropTypeName(TestGardenIdentifiers.CropTypes.Tomato.name);

        assertThat(result).hasSize(4);
        assertThat(result).extracting(Planting::cultivarName)
                .containsExactlyInAnyOrder(
                        TestGardenIdentifiers.CropTypes.Tomato.Cultivars.amishPaste,
                        TestGardenIdentifiers.CropTypes.Tomato.Cultivars.italianPearNicks,
                        TestGardenIdentifiers.CropTypes.Tomato.Cultivars.sanMarzanoF2,
                        TestGardenIdentifiers.CropTypes.Tomato.Cultivars.sungoldCherry);
    }

    @Test
    default void getByZoneName_nullArgument() {
        assertThatThrownBy(() -> repository().getByZoneName(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("zoneName");
    }

    @Test
    default void getByZoneName_unknown_returnsEmpty() {
        assertThat(repository().getByZoneName(ZoneName.of("unobtainium-bed"))).isEmpty();
    }

    /**
     * The back yard holds four plantings of two different crop types — the mixed-bed case. A query
     * by zone must never collapse a bed to one crop.
     */
    @Test
    default void getByZoneName_known_returnsEveryCropTypeInThatBed() {
        List<Planting> result = repository().getByZoneName(ZoneName.of("backyard"));

        assertThat(result).hasSize(4);
        assertThat(result).extracting(Planting::cropTypeName)
                .contains(TestGardenIdentifiers.CropTypes.Tomato.name,
                        TestGardenIdentifiers.CropTypes.Eggplant.name);
    }

    /** One row, two crop types — tomatoes and an eggplant share the back yard south row. */
    @Test
    default void aSubZoneCanHoldMoreThanOneCropType() {
        List<Planting> southRow = repository().getByZoneName(ZoneName.of("backyard")).stream()
                .filter(p -> SubZoneName.of("backyard-south").equals(p.subZoneName()))
                .toList();

        assertThat(southRow).hasSize(2);
        assertThat(southRow).extracting(Planting::cropTypeName)
                .containsExactlyInAnyOrder(TestGardenIdentifiers.CropTypes.Tomato.name,
                        TestGardenIdentifiers.CropTypes.Eggplant.name);
    }

    private static Planting sample(PlantingId id) {
        return new Planting(
                id,
                TestGardenIdentifiers.CropTypes.Tomato.name,
                TestGardenIdentifiers.CropTypes.Tomato.Cultivars.amishPaste,
                ZoneName.of(RandomValue.string()),
                null,
                12,
                LocalDate.of(2026, 4, 6),
                null,
                null);
    }
}
