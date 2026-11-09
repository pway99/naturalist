package com.naturalist.observation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.Constraints;

import java.util.Locale;
import java.util.function.Consumer;

/**
 * A cell of a fixed lattice over the earth, 2.5 arc-minutes (1/24 degree) on a side, and the only
 * form of location the app keeps for an observation. A point is snapped to the cell that contains
 * it; the point itself is then forgotten, so a stored area says roughly where an observation was
 * made without saying exactly.
 *
 * <p>Cell {@code (latIndex, lonIndex)} spans latitudes {@code latIndex/24} to
 * {@code (latIndex+1)/24} and longitudes {@code lonIndex/24} to {@code (lonIndex+1)/24}. A cell
 * owns its south and west edges, so an exact boundary belongs to the cell it opens. Latitude 90
 * and longitude 180 have no cell to open and clamp to the last one.
 *
 * <p>The lattice nests inside the USGS 7.5-minute quadrangle grid: each quadrangle is exactly three
 * cells wide and three cells tall. That is a property of the lattice and nothing more; the app does
 * not name or look up quadrangles.
 *
 * <p>Like the other value objects here, the factories always construct and validity is a
 * property: an out-of-range pair is a cell whose {@link #isValid()} is false.
 */
public record GridCell(int latIndex, int lonIndex) implements ValueObject {

    /** Cells per degree: the lattice is 2.5 arc-minutes, so 60 / 2.5. */
    private static final int CELLS_PER_DEGREE = 24;

    private static final int MIN_LAT_INDEX = -90 * CELLS_PER_DEGREE;

    private static final int MAX_LAT_INDEX = 90 * CELLS_PER_DEGREE - 1;

    private static final int MIN_LON_INDEX = -180 * CELLS_PER_DEGREE;

    private static final int MAX_LON_INDEX = 180 * CELLS_PER_DEGREE - 1;

    /** The edges of a cell in decimal degrees. */
    public record Bounds(double south, double north, double west, double east) {
    }

    /** The cell that contains {@code c}; latitude 90 and longitude 180 clamp to the last cell. */
    public static GridCell of(Coordinates c) {
        int lat = (int) Math.floor(c.latitude() * CELLS_PER_DEGREE);
        int lon = (int) Math.floor(c.longitude() * CELLS_PER_DEGREE);
        return new GridCell(Math.min(lat, MAX_LAT_INDEX), Math.min(lon, MAX_LON_INDEX));
    }

    /** The cell with these indices, exactly as given. */
    public static GridCell of(int latIndex, int lonIndex) {
        return new GridCell(latIndex, lonIndex);
    }

    public Bounds bounds() {
        return new Bounds(
                (double) latIndex / CELLS_PER_DEGREE,
                (double) (latIndex + 1) / CELLS_PER_DEGREE,
                (double) lonIndex / CELLS_PER_DEGREE,
                (double) (lonIndex + 1) / CELLS_PER_DEGREE);
    }

    /**
     * The cell's extent in plain words, such as {@code 45.50–45.54°N, 122.71–122.67°W}: latitude
     * then longitude, each from its south or west edge to its north or east edge as absolute
     * values. The hemisphere letter follows the south or west edge, so a cell straddling the
     * equator reads {@code 0.04–0.00°S}.
     */
    public String describe() {
        Bounds b = bounds();
        return String.format(Locale.ROOT, "%.2f–%.2f°%s, %.2f–%.2f°%s",
                Math.abs(b.south()), Math.abs(b.north()), b.south() >= 0 ? "N" : "S",
                Math.abs(b.west()), Math.abs(b.east()), b.west() >= 0 ? "E" : "W");
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return c -> c
                .isTrue(latIndex >= MIN_LAT_INDEX && latIndex <= MAX_LAT_INDEX, "latIndex")
                .isTrue(lonIndex >= MIN_LON_INDEX && lonIndex <= MAX_LON_INDEX, "lonIndex");
    }

    /**
     * True when this cell satisfies its own {@link #invariants()}: both indices lie on the
     * lattice. The same conditions, as a plain predicate for callers that only need the verdict.
     * Not part of the JSON form: the two indices say it all.
     */
    @JsonIgnore
    public boolean isValid() {
        return latIndex >= MIN_LAT_INDEX && latIndex <= MAX_LAT_INDEX
                && lonIndex >= MIN_LON_INDEX && lonIndex <= MAX_LON_INDEX;
    }
}
