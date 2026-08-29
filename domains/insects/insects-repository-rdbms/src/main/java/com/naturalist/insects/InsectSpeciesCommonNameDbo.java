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
 * Child row for one member of an {@link InsectSpecies}'s {@code Set<CommonName>}. Stored reference is
 * the numeric {@code species_id} (FK-enforced); the DBO carries the species's {@code name} for the
 * mapper's JOIN projection (read) and nested-select (write). {@link CommonName}'s {@link Locale}
 * round-trips as its BCP-47 language tag.
 */
@DboSchema(table = "insect_species_common_name", primaryKey = "species_id,label,locale",
           foreignKeys = @Fk(columns = "species_id", references = "insect_species(id)"),
           entity = InsectSpecies.class)
final class InsectSpeciesCommonNameDbo implements Dbo {
    String speciesName;  // JOIN projection (read) / nested-select key (write); not a stored column
    String label;
    String locale;

    static InsectSpeciesCommonNameDbo from(InsectSpeciesName speciesName, CommonName commonName) {
        InsectSpeciesCommonNameDbo d = new InsectSpeciesCommonNameDbo();
        d.speciesName = speciesName.value();
        d.label = commonName.label();
        d.locale = commonName.locale().toLanguageTag();
        Observer.forClass(InsectSpeciesCommonNameDbo.class)
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
                .notNull(speciesName, "speciesName").kebabFormat(speciesName, "speciesName")
                .notBlank(label, "label").maxLength(label, 128, "label")
                .notBlank(locale, "locale").maxLength(locale, 35, "locale");
    }
}
