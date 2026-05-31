package com.naturalist.insects;

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
 * A catalogued insect order — the topmost rank within Class Insecta.
 * <p>
 * Order-rank records are first-class catalog citizens. A naturalist who
 * recognises a dipteran without resolving the family has a permanent home
 * for that observation here. As identification firms, an {@link InsectFamily}
 * record is added alongside this order record; the order record is never
 * replaced or migrated.
 * <p>
 * Order is the root of the hierarchy within the insects domain — it carries
 * no parent FK. {@link #order} is the proper-cased Linnaean epithet
 * (e.g., {@code "Diptera"}). The slug identity is derived mechanically
 * from the epithet via {@link LinnaeanOrder#orderSlug()}.
 * <p>
 * Per-stage data lives on the {@link com.naturalist.insects.lifestage.LifeStage}
 * records keyed by {@code (name, stageKind)}, queried via
 * {@link com.naturalist.insects.lifestage.InsectLifeStageQuery}.
 */
public record InsectOrder(
        InsectOrderName name,
        TaxonomicOrder order,
        Description description,
        Set<CommonName> commonNames,
        @Nullable Clade placedIn
) implements NamedEntity<InsectOrderName>, LinnaeanOrder {

    public InsectOrder withPlacedIn(@Nullable Clade value) {
        return new InsectOrder(name, order, description, commonNames,
                value);
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
