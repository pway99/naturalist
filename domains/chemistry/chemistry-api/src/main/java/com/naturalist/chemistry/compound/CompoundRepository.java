package com.naturalist.chemistry.compound;

import com.naturalist.Incubating;
import com.naturalist.data.EntityRepository;

import java.util.List;

/**
 * Note: This is an experiment in api organization with the intent being a clean discoverable api not
 * cluttered with a repository api for every entity.
 */
@Incubating("Investigating a pattern where EntityRepositories are nested within a single interface")
interface CompoundRepository {
    interface CompoundEntityRepository extends EntityRepository<CompoundName, Compound> {
        List<CompoundName> getAllCompoundNames();
    }
}
