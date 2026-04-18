package com.naturalist.chemistry.compound;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.NumericNamedValue;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * The molar mass of a compound in grams per mole (g/mol).
 * <p>
 * Scale of 4 reflects IUPAC significant-figure conventions for molecular weights.
 * Nullable on {@link CompoundInfo} — biological complexes and chelate categories
 * do not have a defined molar mass.
 * <p>
 * Scale: 4 decimal places. Rounding: {@link RoundingMode#HALF_UP}.
 */
public record MolecularWeight(BigDecimal value) implements NumericNamedValue {

    @JsonCreator
    public static MolecularWeight of(BigDecimal value) {
        return new MolecularWeight(value);
    }

    @Override
    public int scale() {
        return 4;
    }

    @Override
    public RoundingMode roundingMode() {
        return RoundingMode.HALF_UP;
    }

    @Override
    public boolean isValid() {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }
}
