package com.naturalist.library;

import com.naturalist.infrastructure.DomainService;
import com.naturalist.observability.Observer;

import java.util.Optional;

@DomainService
class CladeQueryImpl implements CladeQuery {

    private static final Observer observer = Observer.forClass(CladeQueryImpl.class);
    private final CladeViewFactory factory;

    CladeQueryImpl() {
        this.factory = new CladeViewFactory();
    }

    @Override
    public Optional<CladeView> getBySlug(String slug) {
        observer.arguments("getBySlug", i -> i.notBlank(slug, "slug")).throwWhenInvalid();
        return factory.buildBySlug(slug);
    }

    @Override
    public CladeTreeNode tree() {
        return factory.buildTree();
    }
}
