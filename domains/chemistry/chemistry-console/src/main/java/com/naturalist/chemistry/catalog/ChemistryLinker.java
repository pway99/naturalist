package com.naturalist.chemistry.catalog;

import com.naturalist.catalog.EntityRef;
import com.naturalist.catalog.EntityRefLinker;
import com.naturalist.chemistry.compound.CompoundName;
import com.naturalist.chemistry.element.ElementName;
import com.naturalist.chemistry.product.ProductName;
import com.naturalist.infrastructure.DomainService;

/**
 * Chemistry-domain {@link EntityRefLinker}: maps chemistry-owned
 * {@code EntityName} types to detail-page URLs. The single place to look
 * when adding a new chemistry entity or moving an existing one.
 */
@DomainService
public class ChemistryLinker implements EntityRefLinker {

    @Override
    public String linkFor(EntityRef ref) {
        return switch (ref.name()) {
            case ElementName n -> "/chemistry/elements/" + n.value();
            case CompoundName n -> "/chemistry/" + n.value();
            case ProductName n -> "/chemistry/products/" + n.value();
            default -> null;
        };
    }
}
