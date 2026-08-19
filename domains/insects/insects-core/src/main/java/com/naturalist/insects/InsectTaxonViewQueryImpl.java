package com.naturalist.insects;

import com.naturalist.observability.Observer;

import java.util.Optional;

class InsectTaxonViewQueryImpl implements InsectQuery.TaxonViewQuery {

    private final Observer observer = Observer.forClass(getClass());
    private final InsectTaxonViewFactory factory;

    InsectTaxonViewQueryImpl(InsectTaxonViewFactory factory) {
        this.factory = factory;
    }

    @Override
    public Optional<InsectTaxonView> getByName(InsectRankName name) {
        observer.arguments("getByName", i -> i.identifier(name, "name")).throwWhenInvalid();
        return factory.buildByName(name);
    }
}
