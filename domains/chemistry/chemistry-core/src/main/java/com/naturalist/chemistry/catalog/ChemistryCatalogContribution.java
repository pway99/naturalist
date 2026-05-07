package com.naturalist.chemistry.catalog;

import com.naturalist.catalog.CatalogContribution;
import com.naturalist.catalog.DomainId;
import com.naturalist.catalog.EntityRef;
import com.naturalist.chemistry.ChemistryDomain;
import com.naturalist.chemistry.compound.Compound;
import com.naturalist.chemistry.compound.CompoundQuery;
import com.naturalist.chemistry.product.Product;
import com.naturalist.chemistry.product.ProductQuery;
import com.naturalist.data.Pages;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.observability.Observer;

import java.util.stream.Stream;

/**
 * Forward-direction catalog contribution for the chemistry domain. Emits one
 * {@link SearchableEntity} per {@link Compound} and one per {@link Product}
 * with the tokens under which a young naturalist might search:
 *
 * <ul>
 *   <li><b>Compound</b> — slug, {@code commonName}, and chemical formula
 *       (e.g. {@code "calcium-sulfate-dihydrate"}, {@code "Calcium Sulfate
 *       Dihydrate"}, {@code "CaSO4·2H2O"}).</li>
 *   <li><b>Product</b> — slug and {@code displayName} (e.g. {@code "apiguard"},
 *       {@code "Apiguard (Véto-pharma)"}).</li>
 * </ul>
 *
 * <h2>Token collisions are normal</h2>
 * Per the redirect plan's M4′ entry, this contribution emits every derivable
 * token unconditionally — ambiguity filtering is the search index's concern.
 *
 * <h2>Live derivation</h2>
 * {@link #searchableEntities()} returns a fresh stream on every call. Compounds
 * or products added after assembly are reflected automatically.
 */
@DomainService
public class ChemistryCatalogContribution implements CatalogContribution {

    private static final DomainId DOMAIN = new ChemistryDomain();

    /**
     * Page size for catalog assembly — bulk read, no horizon needed.
     */
    private static final int ASSEMBLY_PAGE_SIZE = 1000;

    private final CompoundQuery.CompoundEntityQuery compounds;
    private final ProductQuery products;

    public ChemistryCatalogContribution(CompoundQuery.CompoundEntityQuery compounds, ProductQuery products) {
        Observer.forClass(ChemistryCatalogContribution.class)
                .arguments("constructor", i -> i
                        .notNull(compounds, "compounds")
                        .notNull(products, "products"))
                .throwWhenInvalid();
        this.compounds = compounds;
        this.products = products;
    }

    @Override
    public DomainId domain() {
        return DOMAIN;
    }

    @Override
    public Stream<SearchableEntity> searchableEntities() {
        return Stream.concat(compoundEntities(), productEntities());
    }

    private Stream<SearchableEntity> compoundEntities() {
        return Pages.stream(ASSEMBLY_PAGE_SIZE, compounds::findPage)
                .map(ChemistryCatalogContribution::toSearchableCompound);
    }

    private Stream<SearchableEntity> productEntities() {
        return Pages.stream(ASSEMBLY_PAGE_SIZE, products::findPage)
                .map(ChemistryCatalogContribution::toSearchableProduct);
    }

    private static SearchableEntity toSearchableCompound(Compound compound) {
        EntityRef target = new EntityRef(DOMAIN, compound.name());
        return new SearchableEntity(target, compoundTokens(compound));
    }

    private static SearchableEntity toSearchableProduct(Product product) {
        EntityRef target = new EntityRef(DOMAIN, product.name());
        return new SearchableEntity(target, productTokens(product));
    }

    private static Stream<String> compoundTokens(Compound compound) {
        return Stream.of(
                compound.name().value(),
                compound.commonName(),
                compound.formula());
    }

    private static Stream<String> productTokens(Product product) {
        return Stream.of(
                product.name().value(),
                product.displayName());
    }
}
