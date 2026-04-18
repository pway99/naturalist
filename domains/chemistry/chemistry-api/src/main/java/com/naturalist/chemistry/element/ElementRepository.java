package com.naturalist.chemistry.element;

import com.naturalist.Incubating;
import com.naturalist.data.EntityRepository;

/**
 * Note: This is an experiment in api organization with the intent being a clean discoverable api not
 * cluttered with a repository api for every entity.
 */
@Incubating("Investigating a pattern where EntityRepositories are nested within a single interface")
interface ElementRepository {
    interface ElementEntityRepository  extends EntityRepository<ElementId, ElementName, Element> {}
}
