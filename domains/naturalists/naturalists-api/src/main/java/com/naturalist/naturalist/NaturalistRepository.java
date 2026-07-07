package com.naturalist.naturalist;

import com.naturalist.data.EntityRepository;

/**
 * Namespace for the naturalists sub-context's write-side repositories.
 *
 * <p>A {@code class}, not an {@code interface}, so nested repository contracts stay
 * {@code protected} — hidden from foreign packages while permitting same-package
 * adapter implementations (ADR-020). Non-instantiable.
 */
class NaturalistRepository {

    private NaturalistRepository() {
    }

    protected interface NaturalistEntityRepository
            extends EntityRepository<NaturalistName, Naturalist> {
    }
}
