package com.naturalist.plants;

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
 * Parent row of the {@link PlantOrder} rank record — the top of the plant Linnaean chain, so
 * it carries no upward foreign key. The 1:1 scalars ({@code taxonomic_order}, the four-column
 * Durrell {@link Description}) flatten onto columns here; the multi-valued {@code commonNames}
 * become the {@link PlantOrderCommonNameDbo} child table, loaded in one batched query.
 *
 * <p>{@code placedIn} is a nullable {@code kernels/clades} slug ({@code placed_in}, no FK) — an
 * order of genuinely uncertain placement stores {@code null} rather than a fabricated node.
 * Fields are camelCase; MyBatis translates the snake_case columns across on read
 * ({@code taxonomic_order} → {@code taxonomicOrder}). {@code id} is a DB-generated identity,
 * populated on insert and never surfaced in the domain.
 */
@DboSchema(table = "plant_order", primaryKey = "id", unique = {"name"}, entity = PlantOrder.class)
final class PlantOrderDbo implements Dbo {
    Long id;              // numeric identity anchor (BIGINT IDENTITY); null before insert.
    String name;
    String taxonomicOrder;
    String placedIn;      // clade slug (kernels/clades), nullable
    String descriptionPreschool;
    String descriptionElementary;
    String descriptionSecondary;
    String descriptionUniversity;

    static PlantOrderDbo from(PlantOrder o) {
        PlantOrderDbo d = new PlantOrderDbo();
        d.name = o.name().value();
        d.taxonomicOrder = o.order().value();
        d.placedIn = o.placedIn() == null ? null : o.placedIn().slug();
        Description desc = o.description();
        d.descriptionPreschool = desc.preschool();
        d.descriptionElementary = desc.elementary();
        d.descriptionSecondary = desc.secondary();
        d.descriptionUniversity = desc.university();
        Observer.forClass(PlantOrderDbo.class)
                .arguments("from", i -> i.observable(d, "dbo"))
                .throwWhenInvalid();
        return d;
    }

    PlantOrder toEntity(Set<CommonName> commonNames) {
        return new PlantOrder(
                PlantOrderName.of(name),
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
