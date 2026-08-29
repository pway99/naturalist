package com.naturalist.plants;

import com.naturalist.biogeography.Bioregion;
import com.naturalist.observability.Constraints;
import com.naturalist.observability.Observer;
import com.naturalist.persistence.Dbo;
import com.naturalist.persistence.DboSchema;
import com.naturalist.persistence.Fk;

import java.util.function.Consumer;

/**
 * Child row for one member of a {@link PlantSpecies}'s {@code Set<Bioregion>}. Stored reference is
 * the numeric {@code species_id} (FK-enforced); the DBO carries the species's {@code name} for the
 * mapper's JOIN projection (read) and nested-select (write). {@link Bioregion} is a
 * {@code kernels/biogeography} sealed vocabulary persisted as its {@code slug()} — no FK on the
 * slug column, and {@link Bioregion#of(String)} throws on an unknown slug at reconstruction.
 */
@DboSchema(table = "plant_species_native_bioregion", primaryKey = "species_id,bioregion",
           foreignKeys = @Fk(columns = "species_id", references = "plant_species(id)"),
           entity = PlantSpecies.class)
final class PlantSpeciesNativeBioregionDbo implements Dbo {
    String speciesName;  // JOIN projection (read) / nested-select key (write); not a stored column
    String bioregion;    // biogeography slug (no FK)

    static PlantSpeciesNativeBioregionDbo from(PlantSpeciesName speciesName, Bioregion bioregion) {
        PlantSpeciesNativeBioregionDbo d = new PlantSpeciesNativeBioregionDbo();
        d.speciesName = speciesName.value();
        d.bioregion = bioregion.slug();
        Observer.forClass(PlantSpeciesNativeBioregionDbo.class)
                .arguments("from", i -> i.observable(d, "dbo"))
                .throwWhenInvalid();
        return d;
    }

    Bioregion toBioregion() {
        return Bioregion.of(bioregion);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return c -> c
                .notNull(speciesName, "speciesName").kebabFormat(speciesName, "speciesName")
                .notBlank(bioregion, "bioregion").maxLength(bioregion, 64, "bioregion");
    }
}
