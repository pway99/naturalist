package com.naturalist.chemistry.product;

import com.naturalist.observability.Constraints;
import com.naturalist.observability.Observer;
import com.naturalist.persistence.Dbo;
import com.naturalist.persistence.DboSchema;
import com.naturalist.persistence.Fk;

import java.util.function.Consumer;

/**
 * Child row for one entry of a product's {@code Map<String, String> properties} (SKU-scoped
 * attributes: concentration, application window, NPK ratio, …), one row per key. The stored
 * reference is the numeric {@code product_id} (FK-enforced); the DBO carries the product's
 * {@code name} for the mapper's JOIN projection (read) and nested-select (write).
 */
@DboSchema(table = "product_property", primaryKey = "product_id,property_key",
           foreignKeys = @Fk(columns = "product_id", references = "product(id)"),
           entity = Product.class)
final class ProductPropertyDbo implements Dbo {
    String productName;   // JOIN projection (read) / nested-select key (write); not a stored column
    String propertyKey;
    String propertyValue;

    static ProductPropertyDbo from(ProductName productName, String key, String value) {
        ProductPropertyDbo d = new ProductPropertyDbo();
        d.productName = productName.value();
        d.propertyKey = key;
        d.propertyValue = value;
        Observer.forClass(ProductPropertyDbo.class)
                .arguments("from", i -> i.observable(d, "dbo"))
                .throwWhenInvalid();
        return d;
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return c -> c
                .notNull(productName, "productName").kebabFormat(productName, "productName")
                .notBlank(propertyKey, "propertyKey").maxLength(propertyKey, 64, "propertyKey")
                .notNull(propertyValue, "propertyValue");
    }
}
