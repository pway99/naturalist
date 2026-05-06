package com.naturalist.soil.observation;

import com.naturalist.ddd.ValueObject;
import com.naturalist.measurements.ElectricalConductivity;
import com.naturalist.observability.Constraints;
import com.naturalist.soil.SoilPH;

import java.util.function.Consumer;

/**
 * The complete nutrient chemistry panel from a single soil laboratory analysis.
 * <p>
 * {@code NutrientPanel} is an immutable snapshot of the soil's chemical state at a
 * point in time, as measured and reported by the Fruit Growers Laboratory (FGL).
 * It is owned by {@link LabAnalysis} and does not exist independently.
 * <p>
 * <b>Measurement framework:</b> FGL distinguishes two extraction fractions for most
 * macro-cations:
 * <ul>
 *   <li><b>Exchangeable (Exch)</b> — cations bound to clay and organic matter exchange
 *       sites; the medium-term reservoir.</li>
 *   <li><b>Soluble (Sol)</b> — cations in soil solution at the time of extraction;
 *       the fraction immediately available to plant roots.</li>
 * </ul>
 * <p>
 * <b>Oak Vista reference values — Box 1 (CH 2671853-001, sampled March 3, 2026):</b>
 * <pre>
 *   N-Nitrate:     1.36 lbs/1000sqft  (VERY_LOW; optimum 5.3–7.2)
 *   P-P2O5:       39.6               (VERY_HIGH; optimum 10–12)
 *   K-Sol:         1.31              (VERY_LOW; optimum 10–19)
 *   Ca-Sol:        6.99              (VERY_LOW; optimum 16–26)
 *   Mg-Sol:        2.29              (LOW; optimum 5.6–9.0)
 *   Sulfate:       2.73              (VERY_LOW; optimum 18–110)
 *   Boron:         0.0202            (VERY_LOW; optimum 0.072–0.18)
 *   pH:            7.2               (OPTIMAL)
 *   Limestone:     1.7%              (MODERATE PROBLEM)
 * </pre>
 */
public record NutrientPanel(
        NutrientReading nitrateN,
        NutrientReading phosphorusP2O5,
        NutrientReading potassiumExch,
        NutrientReading potassiumSoluble,
        NutrientReading calciumExch,
        NutrientReading calciumSoluble,
        NutrientReading magnesiumSoluble,
        NutrientReading sulfate,
        NutrientReading boron,
        CecMeqPer100g cecMeqPer100g,
        SoilPH pH,
        ElectricalConductivity ecDsPerMeter,
        LimestonePct limestonePct,
        SaturationPct saturationPct
) implements ValueObject {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .valueObject(nitrateN, "nitrateN")
                .valueObject(phosphorusP2O5, "phosphorusP2O5")
                .valueObject(potassiumExch, "potassiumExch")
                .valueObject(potassiumSoluble, "potassiumSoluble")
                .valueObject(calciumExch, "calciumExch")
                .valueObject(calciumSoluble, "calciumSoluble")
                .valueObject(magnesiumSoluble, "magnesiumSoluble")
                .valueObject(sulfate, "sulfate")
                .valueObject(boron, "boron")
                .namedValue(cecMeqPer100g, "cecMeqPer100g")
                .namedValue(pH, "pH")
                .namedValue(ecDsPerMeter, "ecDsPerMeter")
                .namedValue(limestonePct, "limestonePct")
                .namedValue(saturationPct, "saturationPct");
    }
}
