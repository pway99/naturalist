package com.naturalist.zone;

import com.naturalist.ddd.NamedEntity;
import com.naturalist.observability.Constraints;

import java.util.function.Consumer;

public record ZoneInfo(
        ZoneName name,
        ZoneType type
) implements NamedEntity<ZoneName> {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityName(name, "name");
    }
}
