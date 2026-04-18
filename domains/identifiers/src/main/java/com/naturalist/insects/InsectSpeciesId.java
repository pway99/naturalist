package com.naturalist.insects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.PersistenceId;

public final class InsectSpeciesId extends PersistenceId<Long> {
    private InsectSpeciesId(Long value) { super(value); }

    @JsonCreator
    public static InsectSpeciesId of(Long value) { return new InsectSpeciesId(value); }
}
