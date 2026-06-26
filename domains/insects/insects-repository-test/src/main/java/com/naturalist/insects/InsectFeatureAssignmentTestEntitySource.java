package com.naturalist.insects;

import com.naturalist.data.ForeignKeyConstraint;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.TestEntitySource;
import com.naturalist.data.UniqueConstraint;

import java.util.List;
import java.util.function.Function;

public class InsectFeatureAssignmentTestEntitySource
        extends TestEntitySource<InsectFeatureAssignmentId, InsectFeatureAssignment> {

    public InsectFeatureAssignmentTestEntitySource(NaturalistDatabase database) {
        super(database);
        loadFile("insects/insect-feature-assignments.json");
    }

    @Override
    protected List<UniqueConstraint<InsectFeatureAssignment>> uniqueConstraints() {
        return List.of(
                new UniqueConstraint<>() {
                    @Override
                    public String name() {
                        return "featureId+rankName";
                    }

                    @Override
                    public Function<InsectFeatureAssignment, ?> valueFunction() {
                        return a -> a.featureId().value() + ":"
                                + a.rankName().value();
                    }
                });
    }

    @Override
    protected List<ForeignKeyConstraint<InsectFeatureAssignment, ?>> foreignKeyConstraints() {
        return List.of(
                ForeignKeyConstraint.of(
                        "featureId",
                        InsectFeatureAssignment::featureId,
                        InsectFeatureTestEntitySource.class),
                ForeignKeyConstraint.of(
                        "rankName (order)",
                        a -> a.rankName() instanceof InsectOrderName o ? o : null,
                        InsectOrderTestEntitySource.class),
                ForeignKeyConstraint.of(
                        "rankName (family)",
                        a -> a.rankName() instanceof InsectFamilyName f ? f : null,
                        InsectFamilyTestEntitySource.class),
                ForeignKeyConstraint.of(
                        "rankName (genus)",
                        a -> a.rankName() instanceof InsectGenusName g ? g : null,
                        InsectGenusTestEntitySource.class),
                ForeignKeyConstraint.of(
                        "rankName (species)",
                        a -> a.rankName() instanceof InsectSpeciesName s ? s : null,
                        InsectSpeciesTestEntitySource.class));
    }
}
