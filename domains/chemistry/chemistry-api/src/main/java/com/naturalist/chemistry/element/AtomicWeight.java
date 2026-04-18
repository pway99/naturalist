package com.naturalist.chemistry.element;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.NumericNamedValue;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * The standard atomic weight of an element in grams per mole (g/mol), per IUPAC 2021 values.
 * <p>
 * Distinct from {@link com.naturalist.chemistry.compound.MolecularWeight} — atomic weight is
 * a property of a single element; molecular weight is the molar mass of a compound.
 * Both share scale=4 and HALF_UP rounding as IUPAC significant-figure convention.
 * <p>
 * For synthetic elements with no stable isotopes, carries the mass number of the
 * longest-lived isotope.
 * <p>
 * Scale: 4 decimal places. Rounding: {@link RoundingMode#HALF_UP}.
 */
public record AtomicWeight(BigDecimal value) implements NumericNamedValue {

    @JsonCreator
    public static AtomicWeight of(BigDecimal value) {
        return new AtomicWeight(value);
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
