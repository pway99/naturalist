package com.naturalist.insects;

import com.naturalist.ddd.EntityName;
import com.naturalist.taxonomy.LinealRank;

public final class InsectSpeciesName extends EntityName implements InsectRankName {
    private InsectSpeciesName(String value) {
        super(value);
    }

    public static InsectSpeciesName of(String value) {
        return new InsectSpeciesName(value);
    }

    @Override
    protected int maxLength() {
        return 64;
    }

    @Override
    public LinealRank rank() {
        return LinealRank.SPECIES;
    }
}
