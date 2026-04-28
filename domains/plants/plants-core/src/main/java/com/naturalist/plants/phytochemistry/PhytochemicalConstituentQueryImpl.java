package com.naturalist.plants.phytochemistry;

import com.naturalist.observability.Observer;

class PhytochemicalConstituentQueryImpl implements PhytochemicalConstituentQuery {

    private final PhytochemicalConstituentEntityQuery phytochemicalConstituentEntityQuery;

    PhytochemicalConstituentQueryImpl(PhytochemicalConstituentEntityQuery phytochemicalConstituentEntityQuery) {
        Observer.forClass(PhytochemicalConstituentQueryImpl.class).arguments("constructor", i -> i
                        .notNull(phytochemicalConstituentEntityQuery, "phytochemicalConstituentEntityQuery"))
                .throwWhenInvalid();
        this.phytochemicalConstituentEntityQuery = phytochemicalConstituentEntityQuery;
    }

    @Override
    public PhytochemicalConstituentEntityQuery constituents() {
        return phytochemicalConstituentEntityQuery;
    }
}
