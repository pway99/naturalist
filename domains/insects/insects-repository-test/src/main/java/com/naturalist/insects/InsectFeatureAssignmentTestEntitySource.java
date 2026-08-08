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

    /**
     * Enables {@code save()} to reconcile a unique-constraint match on
     * {@code featureId+rankName}: the existing row's id is retained (its
     * {@code ordinal} is overwritten with the incoming value), the
     * caller-supplied id is discarded. A re-identification that reassigns the
     * same feature (its id resolved to the existing {@code InsectFeature} row
     * by {@code save()}'s own unique-constraint reconciliation, not by any
     * deduplication performed here) to the same rank hits this branch instead
     * of tripping the constraint — see {@code InsectCatalogIdentificationTransaction}.
     */
    @Override
    protected InsectFeatureAssignment withKey(InsectFeatureAssignment a, InsectFeatureAssignmentId key) {
        return new InsectFeatureAssignment(key, a.featureId(), a.rankName(), a.ordinal());
    }
}
