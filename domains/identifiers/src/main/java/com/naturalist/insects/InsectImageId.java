package com.naturalist.insects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.PersistenceId;

public final class InsectImageId extends PersistenceId<Long> {
    private InsectImageId(Long value) { super(value); }

    @JsonCreator
    public static InsectImageId of(Long value) { return new InsectImageId(value); }
}
