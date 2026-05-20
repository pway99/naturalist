package com.naturalist.insects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.EntityName;

public final class InsectSpeciesName extends EntityName implements InsectRankName {
    private InsectSpeciesName(String value) {
        super(value);
    }

    @JsonCreator
    public static InsectSpeciesName of(String value) {
        return new InsectSpeciesName(value);
    }

    @Override
    protected int maxLength() {
        return 64;
    }
}
