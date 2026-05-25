package com.naturalist.insects;

import com.naturalist.clades.Clade;
import com.naturalist.ddd.NamedEntity;
import com.naturalist.fieldnotes.CommonName;
import com.naturalist.fieldnotes.Description;
import com.naturalist.insects.lifestage.AdultStage;
import com.naturalist.insects.lifestage.EggStage;
import com.naturalist.insects.lifestage.LarvaStage;
import com.naturalist.insects.lifestage.PupaStage;
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
 */
public record InsectOrder(
        InsectOrderName name,
        TaxonomicOrder order,
        Description description,
        Set<CommonName> commonNames,
        @Nullable Clade placedIn,
        @Nullable EggStage egg,
        @Nullable LarvaStage larva,
        @Nullable PupaStage pupa,
        @Nullable AdultStage adult
) implements NamedEntity<InsectOrderName>, LinnaeanOrder {

    public InsectOrder withPlacedIn(@Nullable Clade value) {
        return new InsectOrder(name, order, description, commonNames,
                value, egg, larva, pupa, adult);
    }

    public InsectOrder withEgg(@Nullable EggStage value) {
        return new InsectOrder(name, order, description, commonNames,
                placedIn, value, larva, pupa, adult);
    }

    public InsectOrder withLarva(@Nullable LarvaStage value) {
        return new InsectOrder(name, order, description, commonNames,
                placedIn, egg, value, pupa, adult);
    }

    public InsectOrder withPupa(@Nullable PupaStage value) {
        return new InsectOrder(name, order, description, commonNames,
                placedIn, egg, larva, value, adult);
    }

    public InsectOrder withAdult(@Nullable AdultStage value) {
        return new InsectOrder(name, order, description, commonNames,
                placedIn, egg, larva, pupa, value);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityName(name, "name")
                .namedValue(order, "order")
                .valueObject(description, "description")
                .notNull(commonNames, "commonNames")
                .namedEntityOrNull(egg, "egg")
                .namedEntityOrNull(larva, "larva")
                .namedEntityOrNull(pupa, "pupa")
                .namedEntityOrNull(adult, "adult");
    }
}
