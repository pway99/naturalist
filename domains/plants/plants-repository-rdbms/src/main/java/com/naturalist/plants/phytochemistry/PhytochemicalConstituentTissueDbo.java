package com.naturalist.plants.phytochemistry;

import com.naturalist.observability.Constraints;
import com.naturalist.observability.Observer;
import com.naturalist.persistence.Dbo;
import com.naturalist.persistence.DboSchema;
import com.naturalist.persistence.Fk;

import java.util.function.Consumer;

/**
 * Child row for one member of a constituent's {@code Set<PlantTissue>}. The stored reference is
 * the numeric {@code constituent_id} (FK-enforced); the DBO carries the constituent's {@code name}
 * for the mapper's JOIN projection (read) and nested-select (write). {@link PlantTissue} is an
 * enum persisted by its constant name.
 */
@DboSchema(table = "phytochemical_constituent_tissue", primaryKey = "constituent_id,tissue",
           foreignKeys = @Fk(columns = "constituent_id", references = "phytochemical_constituent(id)"),
           entity = PhytochemicalConstituent.class)
final class PhytochemicalConstituentTissueDbo implements Dbo {
    String constituentName;  // JOIN projection (read) / nested-select key (write); not a stored column
    String tissue;

    static PhytochemicalConstituentTissueDbo from(PhytochemicalConstituentName constituentName, PlantTissue tissue) {
        PhytochemicalConstituentTissueDbo d = new PhytochemicalConstituentTissueDbo();
        d.constituentName = constituentName.value();
        d.tissue = tissue.name();
        Observer.forClass(PhytochemicalConstituentTissueDbo.class)
                .arguments("from", i -> i.observable(d, "dbo"))
                .throwWhenInvalid();
        return d;
    }

    PlantTissue toTissue() {
        return PlantTissue.valueOf(tissue);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return c -> c
                .notNull(constituentName, "constituentName").kebabFormat(constituentName, "constituentName")
                .notBlank(tissue, "tissue").maxLength(tissue, 32, "tissue");
    }
}
