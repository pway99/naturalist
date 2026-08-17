package com.naturalist.plants;

import com.naturalist.clades.Clade;
import com.naturalist.ddd.NamedEntity;
import com.naturalist.fieldnotes.CommonName;
import com.naturalist.fieldnotes.Description;
import com.naturalist.observability.Constraints;
import com.naturalist.taxonomy.LinnaeanOrder;
import com.naturalist.taxonomy.TaxonomicOrder;
import org.jspecify.annotations.Nullable;

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
 * {@code placedIn} locates the order in the rank-free phylogenetic tree
 * ({@code kernels/clades}) — angiosperms → magnoliids / monocots / eudicots → … .
 * <b>Plants attaches the clade axis here and nowhere lower</b>, unlike insects, which
 * carries {@code placedIn} on every rank. Every mainstream botanical clade node is
 * supra-ordinal (above Order in APG IV), so a family, genus, or species resolves its
 * clade transitively by walking up to its order; a per-rank {@code placedIn} would only
 * replicate the order's value and invite drift. The reference is {@link Nullable}: an
 * order whose placement is genuinely uncertain carries {@code null} rather than a
 * fabricated node.
 */
public record PlantOrder(
        PlantOrderName name,
        TaxonomicOrder order,
        Description description,
        Set<CommonName> commonNames,
        @Nullable Clade placedIn
) implements NamedEntity<PlantOrderName>, LinnaeanOrder {

    public PlantOrder withPlacedIn(@Nullable Clade value) {
        return new PlantOrder(name, order, description, commonNames, value);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityName(name, "name")
                .namedValue(order, "order")
                .valueObject(description, "description")
                .notNull(commonNames, "commonNames");
    }
}
