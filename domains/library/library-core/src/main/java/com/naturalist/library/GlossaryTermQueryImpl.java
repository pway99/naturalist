package com.naturalist.library;

import com.naturalist.data.Page;
import com.naturalist.data.PageRequest;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.observability.Observer;

import java.util.Optional;
import java.util.Set;

@DomainService
class GlossaryTermQueryImpl implements GlossaryTermQuery {

    private static final Observer observer = Observer.forClass(GlossaryTermQueryImpl.class);

    private final GlossaryTermRepository repository;

    GlossaryTermQueryImpl(GlossaryTermRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<GlossaryTerm> getByName(GlossaryTermName name) {
        observer.arguments("getByName", i -> i.notNull(name, "name")).throwWhenInvalid();
        return repository.getByName(name);
    }

    @Override
    public GlossaryTermCollection findByNameSet(Set<GlossaryTermName> names) {
        observer.arguments("findByNameSet", i -> i.notNull(names, "names")).throwWhenInvalid();
        return GlossaryTermCollection.of(repository.getByEntityNameSet(names));
    }

    @Override
    public Page<GlossaryTerm> findPage(PageRequest pageRequest) {
        observer.arguments("findPage", i -> i.notNull(pageRequest, "pageRequest")).throwWhenInvalid();
        return repository.getPage(pageRequest);
    }
}
