package com.naturalist.plants;

import com.naturalist.fieldnotes.CommonName;
import com.naturalist.observability.Constraints;
import com.naturalist.observability.Observer;
import com.naturalist.persistence.Dbo;
import com.naturalist.persistence.DboSchema;
import com.naturalist.persistence.Fk;

import java.util.Locale;
import java.util.function.Consumer;

/**
 * Child row for one member of a {@link PlantFamily}'s {@code Set<CommonName>}. Stored reference is
 * the numeric {@code family_id} (FK-enforced); the DBO carries the family's {@code name} for the
 * mapper's JOIN projection (read) and nested-select (write). {@link CommonName}'s {@link Locale}
 * round-trips as its BCP-47 language tag.
 */
@DboSchema(table = "plant_family_common_name", primaryKey = "family_id,label,locale",
           foreignKeys = @Fk(columns = "family_id", references = "plant_family(id)"),
           entity = PlantFamily.class)
final class PlantFamilyCommonNameDbo implements Dbo {
    String familyName;  // JOIN projection (read) / nested-select key (write); not a stored column
    String label;
    String locale;

    static PlantFamilyCommonNameDbo from(PlantFamilyName familyName, CommonName commonName) {
        PlantFamilyCommonNameDbo d = new PlantFamilyCommonNameDbo();
        d.familyName = familyName.value();
        d.label = commonName.label();
        d.locale = commonName.locale().toLanguageTag();
        Observer.forClass(PlantFamilyCommonNameDbo.class)
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
                .notNull(familyName, "familyName").kebabFormat(familyName, "familyName")
                .notBlank(label, "label").maxLength(label, 128, "label")
                .notBlank(locale, "locale").maxLength(locale, 35, "locale");
    }
}
