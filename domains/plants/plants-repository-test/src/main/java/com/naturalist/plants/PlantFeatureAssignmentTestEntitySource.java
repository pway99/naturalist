package com.naturalist.plants;

import com.naturalist.data.ForeignKeyConstraint;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.TestEntitySource;
import com.naturalist.data.UniqueConstraint;

import java.util.List;
import java.util.function.Function;

/**
 * Mirrors {@code InsectFeatureAssignmentTestEntitySource} — {@link PlantFeatureAssignment}
 * carries surrogate ({@link PlantFeatureAssignmentId}) identity, so this extends
 * {@link TestEntitySource} directly rather than the natural-key {@code NamedTestEntitySource}.
 * Only four rank foreign keys — plants has no {@code PlantSubspeciesName}.
 */
public class PlantFeatureAssignmentTestEntitySource
        extends TestEntitySource<PlantFeatureAssignmentId, PlantFeatureAssignment> {

    public PlantFeatureAssignmentTestEntitySource(NaturalistDatabase database) {
        super(database);
        loadFile("plants/plant-feature-assignments.json");
    }

    @Override
    protected List<UniqueConstraint<PlantFeatureAssignment>> uniqueConstraints() {
        return List.of(
                new UniqueConstraint<>() {
                    @Override
                    public String name() {
                        return "featureId+rankName";
                    }

                    @Override
                    public Function<PlantFeatureAssignment, ?> valueFunction() {
                        return a -> a.featureId().value() + ":"
                                + a.rankName().value();
                    }
                });
    }

    @Override
    protected List<ForeignKeyConstraint<PlantFeatureAssignment, ?>> foreignKeyConstraints() {
        return List.of(
                ForeignKeyConstraint.of(
                        "featureId",
                        PlantFeatureAssignment::featureId,
                        PlantFeatureTestEntitySource.class),
                ForeignKeyConstraint.of(
                        "rankName (order)",
                        a -> a.rankName() instanceof PlantOrderName o ? o : null,
                        PlantOrderTestEntitySource.class),
                ForeignKeyConstraint.of(
                        "rankName (family)",
                        a -> a.rankName() instanceof PlantFamilyName f ? f : null,
                        PlantFamilyTestEntitySource.class),
                ForeignKeyConstraint.of(
                        "rankName (genus)",
                        a -> a.rankName() instanceof PlantGenusName g ? g : null,
                        PlantGenusTestEntitySource.class),
                ForeignKeyConstraint.of(
                        "rankName (species)",
                        a -> a.rankName() instanceof PlantSpeciesName s ? s : null,
                        PlantSpeciesTestEntitySource.class));
    }

    /**
     * Enables {@code save()} to reconcile a unique-constraint match on
     * {@code featureId+rankName}: the existing row's id is retained (its
     * {@code ordinal} is overwritten with the incoming value), the
     * caller-supplied id is discarded. Mirrors {@code InsectFeatureAssignmentTestEntitySource}.
     */
    @Override
    protected PlantFeatureAssignment withKey(PlantFeatureAssignment a, PlantFeatureAssignmentId key) {
        return new PlantFeatureAssignment(key, a.featureId(), a.rankName(), a.ordinal());
    }
}
