package com.naturalist.test.query.nofanout;

import java.util.List;
import java.util.Set;

class FooQueryImpl {
    private final FooRepository repository;
    FooQueryImpl(FooRepository repository) { this.repository = repository; }

    /** Deliberate N+1: one getByName per element. */
    public List<String> loadAll(List<String> names) {
        return names.stream().map(repository::getByName).toList();
    }

    /** Batched sibling: a single select. */
    public List<String> loadBatched(Set<String> names) {
        return repository.getByEntityNameSet(names);
    }
}
