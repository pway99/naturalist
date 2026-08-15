package com.naturalist.soil.observation;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.TestEntitySource;
import com.naturalist.data.UniqueConstraint;

import java.util.List;
import java.util.function.Function;

/**
 * Test data source for {@link NutrientReading} — the real March 2026 FGL nutrient values, one row
 * per nutrient per analysis: seventeen for Box 1's -001 panel and seventeen for the back yard's
 * -002 panel. Backing catalog: {@code soil/observation/nutrient-reading.json}.
 */
public class NutrientReadingTestEntitySource extends TestEntitySource<NutrientReadingId, NutrientReading> {

    public NutrientReadingTestEntitySource(NaturalistDatabase database) {
        super(database);
        loadFile("soil/observation/nutrient-reading.json");
    }

    @Override
    protected List<UniqueConstraint<NutrientReading>> uniqueConstraints() {
        return List.of(new UniqueConstraint<>() {
            @Override
            public String name() {
                return "nutrientName+labAnalysisId";
            }

            @Override
            public Function<NutrientReading, ?> valueFunction() {
                return reading -> reading.nutrientName().value() + ":" + reading.labAnalysisId().value();
            }
        });
    }
}
