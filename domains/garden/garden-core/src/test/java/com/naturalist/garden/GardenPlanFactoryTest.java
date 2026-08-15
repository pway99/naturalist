package com.naturalist.garden;

import com.naturalist.data.NaturalistDatabaseExtension;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.zone.subzone.SubZoneName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Exercises the assembled read path: {@link GardenPlanQuery} → {@code GardenPlanFactory} →
 * {@code PlantingQuery} → its repository mock, over the real 2026 Oak Vista beds.
 */
class GardenPlanFactoryTest {

    @RegisterExtension
    NaturalistDatabaseExtension db = NaturalistDatabaseExtension.create();

    private final GardenPlanQuery query = GardenTestContextInternal.create(db).gardenPlanQuery();

    private static final SubZoneName SOUTH_ROW = SubZoneName.of("backyard-south");

    @Test
    void getByZoneName_nullArgument_throws() {
        assertThatThrownBy(() -> query.getByZoneName(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("zoneName");
    }

    /** Garden has no zone catalog, so an unplanted bed and a non-bed are the same answer. */
    @Test
    void getByZoneName_unknownPlace_returnsEmpty() {
        assertThat(query.getByZoneName(TestGardenIdentifiers.Zones.notFound)).isEmpty();
    }

    @Test
    void getByZoneName_backyard_assemblesTheWholeBed() {
        GardenPlan plan = query.getByZoneName(TestGardenIdentifiers.Zones.backyard).orElseThrow();

        assertThat(plan.coversWholeZone()).isTrue();
        assertThat(plan.plantings().size()).isEqualTo(4);
        assertThat(plan.plants()).containsExactlyInAnyOrder(
                TestGardenIdentifiers.Plants.tomato, TestGardenIdentifiers.Plants.eggplant);
        assertThat(plan.cultivars()).hasSize(4);
        assertThat(plan.plantCount()).isEqualTo(12 + 4 + 3 + 2);
    }

    /**
     * The reason a plan takes a sub-zone. A zone-level plan of the back yard lumps three rows
     * together; the front garden is five boxes in one zone, where the box is the useful unit.
     */
    @Test
    void getBySubZoneName_narrowsThePlanToOneRow() {
        GardenPlan wholeBed = query.getByZoneName(TestGardenIdentifiers.Zones.backyard).orElseThrow();
        GardenPlan southRow = query
                .getBySubZoneName(TestGardenIdentifiers.Zones.backyard, SOUTH_ROW).orElseThrow();

        assertThat(southRow.coversWholeZone()).isFalse();
        assertThat(southRow.subZoneName()).isEqualTo(SOUTH_ROW);
        assertThat(southRow.plantings().size()).isLessThan(wholeBed.plantings().size());
        // ...and the row is still mixed: tomatoes and an eggplant share it.
        assertThat(southRow.plants()).containsExactlyInAnyOrder(
                TestGardenIdentifiers.Plants.tomato, TestGardenIdentifiers.Plants.eggplant);
    }

    @Test
    void getBySubZoneName_unplantedRow_returnsEmpty() {
        assertThat(query.getBySubZoneName(TestGardenIdentifiers.Zones.backyard,
                SubZoneName.of("unobtainium-row"))).isEmpty();
    }

    /** The 2026 tomatoes came out in August; the herbs and the eggplant are still in. */
    @Test
    void activeOn_distinguishesWhatIsGrowingFromWhatWasGrown() {
        GardenPlan box1 = query.getByZoneName(TestGardenIdentifiers.Zones.box1).orElseThrow();

        assertThat(box1.activeOn(LocalDate.of(2026, 6, 1)).size()).isEqualTo(5);
        assertThat(box1.activeOn(LocalDate.of(2026, 8, 14)).size()).isEqualTo(3);
    }

    /** A planting whose variety was never recorded still contributes its species to the plan. */
    @Test
    void aPlantingWithNoVarietyStillNamesItsPlant() {
        GardenPlan box1 = query.getByZoneName(TestGardenIdentifiers.Zones.box1).orElseThrow();

        assertThat(box1.plants()).contains(TestGardenIdentifiers.Plants.radish);
        assertThat(box1.cultivars()).hasSize(4);
    }
}
