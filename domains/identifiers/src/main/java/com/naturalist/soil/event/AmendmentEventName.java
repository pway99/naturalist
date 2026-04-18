package com.naturalist.soil.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.FactName;

import java.util.UUID;

public final class AmendmentEventName extends FactName {

    private AmendmentEventName(UUID value) {
        super(value);
    }

    @JsonCreator
    public static AmendmentEventName of(UUID value) {
        return new AmendmentEventName(value);
    }
}
