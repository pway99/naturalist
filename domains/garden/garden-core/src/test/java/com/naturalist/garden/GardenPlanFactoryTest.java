package com.naturalist.garden;

import com.naturalist.data.NaturalistDatabaseExtension;
import com.naturalist.exception.InvariantViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Exercises the assembled read path: {@link GardenPlanQuery} → {@code GardenPlanFactory} → the two
 * entity queries → their repository mocks, over the real 2026 Oak Vista plantings.
 */
class GardenPlanFactoryTest {

    @RegisterExtension
    NaturalistDatabaseExtension db = NaturalistDatabaseExtension.create();

    private final GardenPlanQuery query = GardenTestContextInternal.create(db).gardenPlanQuery();

    @Test
    void getByCropTypeName_nullArgument_throws() {
        assertThatThrownBy(() -> query.getByCropTypeName(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("cropTypeName");
    }

    @Test
    void getByCropTypeName_unknownType_returnsEmpty() {
        assertThat(query.getByCropTypeName(TestGardenIdentifiers.CropTypes.NotFound.cropType))
                .isEmpty();
    }

    /** The varieties are derived from what was planted, not from a catalog garden does not keep. */
    @Test
    void getByCropTypeName_tomato_assemblesTheTypeWithEveryVarietyPlanted() {
        GardenPlan plan = query.getByCropTypeName(TestGardenIdentifiers.CropTypes.Tomato.name)
                .orElseThrow();

        assertThat(plan.cropTypeName()).isEqualTo(TestGardenIdentifiers.CropTypes.Tomato.name);
        assertThat(plan.cropType().isBotanicallyIdentified()).isTrue();
        assertThat(plan.plantings().size()).isEqualTo(4);
        assertThat(plan.cultivarsPlanted()).containsExactlyInAnyOrder(
                TestGardenIdentifiers.CropTypes.Tomato.Cultivars.amishPaste,
                TestGardenIdentifiers.CropTypes.Tomato.Cultivars.italianPearNicks,
                TestGardenIdentifiers.CropTypes.Tomato.Cultivars.sanMarzanoF2,
                TestGardenIdentifiers.CropTypes.Tomato.Cultivars.sungoldCherry);
    }

    /** The 2026 tomatoes came out in August; the basil is still in. */
    @Test
    void activeOn_distinguishesWhatIsInTheGroundFromWhatWasGrown() {
        GardenPlan tomato = query.getByCropTypeName(TestGardenIdentifiers.CropTypes.Tomato.name)
                .orElseThrow();
        GardenPlan basil = query.getByCropTypeName(TestGardenIdentifiers.CropTypes.Basil.name)
                .orElseThrow();
        LocalDate now = LocalDate.of(2026, 8, 14);

        assertThat(tomato.activeOn(LocalDate.of(2026, 6, 1)).size()).isEqualTo(4);
        assertThat(tomato.activeOn(now).stream()).isEmpty();
        assertThat(basil.activeOn(now).size()).isEqualTo(2);
    }

    /**
     * A crop type soil is tested for but nothing has been planted as. The August 2026 FGL panel is
     * a lettuce panel; the lettuce is not in yet. An empty plan is a valid plan.
     */
    @Test
    void getByCropTypeName_lettuce_assemblesATypeWithNoPlantings() {
        GardenPlan plan = query.getByCropTypeName(TestGardenIdentifiers.CropTypes.Lettuce.name)
                .orElseThrow();

        assertThat(plan.hasBeenPlanted()).isFalse();
        assertThat(plan.cultivarsPlanted()).isEmpty();
        assertThat(plan.cropType().isBotanicallyIdentified()).isFalse();
    }
}
