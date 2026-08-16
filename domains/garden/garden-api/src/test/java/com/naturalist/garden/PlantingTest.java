package com.naturalist.garden;

import com.naturalist.observability.InvariantObservation;
import com.naturalist.observability.Observer;
import com.naturalist.plants.PlantSpeciesName;
import com.naturalist.plants.cultivar.CultivarName;
import com.naturalist.zone.ZoneName;
import com.naturalist.zone.subzone.SubZoneName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PlantingTest {

    private static final Observer observer = Observer.forClass(PlantingTest.class);

    private static final LocalDate PLANTED = LocalDate.of(2026, 4, 6);
    private static final LocalDate PULLED = LocalDate.of(2026, 8, 10);

    private static Planting tomato(LocalDate removed, SubZoneName subZone, Integer count) {
        return new Planting(PlantingId.create(), PlantSpeciesName.of("solanum-lycopersicum"),
                CultivarName.of("amish-paste"), ZoneName.of("backyard"), subZone, count,
                PLANTED, removed, null);
    }

    @Test
    void aFinishedPlantingHasNoViolations() {
        var mo = observer.forMethod("aFinishedPlantingHasNoViolations");

        InvariantObservation result = mo.observable(
                tomato(PULLED, SubZoneName.of("backyard-south"), 12), "planting");

        assertThat(result.violations()).isEmpty();
    }

    /** Still growing, whole bed, uncounted — every nullable at once. */
    @Test
    void aBareActivePlantingHasNoViolations() {
        var mo = observer.forMethod("aBareActivePlantingHasNoViolations");

        InvariantObservation result = mo.observable(tomato(null, null, null), "planting");

        assertThat(result.violations()).isEmpty();
    }

    @Test
    void isActiveSpansThePlantedToRemovedWindowInclusively() {
        Planting finished = tomato(PULLED, null, 12);

        assertThat(finished.isActive(LocalDate.of(2026, 4, 5))).isFalse();  // day before planting
        assertThat(finished.isActive(PLANTED)).isTrue();
        assertThat(finished.isActive(LocalDate.of(2026, 6, 1))).isTrue();
        assertThat(finished.isActive(PULLED)).isTrue();                     // pulled that day
        assertThat(finished.isActive(LocalDate.of(2026, 8, 11))).isFalse();
    }

    @Test
    void aPlantingWithNoRemovedDateIsStillGrowing() {
        Planting active = tomato(null, null, 6);

        assertThat(active.isActive(LocalDate.of(2027, 1, 1))).isTrue();
    }

    /** A bed-wide planting names no sub-zone; a row-scoped one does. */
    @Test
    void zoneScopingMirrorsSoilProfileNullability() {
        assertThat(tomato(null, null, 1).isZoneScoped()).isTrue();
        assertThat(tomato(null, SubZoneName.of("backyard-north"), 1).isZoneScoped()).isFalse();
    }

    @Test
    void aPlantingRemovedBeforeItWasPlantedIsRejected() {
        var mo = observer.forMethod("aPlantingRemovedBeforeItWasPlantedIsRejected");
        var backwards = new Planting(PlantingId.create(), PlantSpeciesName.of("solanum-lycopersicum"),
                null, ZoneName.of("backyard"), null, null, PULLED, PLANTED, null);

        InvariantObservation result = mo.observable(backwards, "planting");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .containsExactly(".planting.removedNotBeforePlanted");
    }

    @Test
    void aPlantingOfNoPlantsIsRejected() {
        var mo = observer.forMethod("aPlantingOfNoPlantsIsRejected");

        InvariantObservation result = mo.observable(tomato(null, null, 0), "planting");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .containsExactly(".planting.plantCount");
    }

    /** Sown from a mixed packet: the species is known and the variety never was. */
    @Test
    void anUnrecordedVarietyIsValidButKnowable() {
        Planting anonymous = new Planting(PlantingId.create(), PlantSpeciesName.of("raphanus-sativus"),
                null, ZoneName.of("box-1"), null, null, PLANTED, null, null);

        assertThat(anonymous.isVarietyKnown()).isFalse();
        assertThat(tomato(null, null, 1).isVarietyKnown()).isTrue();
    }

    /**
     * A planting naming neither a plant nor a variety records only that something was put
     * somewhere, which no consumer can use.
     */
    @Test
    void aPlantingNamingNeitherPlantNorVarietyIsRejected() {
        var mo = observer.forMethod("aPlantingNamingNeitherPlantNorVarietyIsRejected");
        var nothing = new Planting(PlantingId.create(), null, null, ZoneName.of("box-1"),
                null, null, PLANTED, null, null);

        InvariantObservation result = mo.observable(nothing, "planting");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .containsExactly(".planting.plantOrCultivarKnown");
    }

    /** Knowing only the variety is enough — plants can resolve its species. */
    @Test
    void aPlantingNamingOnlyTheVarietyIsValid() {
        var mo = observer.forMethod("aPlantingNamingOnlyTheVarietyIsValid");
        var varietyOnly = new Planting(PlantingId.create(), null,
                CultivarName.of("amish-paste"), ZoneName.of("backyard"), null, null,
                PLANTED, null, null);

        assertThat(mo.observable(varietyOnly, "planting").violations()).isEmpty();
    }
}
