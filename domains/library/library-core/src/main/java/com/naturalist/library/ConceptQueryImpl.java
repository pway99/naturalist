package com.naturalist.library;

import com.naturalist.data.Page;
import com.naturalist.data.PageRequest;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.observability.Observer;

import java.util.Optional;
import java.util.Set;

@DomainService
class ConceptQueryImpl implements ConceptQuery {

    private static final Observer observer = Observer.forClass(ConceptQueryImpl.class);

    private final ConceptRepository repository;

    ConceptQueryImpl(ConceptRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Concept> getByName(ConceptName name) {
        observer.arguments("getByName", i -> i.notNull(name, "name")).throwWhenInvalid();
        return repository.getByName(name);
    }

    @Override
    public ConceptCollection findByNameSet(Set<ConceptName> names) {
        observer.arguments("findByNameSet", i -> i.notNull(names, "names")).throwWhenInvalid();
        return ConceptCollection.of(repository.getByEntityNameSet(names));
    }

    @Override
    public Page<Concept> findPage(PageRequest pageRequest) {
        observer.arguments("findPage", i -> i.notNull(pageRequest, "pageRequest")).throwWhenInvalid();
        return repository.getPage(pageRequest);
    }
}
