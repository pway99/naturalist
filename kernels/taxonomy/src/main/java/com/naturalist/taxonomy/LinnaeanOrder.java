package com.naturalist.taxonomy;

/**
 * Contract for catalog entities that represent an order-rank Linnaean taxon.
 * <p>
 * The contract exposes the order epithet, which is non-null at the
 * implementing entity level. The order slug is derived mechanically
 * from the epithet.
 *
 * <p>Implemented by per-domain order entities (e.g.
 * {@code com.naturalist.insects.InsectOrder}). The implementing entity
 * supplies its own typed name via its {@code NamedEntity<ORDER_NAME>}
 * binding; this interface is concerned only with the rank-level contract.
 */
public interface LinnaeanOrder {

    TaxonomicOrder order();

    /**
     * The order slug derived from the order epithet — lowercase form.
     * The slug is {@code identity}; common names are findable but not
     * authoritative.
     */
    default String orderSlug() {
        return TaxonomicSlugs.orderSlug(order());
    }
}
