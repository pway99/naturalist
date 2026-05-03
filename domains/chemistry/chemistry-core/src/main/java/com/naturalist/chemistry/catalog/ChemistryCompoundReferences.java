package com.naturalist.chemistry.catalog;

import com.naturalist.catalog.DomainId;
import com.naturalist.catalog.EntityRef;
import com.naturalist.catalog.EntityReferences;
import com.naturalist.chemistry.ChemistryDomain;
import com.naturalist.chemistry.compound.CompoundName;
import com.naturalist.chemistry.product.ProductCollection;
import com.naturalist.chemistry.product.ProductQuery;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.observability.Observer;
import com.naturalist.resilience.Resilient;

import java.util.stream.Stream;

/**
 * Inverse-direction catalog provider for the chemistry domain. Answers
 * "which chemistry entities reference this {@link CompoundName}?" by
 * delegating to the product sub-context, where {@link com.naturalist.chemistry.product.Product}
 * authoritatively records its formulation.
 *
 * <h2>Compound → Product back-reference</h2>
 * Powers the "Found in products" panel on the compound details page —
 * looking up Thymol surfaces Apiguard, looking up Calcium-Chloride surfaces
 * Bonide Rot-Stop RTU. The relationship is intra-domain (both ends live in
 * chemistry) but uses the same SPI as cross-domain providers; the kernel
 * does not distinguish.
 *
 * <h2>One ref per matching product</h2>
 * Each matching {@link com.naturalist.chemistry.product.Product} contributes
 * a single {@link EntityRef} to the result — Product holds the
 * {@link CompoundName} reference directly via {@code Set<CompoundName> compounds},
 * with no intervening link record.
 *
 * <h2>Live, not cached</h2>
 * The kernel re-queries on every fan-out. No caching here either.
 *
 * <h2>Empty target</h2>
 * A {@code null} {@code target} short-circuits to an empty stream. Defensive —
 * the {@code Catalog} surface already maps null to an empty result.
 */
@DomainService
@Resilient(name = "catalog.fanout")
public class ChemistryCompoundReferences implements EntityReferences<CompoundName> {

    private static final DomainId DOMAIN = new ChemistryDomain();

    private final ProductQuery products;

    public ChemistryCompoundReferences(ProductQuery products) {
        Observer.forClass(ChemistryCompoundReferences.class)
                .arguments("constructor", i -> i.notNull(products, "products"))
                .throwWhenInvalid();
        this.products = products;
    }

    @Override
    public DomainId domain() {
        return DOMAIN;
    }

    @Override
    public Class<CompoundName> referenceType() {
        return CompoundName.class;
    }

    @Override
    public Stream<EntityRef> referencesTo(CompoundName target) {
        if (target == null) {
            return Stream.empty();
        }
        ProductCollection matches = products.findByCompoundName(target);
        return matches.stream().map(product -> new EntityRef(DOMAIN, product.name()));
    }
}
