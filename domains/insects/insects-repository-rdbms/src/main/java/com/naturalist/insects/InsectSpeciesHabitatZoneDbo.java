package com.naturalist.insects;

import com.naturalist.habitat.HabitatZone;
import com.naturalist.observability.Constraints;
import com.naturalist.observability.Observer;
import com.naturalist.persistence.Dbo;
import com.naturalist.persistence.DboSchema;
import com.naturalist.persistence.Fk;

import java.util.function.Consumer;

/**
 * Child row for one member of an {@link InsectSpecies}'s {@code HabitatProfile.zones}
 * {@code Set<HabitatZone>}. Stored reference is the numeric {@code species_id} (FK-enforced); the DBO
 * carries the species's {@code name} for the mapper's JOIN projection (read) and nested-select
 * (write). {@link HabitatZone} round-trips as its {@code name()} / {@code valueOf}. The presence of
 * any zone row is what marks the whole {@code HabitatProfile} group present on read.
 */
@DboSchema(table = "insect_species_habitat_zone", primaryKey = "species_id,zone",
           foreignKeys = @Fk(columns = "species_id", references = "insect_species(id)"),
           entity = InsectSpecies.class)
final class InsectSpeciesHabitatZoneDbo implements Dbo {
    String speciesName;  // JOIN projection (read) / nested-select key (write); not a stored column
    String zone;

    static InsectSpeciesHabitatZoneDbo from(InsectSpeciesName speciesName, HabitatZone zone) {
        InsectSpeciesHabitatZoneDbo d = new InsectSpeciesHabitatZoneDbo();
        d.speciesName = speciesName.value();
        d.zone = zone.name();
        Observer.forClass(InsectSpeciesHabitatZoneDbo.class)
                .arguments("from", i -> i.observable(d, "dbo"))
                .throwWhenInvalid();
        return d;
    }

    HabitatZone toZone() {
        return HabitatZone.valueOf(zone);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return c -> c
                .notNull(speciesName, "speciesName").kebabFormat(speciesName, "speciesName")
                .notBlank(zone, "zone").maxLength(zone, 24, "zone");
    }
}
