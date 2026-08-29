package com.naturalist.chemistry.product;

import com.naturalist.chemistry.compound.CompoundName;
import com.naturalist.observability.Constraints;
import com.naturalist.observability.Observer;
import com.naturalist.persistence.Dbo;
import com.naturalist.persistence.DboSchema;
import com.naturalist.persistence.Fk;

import java.util.function.Consumer;

/**
 * Join row for one member of a product's {@code Set<CompoundName>} formulation. Both stored
 * references are numeric FKs ({@code product_id}, {@code compound_id}); the DBO carries the two
 * {@code name}s, which the mapper JOINs to project on read and nested-selects the ids from on
 * write (a product citing a missing compound fails). No name is persisted here. Backs
 * {@code ProductQuery.findByCompoundName} — the reverse "which products contain X" lookup.
 */
@DboSchema(table = "product_compound", primaryKey = "product_id,compound_id",
           foreignKeys = {
                   @Fk(columns = "product_id", references = "product(id)"),
                   @Fk(columns = "compound_id", references = "compound(id)")
           },
           entity = Product.class)
final class ProductCompoundDbo implements Dbo {
    String productName;   // JOIN projection (read) / nested-select key (write); not a stored column
    String compoundName;  // JOIN projection (read) / nested-select key (write); not a stored column

    static ProductCompoundDbo from(ProductName productName, CompoundName compoundName) {
        ProductCompoundDbo d = new ProductCompoundDbo();
        d.productName = productName.value();
        d.compoundName = compoundName.value();
        Observer.forClass(ProductCompoundDbo.class)
                .arguments("from", i -> i.observable(d, "dbo"))
                .throwWhenInvalid();
        return d;
    }

    CompoundName toCompoundName() {
        return CompoundName.of(compoundName);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return c -> c
                .notNull(productName, "productName").kebabFormat(productName, "productName")
                .notNull(compoundName, "compoundName").kebabFormat(compoundName, "compoundName");
    }
}
