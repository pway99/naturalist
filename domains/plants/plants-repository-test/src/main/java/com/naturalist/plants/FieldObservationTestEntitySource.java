package com.naturalist.plants;

import com.naturalist.data.ForeignKeyConstraint;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.TestEntitySource;

import java.util.List;

/**
 * Plant field observations. The {@code subject} is a {@link PlantRankName}, so referential
 * integrity is declared per-rank: each constraint extracts the subject only when it is that
 * rank (null otherwise, which the FK enforcer treats as a pass), so a species-rank subject
 * is checked against the species source, a genus subject against the genus source, and so on.
 */
public class FieldObservationTestEntitySource
        extends TestEntitySource<FieldObservationId, FieldObservation> {

    public FieldObservationTestEntitySource(NaturalistDatabase database) {
        super(database);
        loadFile("plants/field-observations.json");
    }

    @Override
    protected List<ForeignKeyConstraint<FieldObservation, ?>> foreignKeyConstraints() {
        return List.of(
                ForeignKeyConstraint.of("subject (order)",
                        o -> o.subject() instanceof PlantOrderName x ? x : null,
                        PlantOrderTestEntitySource.class),
                ForeignKeyConstraint.of("subject (family)",
                        o -> o.subject() instanceof PlantFamilyName x ? x : null,
                        PlantFamilyTestEntitySource.class),
                ForeignKeyConstraint.of("subject (genus)",
                        o -> o.subject() instanceof PlantGenusName x ? x : null,
                        PlantGenusTestEntitySource.class),
                ForeignKeyConstraint.of("subject (species)",
                        o -> o.subject() instanceof PlantSpeciesName x ? x : null,
                        PlantSpeciesTestEntitySource.class));
    }
}
