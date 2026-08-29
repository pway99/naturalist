package com.naturalist.chemistry.compound;

import com.naturalist.observability.Constraints;
import com.naturalist.observability.Observer;
import com.naturalist.persistence.Dbo;
import com.naturalist.persistence.DboSchema;
import com.naturalist.persistence.Fk;

import java.util.function.Consumer;

/**
 * Child row for one entry of a compound's {@code Map<String, String> properties} — the normalized
 * form of the free-form key/value bag (one row per key). The stored reference is the numeric
 * {@code compound_id} (FK-enforced); the DBO carries the compound's {@code name} for the mapper's
 * JOIN projection (read) and nested-select (write).
 */
@DboSchema(table = "compound_property", primaryKey = "compound_id,property_key",
           foreignKeys = @Fk(columns = "compound_id", references = "compound(id)"),
           entity = Compound.class)
final class CompoundPropertyDbo implements Dbo {
    String compoundName;  // JOIN projection (read) / nested-select key (write); not a stored column
    String propertyKey;
    String propertyValue;

    static CompoundPropertyDbo from(CompoundName compoundName, String key, String value) {
        CompoundPropertyDbo d = new CompoundPropertyDbo();
        d.compoundName = compoundName.value();
        d.propertyKey = key;
        d.propertyValue = value;
        Observer.forClass(CompoundPropertyDbo.class)
                .arguments("from", i -> i.observable(d, "dbo"))
                .throwWhenInvalid();
        return d;
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return c -> c
                .notNull(compoundName, "compoundName").kebabFormat(compoundName, "compoundName")
                .notBlank(propertyKey, "propertyKey").maxLength(propertyKey, 64, "propertyKey")
                .notNull(propertyValue, "propertyValue");
    }
}
