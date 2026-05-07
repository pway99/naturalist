package com.naturalist.plants.phytochemistry;

import com.naturalist.data.ForeignKeyConstraint;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.TestEntitySource;
import com.naturalist.plants.PlantTestEntitySource;

import java.util.List;

/**
 * Loads the phytochemistry catalog from JSON resources at test time.
 * <p>
 * The catalog records plant-compound links — one entry per
 * (plant, compound, role-context) tuple — and grows as new plant
 * constituents are documented. There are no secondary {@code @UniqueValue}
 * or {@code @EntityIdentifier} components on
 * {@link PhytochemicalConstituent}, so the canonical
 * {@code name()}-uniqueness check inherited from
 * {@link TestEntitySource} is the only constraint.
 * <p>
 * Catalog entries reference {@code plantName} (intra-domain soft FK,
 * enforced declaratively below) and {@code compoundName} (cross-domain
 * soft FK to the chemistry catalog). The cross-domain reference cannot
 * be declared as a {@link ForeignKeyConstraint} — resolution would have
 * to cross the {@code <domain>-repository-test} module boundary, which
 * the project's DAG forbids. Cross-aggregate referential integrity for
 * the referenced compound remains a service-layer rule, not a record
 * invariant.
 */
public class PhytochemicalConstituentTestEntitySource
        extends TestEntitySource<PhytochemicalConstituentName, PhytochemicalConstituent> {

    public PhytochemicalConstituentTestEntitySource(NaturalistDatabase database) {
        super(database);
        loadFile("plants/phytochemistry/phytochemical-constituents.json");
    }

    @Override
    protected List<ForeignKeyConstraint<PhytochemicalConstituent, ?>> foreignKeyConstraints() {
        return List.of(ForeignKeyConstraint.of(
                "plantName",
                PhytochemicalConstituent::plantName,
                PlantTestEntitySource.class));
    }
}
