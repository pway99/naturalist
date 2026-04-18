package com.naturalist.soil.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.FactName;

import java.util.UUID;

public final class TillageEventName extends FactName {

    private TillageEventName(UUID value) {
        super(value);
    }

    @JsonCreator
    public static TillageEventName of(UUID value) {
        return new TillageEventName(value);
    }
}
