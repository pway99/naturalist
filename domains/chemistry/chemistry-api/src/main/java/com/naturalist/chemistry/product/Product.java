package com.naturalist.chemistry.product;

import com.naturalist.chemistry.compound.CompoundName;
import com.naturalist.ddd.NamedEntity;
import com.naturalist.ddd.UniqueValue;
import com.naturalist.observability.Constraints;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * A commercial product that contains one or more {@link com.naturalist.chemistry.compound.Compound}
 * formulations.
 * <p>
 * Products are catalog SKUs (e.g. {@code "Apiguard (Véto-pharma)"},
 * {@code "TPS Nutrients CalMag OAC"}). The product authoritatively defines its ingredient
 * list — the reverse lookup ("which products contain compound X?") is a query, not a
 * stored field on {@code Compound}.
 * <p>
 * Lives in its own subpackage so it can graduate to a standalone {@code product} domain
 * without disturbing chemistry consumers.
 */
public record Product(
        ProductName name,
        @UniqueValue String displayName,
        Set<CompoundName> compounds,
        Map<String, String> properties
) implements NamedEntity<ProductName> {

    public Optional<String> property(String key) {
        return Optional.ofNullable(properties.get(key));
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityName(name, "name")
                .notBlank(displayName, "displayName")
                .notEmpty(compounds, "compounds")
                .notNull(properties, "properties");
    }
}
