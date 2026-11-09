package com.naturalist.plants.phytochemistry;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.test.context.TestGraph;

/**
 * External test context of the phytochemistry sub-context: a facade over a fresh {@link TestGraph}, which
 * builds the package's query implementation and repository mock by reflection under plants-core's
 * {@code PhytochemistryTestContextInternal}. The wiring lives there once.
 */
public final class PhytochemicalConstituentTestContext {

    private PhytochemicalConstituentTestContext() {
    }

    public static PhytochemicalConstituentQuery createQuery(NaturalistDatabase db) {
        return TestGraph.create(db).get(PhytochemicalConstituentQuery.class);
    }
}
