package com.naturalist.soil.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.PersistenceId;

public final class IrrigationEventId extends PersistenceId<Long> {

    private IrrigationEventId(Long value) {
        super(value);
    }

    @JsonCreator
    public static IrrigationEventId of(Long value) {
        return new IrrigationEventId(value);
    }
}
