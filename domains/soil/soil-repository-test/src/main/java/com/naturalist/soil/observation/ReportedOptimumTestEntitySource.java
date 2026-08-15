package com.naturalist.soil.observation;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.TestEntitySource;
import com.naturalist.data.UniqueConstraint;

import java.util.List;
import java.util.function.Function;

/**
 * Test data source for {@link ReportedOptimum} — the optimum ranges printed on the March 2026 FGL
 * reports, one row per nutrient per analysis, transcribed from the "Optimum Range" column of
 * CH 2671853-001 (Box 1) and -002 (back yard). Seventeen per analysis, thirty-four in all.
 * <p>
 * These are the golden master: they are what the lab actually said, and a strategy that claims to
 * reproduce FGL's reasoning is checked against them. Do not regenerate them from a formula — that
 * would make the test assert the formula against itself. Backing catalog:
 * {@code soil/observation/reported-optimum.json}.
 */
public class ReportedOptimumTestEntitySource extends TestEntitySource<ReportedOptimumId, ReportedOptimum> {

    public ReportedOptimumTestEntitySource(NaturalistDatabase database) {
        super(database);
        loadFile("soil/observation/reported-optimum.json");
    }

    @Override
    protected List<UniqueConstraint<ReportedOptimum>> uniqueConstraints() {
        return List.of(new UniqueConstraint<>() {
            @Override
            public String name() {
                return "nutrientName+labAnalysisId";
            }

            @Override
            public Function<ReportedOptimum, ?> valueFunction() {
                return optimum -> optimum.nutrientName().value() + ":" + optimum.labAnalysisId().value();
            }
        });
    }
}
