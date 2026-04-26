package com.naturalist.chemistry.compound;

import com.naturalist.data.EntityRepository;

import java.util.List;
import java.util.Optional;

/**
 * Namespace for the compound sub-context's write-side repositories — the single
 * discoverable entry point for persistence of compound catalog data.
 *
 * <p>Nested repositories scope to a single entity each:
 * <ul>
 *   <li>{@link CompoundEntityRepository} — {@link Compound} entities.</li>
 *   <li>{@link DepictionRepository} — {@link CompoundDepiction} entities.</li>
 * </ul>
 *
 * <p>This is a {@code class}, not an {@code interface}, so the nested repository
 * contracts can carry their own access modifiers. Inside an interface, nested types
 * would be implicitly {@code public static}; inside a class, {@code protected} keeps
 * them hidden from foreign packages while permitting same-package adapter
 * implementations. The class is non-instantiable — it holds no state and no behavior,
 * only the namespace (ADR-020).
 */
class CompoundRepository {
    final CompoundEntityRepository compoundRepository;
    final DepictionRepository depictionRepository;

    private CompoundRepository(CompoundEntityRepository compoundRepository, DepictionRepository depictionRepository) {
        this.compoundRepository = compoundRepository;
        this.depictionRepository = depictionRepository;
    }

    static CompoundRepository create(CompoundEntityRepository compoundRepository, DepictionRepository depictionRepository) {
        return new CompoundRepository(compoundRepository, depictionRepository);
    }

    CompoundEntityRepository compoundRepository() {
        return compoundRepository;
    }

    DepictionRepository depictionRepository() {
        return depictionRepository;
    }

    protected interface CompoundEntityRepository
            extends EntityRepository<CompoundName, Compound> {
        List<CompoundName> getAllCompoundNames();
    }

    protected interface DepictionRepository
            extends EntityRepository<DepictionId, CompoundDepiction> {

        /**
         * Look up the depiction for a given compound. The {@code compoundName} field
         * is unique on {@link CompoundDepiction} (one depiction per compound), so the
         * result is at most one entity.
         */
        Optional<CompoundDepiction> getByCompoundName(CompoundName compoundName);

        /**
         * The set of {@link CompoundName}s with a catalogued depiction — drives the
         * "depictable compounds" filter at the consumer surface (e.g. the chemistry
         * console list view).
         */
        List<CompoundName> getAllDepictedCompoundNames();
    }
}
