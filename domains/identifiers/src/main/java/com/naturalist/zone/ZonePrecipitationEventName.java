package com.naturalist.zone;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.FactName;

import java.util.UUID;

public final class ZonePrecipitationEventName extends FactName {

    private ZonePrecipitationEventName(UUID value) {
        super(value);
    }

    @JsonCreator
    public static ZonePrecipitationEventName of(UUID value) {
        return new ZonePrecipitationEventName(value);
    }
}
