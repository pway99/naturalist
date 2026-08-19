package com.naturalist.plants.phytochemistry;

import com.naturalist.data.NaturalistDatabase;

/**
 * Same-package assembly seam for the phytochemistry sub-context. Lives here (not folded into
 * {@code PlantsTestContext}) because PlantPhytochemicalConstituentQueryImpl and PlantPhytochemicalConstituentRepositoryMock are
 * package-private in this package; the root-package PlantsTestContext cannot reach them.
 */
public final class PhytochemicalConstituentTestContext {

    private PhytochemicalConstituentTestContext() {
    }

    public static PhytochemicalConstituentQuery createQuery(NaturalistDatabase db) {
        return new PlantPhytochemicalConstituentQueryImpl(new PlantPhytochemicalConstituentRepositoryMock(db));
    }
}
