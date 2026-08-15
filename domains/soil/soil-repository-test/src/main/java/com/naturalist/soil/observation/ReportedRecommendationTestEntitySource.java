package com.naturalist.soil.observation;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.TestEntitySource;
import com.naturalist.data.UniqueConstraint;

import java.util.List;
import java.util.function.Function;

/**
 * Test data source for {@link ReportedRecommendation} — what FGL advised on the March 2026
 * reports, transcribed from the Fertilization Recommendations table and the requirements block of
 * CH 2671853-001 (Box 1) and -002 (back yard). Fourteen rows per analysis, twenty-eight in all.
 * <p>
 * Seventeen of the twenty-eight are {@code None}, which is the point: the lab said "apply none of
 * this" seventeen times, and those rows are as real as the nine that carry a quantity. Backing
 * catalog: {@code soil/observation/reported-recommendation.json}.
 */
public class ReportedRecommendationTestEntitySource
        extends TestEntitySource<ReportedRecommendationId, ReportedRecommendation> {

    public ReportedRecommendationTestEntitySource(NaturalistDatabase database) {
        super(database);
        loadFile("soil/observation/reported-recommendation.json");
    }

    @Override
    protected List<UniqueConstraint<ReportedRecommendation>> uniqueConstraints() {
        return List.of(new UniqueConstraint<>() {
            @Override
            public String name() {
                return "inputName+labAnalysisId";
            }

            @Override
            public Function<ReportedRecommendation, ?> valueFunction() {
                return r -> r.inputName().value() + ":" + r.labAnalysisId().value();
            }
        });
    }
}
