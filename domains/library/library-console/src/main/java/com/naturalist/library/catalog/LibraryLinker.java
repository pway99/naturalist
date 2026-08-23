package com.naturalist.library.catalog;

import com.naturalist.catalog.EntityRef;
import com.naturalist.catalog.EntityRefLinker;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.library.ConceptName;

/**
 * Library-domain {@link EntityRefLinker}: maps library-owned
 * {@code EntityName} types to detail-page URLs. The single place to look
 * when adding a new library entity or moving an existing one.
 */
@DomainService
public class LibraryLinker implements EntityRefLinker {

    @Override
    public String linkFor(EntityRef ref) {
        return switch (ref.name()) {
            case ConceptName n -> "/concepts/" + n.value();
            default -> null;
        };
    }
}
