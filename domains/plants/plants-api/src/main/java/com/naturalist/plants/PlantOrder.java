package com.naturalist.plants;

import com.naturalist.ddd.NamedEntity;
import com.naturalist.fieldnotes.CommonName;
import com.naturalist.fieldnotes.Description;
import com.naturalist.observability.Constraints;
import com.naturalist.taxonomy.LinnaeanOrder;
import com.naturalist.taxonomy.TaxonomicOrder;

import java.util.Set;
import java.util.function.Consumer;

/**
 * A catalogued plant order — the top of the plant-side Linnaean chain, above
 * {@link PlantFamily}.
 * <p>
 * Order-rank records are first-class catalog citizens, not lookup rows. A naturalist who
 * recognises a Lamiales flower — square stem, bilabiate corolla — without resolving the
 * family has a permanent home for that observation here, and the record is never replaced
 * as identification firms; a {@code PlantFamily} is added alongside it.
 * <p>
 * Its practical job is anchoring the chain. Before this record existed, {@code PlantFamily}
 * carried a bare {@link TaxonomicOrder} epithet with nothing behind it and no constraint on
 * it — position expressed as a string, which is exactly the shape the species rung was
 * rescued from. With {@code PlantOrder} in place every rung references its parent, and
 * every rank fixture can declare a foreign key.
 * <p>
 * No {@code placedIn} component. Its insect counterpart carries a clade, but plants has no
 * clade permits in the kernel yet; when they arrive, this record and its siblings gain the
 * axis together.
 */
public record PlantOrder(
        PlantOrderName name,
        TaxonomicOrder order,
        Description description,
        Set<CommonName> commonNames
) implements NamedEntity<PlantOrderName>, LinnaeanOrder {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityName(name, "name")
                .namedValue(order, "order")
                .valueObject(description, "description")
                .notNull(commonNames, "commonNames");
    }
}
