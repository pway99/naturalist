package com.naturalist.library;

import com.naturalist.ddd.EntityId;

import java.util.UUID;

public final class CitationAssociationId extends EntityId {
    private CitationAssociationId(UUID value) {
        super(value);
    }

    public static CitationAssociationId of(UUID value) {
        return new CitationAssociationId(value);
    }

    public static CitationAssociationId create() {
        return new CitationAssociationId(EntityId.newUUID());
    }
}
