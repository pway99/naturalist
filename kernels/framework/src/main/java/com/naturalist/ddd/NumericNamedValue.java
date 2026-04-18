package com.naturalist.ddd;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * A {@link NamedValue} specialisation for decimal domain quantities.
 * <p>
 * Every decimal domain field that carries a named concept (temperature, molecular weight,
 * soil pH, solubility) must use a {@code NumericNamedValue} implementation rather than
 * plain {@code BigDecimal}. The scale and rounding mode are domain facts encoded in the
 * type, not runtime configuration scattered at call sites.
 *
 * <h2>Contract</h2>
 * <ul>
 *   <li>{@link #scale()} — the number of decimal places this quantity is meaningful to.
 *       Declared as a constant on the concrete type; never passed as an argument.</li>
 *   <li>{@link #roundingMode()} — the rounding strategy; typically {@link RoundingMode#HALF_UP}
 *       for all scientific measurement contexts in this application.</li>
 *   <li>{@link #normalized()} — returns {@link #value()} rescaled to {@link #scale()} with
 *       {@link #roundingMode()}. Use this whenever the value enters arithmetic or comparison.</li>
 * </ul>
 *
 * <h2>Integer quantities</h2>
 * Domain values that are whole numbers by scientific definition (e.g. atomic number)
 * use {@link NamedValue}{@code <Integer>} directly. Applying {@code NumericNamedValue}
 * with {@code scale=0} to an inherently integer quantity obscures the domain fact
 * that the quantity is integral.
 *
 * <h2>Enum fields</h2>
 * Enum types cannot implement this interface. An enum carrying a decimal quantity
 * (e.g. {@code PeriodicElement#atomicWeight}) declares private {@code SCALE} and
 * {@code ROUNDING_MODE} constants and normalises via {@code BigDecimal.valueOf(double).setScale(SCALE, ROUNDING_MODE)}
 * in its constructor. The resulting field is {@code BigDecimal}, not a {@code NumericNamedValue}.
 *
 * <h2>Implementation pattern</h2>
 * <pre>{@code
 * public record MolecularWeight(BigDecimal value) implements NumericNamedValue {
 *
 *     @JsonCreator
 *     public static MolecularWeight of(BigDecimal value) {
 *         return new MolecularWeight(value);
 *     }
 *
 *     @Override public int scale()                 { return 4; }
 *     @Override public RoundingMode roundingMode() { return RoundingMode.HALF_UP; }
 *     @Override public boolean isValid()           { return value != null && value.compareTo(BigDecimal.ZERO) > 0; }
 * }
 * }</pre>
 *
 * @see NamedValue
 * @see NamedValueConstraints
 */
public interface NumericNamedValue extends NamedValue<BigDecimal> {

    /**
     * The number of decimal places this quantity is meaningful to.
     * Declared as a domain fact on the concrete type.
     */
    int scale();

    /**
     * The rounding mode applied when normalising this quantity.
     * Typically {@link RoundingMode#HALF_UP} for all measurements in this application.
     */
    RoundingMode roundingMode();

    /**
     * Returns {@link #value()} rescaled to {@link #scale()} with {@link #roundingMode()}.
     * Null-safe: returns {@code null} if {@link #value()} is {@code null}.
     */
    default BigDecimal normalized() {
        BigDecimal v = value();
        return v == null ? null : v.setScale(scale(), roundingMode());
    }
}
