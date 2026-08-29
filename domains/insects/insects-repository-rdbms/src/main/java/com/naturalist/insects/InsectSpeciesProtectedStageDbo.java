package com.naturalist.insects;

import com.naturalist.observability.Constraints;
import com.naturalist.observability.Observer;
import com.naturalist.persistence.Dbo;
import com.naturalist.persistence.DboSchema;
import com.naturalist.persistence.Fk;

import java.util.function.Consumer;

/**
 * Child row for one member of an {@link InsectSpecies}'s {@code ChemicalDefense.protectedStages}
 * {@code Set<LifeStageKind>}. Stored reference is the numeric {@code species_id} (FK-enforced); the
 * DBO carries the species's {@code name} for the mapper's JOIN projection (read) and nested-select
 * (write). {@link LifeStageKind} round-trips as its {@code name()} / {@code valueOf}.
 */
@DboSchema(table = "insect_species_protected_stage", primaryKey = "species_id,stage_kind",
           foreignKeys = @Fk(columns = "species_id", references = "insect_species(id)"),
           entity = InsectSpecies.class)
final class InsectSpeciesProtectedStageDbo implements Dbo {
    String speciesName;  // JOIN projection (read) / nested-select key (write); not a stored column
    String stageKind;

    static InsectSpeciesProtectedStageDbo from(InsectSpeciesName speciesName, LifeStageKind stage) {
        InsectSpeciesProtectedStageDbo d = new InsectSpeciesProtectedStageDbo();
        d.speciesName = speciesName.value();
        d.stageKind = stage.name();
        Observer.forClass(InsectSpeciesProtectedStageDbo.class)
                .arguments("from", i -> i.observable(d, "dbo"))
                .throwWhenInvalid();
        return d;
    }

    LifeStageKind toStageKind() {
        return LifeStageKind.valueOf(stageKind);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return c -> c
                .notNull(speciesName, "speciesName").kebabFormat(speciesName, "speciesName")
                .notBlank(stageKind, "stageKind").maxLength(stageKind, 16, "stageKind");
    }
}
