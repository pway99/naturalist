package com.naturalist.usage;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.EntityName;

/**
 * Strongly typed natural key for {@code UsageCounter} entities.
 * <p>
 * The single seeded counter is {@code identification}.
 */
public final class UsageCounterName extends EntityName {

    private UsageCounterName(String value) {
        super(value);
    }

    @JsonCreator
    public static UsageCounterName of(String value) {
        return new UsageCounterName(value);
    }

    @Override
    protected int maxLength() {
        return 64;
    }
}
