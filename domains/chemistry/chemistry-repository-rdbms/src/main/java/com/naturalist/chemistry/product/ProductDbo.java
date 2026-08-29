package com.naturalist.chemistry.product;

import com.naturalist.chemistry.compound.CompoundName;
import com.naturalist.observability.Constraints;
import com.naturalist.observability.Observer;
import com.naturalist.persistence.Dbo;
import com.naturalist.persistence.DboSchema;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Parent row of the {@link Product} SKU, anchored on a numeric {@code id} (PK) with
 * {@code name} and {@code display_name} unique — the same numeric-id rule as compound and
 * element. Its two multi-valued members — the {@code Set<CompoundName>} formulation and the
 * {@code Map<String, String> properties} — normalize into {@link ProductCompoundDbo} and
 * {@link ProductPropertyDbo}, both keyed on this row's {@code id}; the adapter reassembles
 * them via {@link #toEntity(Set, Map)}.
 */
@DboSchema(table = "product", primaryKey = "id", unique = {"name", "display_name"}, entity = Product.class)
final class ProductDbo implements Dbo {
    Long id;              // numeric identity anchor (BIGINT IDENTITY); null before insert.
    String name;
    String displayName;

    static ProductDbo from(Product p) {
        ProductDbo d = new ProductDbo();
        d.name = p.name().value();
        d.displayName = p.displayName();
        Observer.forClass(ProductDbo.class)
                .arguments("from", i -> i.observable(d, "dbo"))
                .throwWhenInvalid();
        return d;
    }

    Product toEntity(Set<CompoundName> compounds, Map<String, String> properties) {
        return new Product(ProductName.of(name), displayName, compounds, properties);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return c -> c
                .notNull(name, "name").kebabFormat(name, "name").maxLength(name, 96, "name")
                .notBlank(displayName, "displayName").maxLength(displayName, 128, "displayName");
    }
}
