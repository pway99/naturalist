package com.naturalist.library;

import com.naturalist.authority.CitationName;
import com.naturalist.catalog.EntityRef;
import com.naturalist.ddd.Entity;
import com.naturalist.observability.Constraints;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

public record CitationAssociation(
        CitationAssociationId id,
        CitationName citationName,
        EntityRef subject,
        @Nullable String note
) implements Entity<CitationAssociationId> {

    public CitationAssociation withNote(@Nullable String value) {
        return new CitationAssociation(id, citationName, subject, value);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityId(id, "id")
                .entityName(citationName, "citationName")
                .valueObject(subject, "subject");
    }
}
