package com.naturalist.plants.phytochemistry;

import com.naturalist.data.TestEntitySource;

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
 * Catalog entries reference {@code plantName} (intra-domain soft FK) and
 * {@code compoundName} (cross-domain soft FK to the chemistry catalog).
 * Cross-aggregate referential integrity — the referenced compound exists
 * in {@code compounds.json} — is a service-layer rule, not a record
 * invariant.
 */
public class PhytochemicalConstituentTestEntitySource
        extends TestEntitySource<PhytochemicalConstituentName, PhytochemicalConstituent> {

    public PhytochemicalConstituentTestEntitySource() {
        loadFile("plants/phytochemistry/phytochemical-constituents.json");
    }
}
