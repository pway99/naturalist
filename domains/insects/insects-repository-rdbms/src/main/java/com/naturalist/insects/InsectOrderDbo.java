package com.naturalist.insects;

import com.naturalist.clades.Clade;
import com.naturalist.fieldnotes.CommonName;
import com.naturalist.fieldnotes.Description;
import com.naturalist.observability.Constraints;
import com.naturalist.observability.Observer;
import com.naturalist.persistence.Dbo;
import com.naturalist.persistence.DboSchema;
import com.naturalist.taxonomy.TaxonomicOrder;

import java.util.Set;
import java.util.function.Consumer;

/**
 * Parent row of the {@link InsectOrder} rank record — the top of the insect Linnaean chain within
 * Class Insecta, so it carries no upward foreign key. The 1:1 scalars ({@code taxonomic_order}, the
 * four-column Durrell {@link Description}) flatten onto columns here; the multi-valued
 * {@code commonNames} become the {@link InsectOrderCommonNameDbo} child table, loaded in one batched
 * query.
 *
 * <p>{@code placedIn} is a nullable {@code kernels/clades} slug ({@code placed_in}, no FK) — an order
 * of genuinely uncertain evolutionary placement stores {@code null} rather than a fabricated node, so
 * it is not validated. Fields are camelCase; MyBatis translates the snake_case columns across on read
 * ({@code taxonomic_order} → {@code taxonomicOrder}). {@code id} is a DB-generated identity, populated
 * on insert and never surfaced in the domain.
 */
@DboSchema(table = "insect_order", primaryKey = "id", unique = {"name"}, entity = InsectOrder.class)
final class InsectOrderDbo implements Dbo {
    Long id;              // numeric identity anchor (BIGINT IDENTITY); null before insert.
    String name;
    String taxonomicOrder;
    String placedIn;      // clade slug (kernels/clades), nullable
    String descriptionPreschool;
    String descriptionElementary;
    String descriptionSecondary;
    String descriptionUniversity;

    static InsectOrderDbo from(InsectOrder o) {
        InsectOrderDbo d = new InsectOrderDbo();
        d.name = o.name().value();
        d.taxonomicOrder = o.order().value();
        d.placedIn = o.placedIn() == null ? null : o.placedIn().slug();
        Description desc = o.description();
        d.descriptionPreschool = desc.preschool();
        d.descriptionElementary = desc.elementary();
        d.descriptionSecondary = desc.secondary();
        d.descriptionUniversity = desc.university();
        Observer.forClass(InsectOrderDbo.class)
                .arguments("from", i -> i.observable(d, "dbo"))
                .throwWhenInvalid();
        return d;
    }

    InsectOrder toEntity(Set<CommonName> commonNames) {
        return new InsectOrder(
                InsectOrderName.of(name),
                TaxonomicOrder.of(taxonomicOrder),
                new Description(
                        descriptionPreschool,
                        descriptionElementary,
                        descriptionSecondary,
                        descriptionUniversity),
                commonNames,
                placedIn == null ? null : Clade.of(placedIn));
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return c -> c
                .notNull(name, "name").kebabFormat(name, "name").maxLength(name, 64, "name")
                .notBlank(taxonomicOrder, "taxonomicOrder").maxLength(taxonomicOrder, 128, "taxonomicOrder")
                .notBlank(descriptionPreschool, "descriptionPreschool")
                .notBlank(descriptionElementary, "descriptionElementary")
                .notBlank(descriptionSecondary, "descriptionSecondary")
                .notBlank(descriptionUniversity, "descriptionUniversity");
    }
}
