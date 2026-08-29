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
 * Child row for one member of an {@link InsectGenus}'s {@code Set<CommonName>}. Stored reference is
 * the numeric {@code genus_id} (FK-enforced); the DBO carries the genus's {@code name} for the
 * mapper's JOIN projection (read) and nested-select (write). {@link CommonName}'s {@link Locale}
 * round-trips as its BCP-47 language tag.
 */
@DboSchema(table = "insect_genus_common_name", primaryKey = "genus_id,label,locale",
           foreignKeys = @Fk(columns = "genus_id", references = "insect_genus(id)"),
           entity = InsectGenus.class)
final class InsectGenusCommonNameDbo implements Dbo {
    String genusName;  // JOIN projection (read) / nested-select key (write); not a stored column
    String label;
    String locale;

    static InsectGenusCommonNameDbo from(InsectGenusName genusName, CommonName commonName) {
        InsectGenusCommonNameDbo d = new InsectGenusCommonNameDbo();
        d.genusName = genusName.value();
        d.label = commonName.label();
        d.locale = commonName.locale().toLanguageTag();
        Observer.forClass(InsectGenusCommonNameDbo.class)
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
                .notNull(genusName, "genusName").kebabFormat(genusName, "genusName")
                .notBlank(label, "label").maxLength(label, 128, "label")
                .notBlank(locale, "locale").maxLength(locale, 35, "locale");
    }
}
