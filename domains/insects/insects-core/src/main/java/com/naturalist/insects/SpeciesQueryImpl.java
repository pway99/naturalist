package com.naturalist.insects;

import com.naturalist.data.AbstractEntityQuery;
import com.naturalist.data.EntityRepository;
import com.naturalist.ddd.BehavioralCollection;
import com.naturalist.ddd.EntityName;
import com.naturalist.ddd.PersistenceId;

import java.util.Optional;
import java.util.Set;

class SpeciesQueryImpl extends AbstractEntityQuery<InsectSpeciesId, InsectSpeciesName, InsectSpecies, InsectEntityCollections.SpeciesCollection> implements InsectQuery.SpeciesQuery {
    /**
     * Constructs a query backed by the supplied repository.
     * The {@link Observer} is scoped to the concrete subclass — {@code getClass()} at
     * construction time is the runtime type of the adapter, not this abstract class.
     *
     * @param repository the entity repository to delegate reads to; must not be null
     */
    protected SpeciesQueryImpl(InsectRepository.SpeciesRepository repository) {
        super(repository);
    }

    @Override
    public InsectEntityCollections.SpeciesCollection findByNameSet(Set<InsectSpeciesName> insectSpeciesNames) {
        return new InsectEntityCollections.SpeciesCollection(repository().getByEntityNameSet(insectSpeciesNames));
    }

    @Override
    public InsectEntityCollections.SpeciesCollection findByIdSet(Set<InsectSpeciesId> insectSpeciesIds) {
        return new InsectEntityCollections.SpeciesCollection(repository().getByIdSet(insectSpeciesIds));
    }
}
