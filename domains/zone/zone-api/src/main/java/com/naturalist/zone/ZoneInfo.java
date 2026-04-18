package com.naturalist.zone;

import com.naturalist.ddd.CatalogEntity;
import com.naturalist.observability.Constraints;

import java.util.function.Consumer;

public record ZoneInfo(
        ZoneId id,
        ZoneName name,
        ZoneType type
) implements CatalogEntity<ZoneId, ZoneName> {

    @Override
    public ZoneInfo withId(ZoneId id) {
        return new ZoneInfo(id, name, type);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityId(id, "id")
                .entityName(name, "name");
    }
}
