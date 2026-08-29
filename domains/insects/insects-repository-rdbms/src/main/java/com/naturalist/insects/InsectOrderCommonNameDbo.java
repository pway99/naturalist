package com.naturalist.insects;

import com.naturalist.fieldnotes.CommonName;
import com.naturalist.observability.Constraints;
import com.naturalist.observability.Observer;
import com.naturalist.persistence.Dbo;
import com.naturalist.persistence.DboSchema;
import com.naturalist.persistence.Fk;

import java.util.Locale;
import java.util.function.Consumer;

/**
 * Child row for one member of an {@link InsectOrder}'s {@code Set<CommonName>}. The stored reference
 * is the numeric {@code order_id} (FK-enforced); the DBO carries the order's {@code name} — the
 * mapper JOINs {@code insect_order} to project it on read and nested-selects {@code insect_order.id}
 * from it on write, so no name is persisted here and nothing can drift. {@link CommonName}'s
 * {@link Locale} round-trips as its BCP-47 language tag.
 */
@DboSchema(table = "insect_order_common_name", primaryKey = "order_id,label,locale",
           foreignKeys = @Fk(columns = "order_id", references = "insect_order(id)"),
           entity = InsectOrder.class)
final class InsectOrderCommonNameDbo implements Dbo {
    String orderName;  // JOIN projection (read) / nested-select key (write); not a stored column
    String label;
    String locale;

    static InsectOrderCommonNameDbo from(InsectOrderName orderName, CommonName commonName) {
        InsectOrderCommonNameDbo d = new InsectOrderCommonNameDbo();
        d.orderName = orderName.value();
        d.label = commonName.label();
        d.locale = commonName.locale().toLanguageTag();
        Observer.forClass(InsectOrderCommonNameDbo.class)
                .arguments("from", i -> i.observable(d, "dbo"))
                .throwWhenInvalid();
        return d;
    }

    CommonName toCommonName() {
        return CommonName.of(label, Locale.forLanguageTag(locale));
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return c -> c
                .notNull(orderName, "orderName").kebabFormat(orderName, "orderName")
                .notBlank(label, "label").maxLength(label, 128, "label")
                .notBlank(locale, "locale").maxLength(locale, 35, "locale");
    }
}
