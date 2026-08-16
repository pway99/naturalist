package com.naturalist.plants.phytochemistry;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.TestEntitySource;

/**
 * Loads the phytochemistry catalog from JSON resources at test time.
 * <p>
 * The catalog records plant-compound links — one entry per
 * (plant, compound, role-context) tuple — and grows as new plant
 * constituents are documented. There are no secondary {@code @UniqueValue}
 * or {@code @EntityIdentifier} components on {@link PhytochemicalConstituent},
 * so the canonical {@code name()}-uniqueness check inherited from
 * {@link TestEntitySource} is the only structural constraint.
 * <p>
 * Two soft foreign keys go undeclared here, for two different reasons:
 * <ul>
 *   <li>{@code plantName} is a {@link com.naturalist.plants.PlantRankName} — a
 *       constituent can be recorded for a genus (thymol against {@code thymus}) as
 *       readily as a species — and a {@link com.naturalist.data.ForeignKeyConstraint}
 *       resolves a single source class, while this spans the rank chain.
 *       {@code PhytochemicalConstituentCatalogDataTest} covers it instead.</li>
 *   <li>{@code compoundName} is a cross-domain reference to the chemistry catalog;
 *       resolving it would cross the {@code <domain>-repository-test} module boundary,
 *       which the DAG forbids. It stays a service-layer rule.</li>
 * </ul>
 */
public class PhytochemicalConstituentTestEntitySource
        extends TestEntitySource<PhytochemicalConstituentName, PhytochemicalConstituent> {

    public PhytochemicalConstituentTestEntitySource(NaturalistDatabase database) {
        super(database);
        loadFile("plants/phytochemistry/phytochemical-constituents.json");
    }
}
