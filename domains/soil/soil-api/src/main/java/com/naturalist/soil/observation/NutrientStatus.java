package com.naturalist.soil.observation;

/**
 * The agronomic status of a soil nutrient measurement relative to the optimum range
 * defined by the Fruit Growers Laboratory (FGL) for the specific crop context.
 * <p>
 * {@code NutrientStatus} drives amendment recommendations and identifies limiting factors
 * in the soil system. Status is assigned at analysis time by comparing the measured value
 * against the FGL-published optimum ranges for each nutrient.
 * <p>
 * <b>Oak Vista status as of March 2026 (FGL report CH 2671853):</b>
 * <ul>
 *   <li><b>VERY_LOW</b>: N-Nitrate (both beds), K-Sol, Ca-Sol, Mg-Sol, Sulfate, Boron</li>
 *   <li><b>HIGH or VERY_HIGH</b>: P-P2O5 (both beds; zero inputs — legacy accumulation)</li>
 *   <li><b>SATISFACTORY</b>: K-Exch, Ca-Exch, pH, EC</li>
 * </ul>
 * <p>
 * The soluble fractions (K-Sol, Ca-Sol, Mg-Sol) are uniformly deficient while their
 * exchangeable counterparts are at or above optimum — a consistent pattern indicating
 * that cation exchange sites are occupied but plant-available soluble fractions are
 * not being released at the rate crops require during active growth.
 */
public enum NutrientStatus {

    /**
     * Measured value is well below the low end of the optimum range.
     * <p>
     * Corrective input is urgent. VERY_LOW nutrients are limiting factors —
     * their deficiency constrains plant growth regardless of the status of
     * other nutrients in the system.
     * <p>
     * Oak Vista examples: N-Nitrate Box 1 (1.36 vs 5.3–7.2 lbs/1000sqft optimum),
     * Ca-Sol Box 1 (6.99 vs 16–26 optimum), Boron Box 1 (0.0202 vs 0.072–0.18 optimum).
     */
    VERY_LOW,

    /**
     * Measured value is below the optimum range but not critically so.
     * <p>
     * Input is recommended; deficiency is present but not yet fully limiting.
     * Close monitoring of crop symptoms warranted.
     * <p>
     * Oak Vista examples: Mg-Sol backyard (2.44 vs 2.8–6.2 optimum, ~87% of low end).
     */
    LOW,

    /**
     * Measured value falls within the optimum range for this nutrient and crop context.
     * <p>
     * No corrective input required. Standard maintenance applications sufficient.
     * <p>
     * Oak Vista examples: K-Exch (K2O) both beds, Ca-Exch both beds, pH (7.2), EC.
     */
    SATISFACTORY,

    /**
     * Measured value exceeds the optimum range but not to a degree requiring remediation.
     * <p>
     * Monitor for antagonism with other nutrients. Withhold further inputs of this nutrient.
     */
    HIGH,

    /**
     * Measured value substantially exceeds the optimum range.
     * <p>
     * May cause antagonistic suppression of other nutrients (e.g., very high P can suppress
     * Zn uptake). Zero inputs of this nutrient until levels decline.
     * <p>
     * Oak Vista examples: P-P2O5 Box 1 (39.6 vs 10–12 optimum),
     * P-P2O5 Backyard (43.3 vs 8.4–10 optimum). Neither bed has received phosphorus
     * inputs — the elevation is a legacy accumulation likely from prior organic amendments.
     */
    VERY_HIGH
}
