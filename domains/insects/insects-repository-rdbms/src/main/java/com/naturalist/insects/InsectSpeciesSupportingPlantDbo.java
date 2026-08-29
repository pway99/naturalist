package com.naturalist.insects;

import com.naturalist.observability.Constraints;
import com.naturalist.observability.Observer;
import com.naturalist.persistence.Dbo;
import com.naturalist.persistence.DboSchema;
import com.naturalist.persistence.Fk;

import java.util.function.Consumer;

/**
 * Child row for one member of an {@link InsectSpecies}'s {@code GardenConnections.supportingPlants}
 * {@code List<String>}, ordered within its parent by {@code ordinal}. Stored reference is the numeric
 * {@code species_id} (FK-enforced); the DBO carries the species's {@code name} for the mapper's JOIN
 * projection (read) and nested-select (write). The list is order-significant, so the mapper reads
 * {@code ORDER BY ordinal} and the adapter writes the enumeration index. The presence of any row is
 * (together with either garden text column) what marks the {@code GardenConnections} group present.
 */
@DboSchema(table = "insect_species_supporting_plant", primaryKey = "species_id,ordinal",
           foreignKeys = @Fk(columns = "species_id", references = "insect_species(id)"),
           entity = InsectSpecies.class)
final class InsectSpeciesSupportingPlantDbo implements Dbo {
    String speciesName;  // JOIN projection (read) / nested-select key (write); not a stored column
    int ordinal;
    String plant;

    static InsectSpeciesSupportingPlantDbo from(InsectSpeciesName speciesName, int ordinal, String plant) {
        InsectSpeciesSupportingPlantDbo d = new InsectSpeciesSupportingPlantDbo();
        d.speciesName = speciesName.value();
        d.ordinal = ordinal;
        d.plant = plant;
        Observer.forClass(InsectSpeciesSupportingPlantDbo.class)
                .arguments("from", i -> i.observable(d, "dbo"))
                .throwWhenInvalid();
        return d;
    }

    String toPlant() {
        return plant;
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return c -> c
                .notNull(speciesName, "speciesName").kebabFormat(speciesName, "speciesName")
                .notBlank(plant, "plant");
    }
}
