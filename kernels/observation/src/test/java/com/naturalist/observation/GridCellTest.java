package com.naturalist.observation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.naturalist.observability.InvariantObservation;
import com.naturalist.observability.Observer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class GridCellTest {
    private static final Observer observer = Observer.forClass(GridCellTest.class);

    @Test
    void portland_snapsToCell1092_minus2945() {
        var cell = GridCell.of(new Coordinates(45.52, -122.68));
        assertThat(cell).isEqualTo(GridCell.of(1092, -2945));
        assertThat(cell.bounds().south()).isCloseTo(45.5, within(1e-9));
        assertThat(cell.bounds().north()).isCloseTo(45.5 + 1.0 / 24, within(1e-9));
        assertThat(cell.bounds().west()).isCloseTo(-2945.0 / 24, within(1e-9));
        assertThat(cell.bounds().east()).isCloseTo(-2944.0 / 24, within(1e-9));
        assertThat(cell.describe()).isEqualTo("45.50–45.54°N, 122.71–122.67°W");
    }

    @Test
    void southernWestern_pointHasNegativeIndices_andSWLetters() {
        var sydney = GridCell.of(new Coordinates(-33.87, 151.21));
        assertThat(sydney).isEqualTo(GridCell.of(-813, 3629));
        assertThat(sydney.describe()).isEqualTo("33.88–33.83°S, 151.21–151.25°E");

        var rio = GridCell.of(new Coordinates(-22.9, -43.2));
        assertThat(rio).isEqualTo(GridCell.of(-550, -1037));
        assertThat(rio.describe()).isEqualTo("22.92–22.88°S, 43.21–43.17°W");
    }

    @Test
    void exactBoundary_belongsToTheCellItOpens() {
        assertThat(GridCell.of(new Coordinates(45.5, -122.75))).isEqualTo(GridCell.of(1092, -2946));
    }

    @Test
    void poles_andAntimeridian_clampToTheLastCell() {
        assertThat(GridCell.of(new Coordinates(90, 180))).isEqualTo(GridCell.of(2159, 4319));
        assertThat(GridCell.of(new Coordinates(-90, -180))).isEqualTo(GridCell.of(-2160, -4320));
    }

    @Test
    void equatorStraddle_describesHemisphereBySouthEdge() {
        assertThat(GridCell.of(-1, 0).describe()).startsWith("0.04–0.00°S, 0.00–0.04°E");
    }

    @Test
    void outOfRangeIndices_construct_butAreNotValid() {
        assertThat(GridCell.of(2160, 0).isValid()).isFalse();
        assertThat(GridCell.of(0, -4321).isValid()).isFalse();
        assertThat(GridCell.of(0, 0).isValid()).isTrue();

        var mo = observer.forMethod("outOfRangeIndices_construct_butAreNotValid");
        InvariantObservation result = mo.observable(GridCell.of(2160, -4321), "cell");
        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .containsExactlyInAnyOrder(".cell.latIndex", ".cell.lonIndex");
    }

    @Test
    void coordinates_outOfRange_failInvariants() {
        var mo = observer.forMethod("coordinates_outOfRange_failInvariants");
        assertThat(mo.observable(new Coordinates(91, 0), "point").violationNamesRemovingPrefix(mo.observationPoint()))
                .containsExactlyInAnyOrder(".point.latitude");
        assertThat(mo.observable(new Coordinates(0, -181), "point").violationNamesRemovingPrefix(mo.observationPoint()))
                .containsExactlyInAnyOrder(".point.longitude");
        assertThat(mo.observable(new Coordinates(45.52, -122.68), "point").violations()).isEmpty();
    }

    @Test
    void gridCell_roundTripsThroughJackson() throws Exception {
        var mapper = new ObjectMapper();
        var cell = GridCell.of(1092, -2945);
        var json = mapper.writeValueAsString(cell);
        assertThat(mapper.readValue(json, GridCell.class)).isEqualTo(cell);
    }
}
