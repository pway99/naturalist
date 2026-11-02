package com.naturalist.insects;

import com.naturalist.infrastructure.DomainService;
import com.naturalist.naturalist.NaturalistName;
import com.naturalist.observability.Observer;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

@DomainService
class InsectViewQueryImpl implements InsectViewQuery {
    private final Observer observer = Observer.forClass(InsectViewQueryImpl.class);

    private final InsectViewFactory viewFactory;

    InsectViewQueryImpl(InsectQuery insectQuery) {
        observer.arguments("constructor", i -> i.notNull(insectQuery, "insectQuery")).throwWhenInvalid();
        this.viewFactory = new InsectViewFactory(insectQuery);
    }

    @Override
    public Optional<InsectViews.RankPage> rankPage(InsectRankName name, @Nullable NaturalistName viewer) {
        observer.arguments("rankPage", i -> i.identifier(name, "name").whenNotNull(viewer, v -> v.entityName(viewer, "viewer"))).throwWhenInvalid();
        return viewFactory.buildRankPage(name, viewer);
    }
}
