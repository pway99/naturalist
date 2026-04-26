package com.naturalist.chemistry.compound.depiction;

import com.naturalist.Incubating;
import com.naturalist.chemistry.compound.CompoundName;
import com.naturalist.data.EntityRepository;

import java.util.List;
import java.util.Optional;

/**
 * Note: This is an experiment in api organization with the intent being a clean discoverable api not
 * cluttered with a repository api for every entity.
 */
@Incubating("Investigating a pattern where EntityRepositories are nested within a single interface")
interface DepictionRepository {
    interface DepictionEntityRepository extends EntityRepository<DepictionId, CompoundDepiction> {

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
