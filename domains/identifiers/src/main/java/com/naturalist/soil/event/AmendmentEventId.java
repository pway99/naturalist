package com.naturalist.soil.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.PersistenceId;

public final class AmendmentEventId extends PersistenceId<Long> {

    private AmendmentEventId(Long value) {
        super(value);
    }

    @JsonCreator
    public static AmendmentEventId of(Long value) {
        return new AmendmentEventId(value);
    }
}
