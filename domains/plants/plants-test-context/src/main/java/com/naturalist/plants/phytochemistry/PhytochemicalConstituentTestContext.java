package com.naturalist.plants.phytochemistry;

import com.naturalist.data.NaturalistDatabase;

/**
 * Same-package assembly seam for the phytochemistry sub-context. Lives here (not folded into
 * {@code PlantsTestContext}) because PhytochemicalConstituentQueryImpl and PhytochemicalConstituentRepositoryMock are
 * package-private in this package; the root-package PlantsTestContext cannot reach them.
 */
public final class PhytochemicalConstituentTestContext {

    private PhytochemicalConstituentTestContext() {
    }

    public static PhytochemicalConstituentQuery createQuery(NaturalistDatabase db) {
        return new PhytochemicalConstituentQueryImpl(new PhytochemicalConstituentRepositoryMock(db));
    }
}
