package com.naturalist.library;

import com.naturalist.authority.Citation;
import com.naturalist.authority.CitationName;
import com.naturalist.data.Page;
import com.naturalist.data.PageRequest;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.observability.Observer;

import java.util.Optional;
import java.util.Set;

@DomainService
class CitationQueryImpl implements CitationQuery {

    private static final Observer observer = Observer.forClass(CitationQueryImpl.class);

    private final CitationRepository repository;

    CitationQueryImpl(CitationRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Citation> getByName(CitationName name) {
        observer.arguments("getByName", i -> i.notNull(name, "name")).throwWhenInvalid();
        return repository.getByName(name);
    }

    @Override
    public CitationCollection findByNameSet(Set<CitationName> names) {
        observer.arguments("findByNameSet", i -> i.notNull(names, "names")).throwWhenInvalid();
        return CitationCollection.of(repository.getByEntityNameSet(names));
    }

    @Override
    public Page<Citation> findPage(PageRequest pageRequest) {
        observer.arguments("findPage", i -> i.notNull(pageRequest, "pageRequest")).throwWhenInvalid();
        return repository.getPage(pageRequest);
    }
}
