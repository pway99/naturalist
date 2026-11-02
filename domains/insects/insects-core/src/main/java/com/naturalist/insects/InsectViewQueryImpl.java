package com.naturalist.insects;

import com.naturalist.data.Page;
import com.naturalist.data.PageRequest;
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

    @Override
    public Page<InsectViews.SpeciesCard> speciesCards(PageRequest request, @Nullable NaturalistName viewer) {
        observer.arguments("speciesCards", i -> i.valueObject(request, "request").whenNotNull(viewer, v -> v.entityName(viewer, "viewer"))).throwWhenInvalid();
        return viewFactory.speciesCards(request, viewer);
    }

    @Override
    public Page<InsectViews.SpeciesCard> collectedBy(NaturalistName viewer, PageRequest request) {
        observer.arguments("collectedBy", i -> i.entityName(viewer, "viewer").valueObject(request, "request")).throwWhenInvalid();
        return viewFactory.collectedBy(viewer, request);
    }

    @Override
    public Page<InsectViews.RankCard> orderCards(PageRequest request) {
        observer.arguments("orderCards", i -> i.valueObject(request, "request")).throwWhenInvalid();
        return viewFactory.orderCards(request);
    }

    @Override
    public Page<InsectViews.RankCard> familyCards(PageRequest request) {
        observer.arguments("familyCards", i -> i.valueObject(request, "request")).throwWhenInvalid();
        return viewFactory.familyCards(request);
    }

    @Override
    public Page<InsectViews.RankCard> genusCards(PageRequest request) {
        observer.arguments("genusCards", i -> i.valueObject(request, "request")).throwWhenInvalid();
        return viewFactory.genusCards(request);
    }
}
