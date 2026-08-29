package com.naturalist.insects;

import com.naturalist.habitat.VerticalLayer;
import com.naturalist.observability.Constraints;
import com.naturalist.observability.Observer;
import com.naturalist.persistence.Dbo;
import com.naturalist.persistence.DboSchema;
import com.naturalist.persistence.Fk;

import java.util.function.Consumer;

/**
 * Child row for one member of an {@link InsectSpecies}'s {@code HabitatProfile.layers}
 * {@code @Nullable Set<VerticalLayer>}. Stored reference is the numeric {@code species_id}
 * (FK-enforced); the DBO carries the species's {@code name} for the mapper's JOIN projection (read)
 * and nested-select (write). {@link VerticalLayer} round-trips as its {@code name()} /
 * {@code valueOf}. Zero layer rows reconstruct as a {@code null} {@code layers} set (not-yet-
 * characterised), distinct from the required non-empty {@code zones}.
 */
@DboSchema(table = "insect_species_habitat_layer", primaryKey = "species_id,layer",
           foreignKeys = @Fk(columns = "species_id", references = "insect_species(id)"),
           entity = InsectSpecies.class)
final class InsectSpeciesHabitatLayerDbo implements Dbo {
    String speciesName;  // JOIN projection (read) / nested-select key (write); not a stored column
    String layer;

    static InsectSpeciesHabitatLayerDbo from(InsectSpeciesName speciesName, VerticalLayer layer) {
        InsectSpeciesHabitatLayerDbo d = new InsectSpeciesHabitatLayerDbo();
        d.speciesName = speciesName.value();
        d.layer = layer.name();
        Observer.forClass(InsectSpeciesHabitatLayerDbo.class)
                .arguments("from", i -> i.observable(d, "dbo"))
                .throwWhenInvalid();
        return d;
    }

    VerticalLayer toLayer() {
        return VerticalLayer.valueOf(layer);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return c -> c
                .notNull(speciesName, "speciesName").kebabFormat(speciesName, "speciesName")
                .notBlank(layer, "layer").maxLength(layer, 24, "layer");
    }
}
