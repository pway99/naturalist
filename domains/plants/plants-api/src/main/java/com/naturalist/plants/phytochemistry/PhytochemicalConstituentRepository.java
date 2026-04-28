package com.naturalist.plants.phytochemistry;

import com.naturalist.data.EntityRepository;

/**
 * Namespace class for the phytochemistry sub-context's repository surface.
 * <p>
 * The nested {@link PhytochemicalConstituentEntityRepository} is the single
 * port for persisting and reading {@link PhytochemicalConstituent} entities.
 * It is package-private so adapters in {@code plants-repository-test} and the
 * eventual {@code plants-repository-rdms} module can implement it; consumers
 * in other domains never see the repository contract directly — cross-domain
 * reads go through queries.
 */
class PhytochemicalConstituentRepository {
    protected interface PhytochemicalConstituentEntityRepository
            extends EntityRepository<PhytochemicalConstituentName, PhytochemicalConstituent> {}
}
