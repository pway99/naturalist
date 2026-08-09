package com.naturalist.soil.observation;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.TestEntitySource;
import com.naturalist.data.UniqueConstraint;

import java.util.List;
import java.util.function.Function;

/**
 * Test data source for {@link SoilPhysicalCharacteristics} — one row per Oak Vista analysis with
 * the real March 2026 FGL physical/derived values. Backing catalog:
 * {@code soil/observation/soil-physical-characteristics.json}.
 */
public class SoilPhysicalCharacteristicsTestEntitySource
        extends TestEntitySource<SoilPhysicalCharacteristicsId, SoilPhysicalCharacteristics> {

    public SoilPhysicalCharacteristicsTestEntitySource(NaturalistDatabase database) {
        super(database);
        loadFile("soil/observation/soil-physical-characteristics.json");
    }

    @Override
    protected List<UniqueConstraint<SoilPhysicalCharacteristics>> uniqueConstraints() {
        return List.of(new UniqueConstraint<>() {
            @Override
            public String name() {
                return "labAnalysisId";
            }

            @Override
            public Function<SoilPhysicalCharacteristics, ?> valueFunction() {
                return characteristics -> characteristics.labAnalysisId().value();
            }
        });
    }
}
