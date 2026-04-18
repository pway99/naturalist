package com.naturalist.insects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.CatalogName;

public final class InsectSpeciesName extends CatalogName {
    private InsectSpeciesName(String value) { super(value); }

    @JsonCreator
    public static InsectSpeciesName of(String value) { return new InsectSpeciesName(value); }

    @Override
    protected int maxLength() {
        return 64;
    }
}
