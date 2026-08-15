package com.naturalist.soil.observation;

/**
 * The unit a {@link NutrientReading}'s value is expressed in, making each reading self-describing
 * (the monitoring series and other labs need it — a bare number is ambiguous). Physical
 * characteristics do not carry a unit: their values are typed ({@code ElectricalConductivity} is
 * dS/m, {@code CecMeqPer100g} is meq/100g, …), so the unit is intrinsic to the type.
 */
public enum MeasurementUnit {

    /** Pounds per 1000 square feet — the unit FGL reports soil nutrients in. */
    LBS_PER_1000_SQFT("lbs/1000 ft²"),

    /**
     * Tons per acre-foot — the unit FGL states lime and gypsum <em>requirements</em> in, as
     * distinct from the fertilisation table's lbs/1000 ft². Both appear on the same report page,
     * which is why a recommendation row carries its unit rather than assuming one.
     */
    TONS_PER_ACRE_FOOT("tons/AF");

    private final String symbol;

    MeasurementUnit(String symbol) {
        this.symbol = symbol;
    }

    public String symbol() {
        return symbol;
    }
}
