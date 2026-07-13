package com.naturalist.insects;

import com.naturalist.data.ForeignKeyConstraint;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.TestEntitySource;

import java.util.List;

public class FieldObservationTestEntitySource
        extends TestEntitySource<FieldObservationId, FieldObservation> {

    public FieldObservationTestEntitySource(NaturalistDatabase database) {
        super(database);
        loadFile("insects/field-observations.json");
    }

    @Override
    protected List<ForeignKeyConstraint<FieldObservation, ?>> foreignKeyConstraints() {
        return List.of(
                ForeignKeyConstraint.of(
                        "subject (family)",
                        o -> o.subject() instanceof InsectFamilyName f ? f : null,
                        InsectFamilyTestEntitySource.class),
                ForeignKeyConstraint.of(
                        "subject (genus)",
                        o -> o.subject() instanceof InsectGenusName g ? g : null,
                        InsectGenusTestEntitySource.class),
                ForeignKeyConstraint.of(
                        "subject (species)",
                        o -> o.subject() instanceof InsectSpeciesName s ? s : null,
                        InsectSpeciesTestEntitySource.class));
    }
}
