package com.naturalist.plants;

import com.naturalist.fieldnotes.CommonName;
import com.naturalist.fieldnotes.Description;
import com.naturalist.observability.Constraints;
import com.naturalist.observability.Observer;
import com.naturalist.persistence.Dbo;
import com.naturalist.persistence.DboSchema;
import com.naturalist.persistence.Fk;
import com.naturalist.taxonomy.TaxonomicFamily;

import java.util.Set;
import java.util.function.Consumer;

/**
 * Parent row of the {@link PlantFamily} rank record. Its upward reference to the parent
 * {@link PlantOrder} is stored as the numeric {@code order_id} (FK-enforced); the DBO carries the
 * order's {@code name} in {@link #orderName} — the mapper JOINs {@code plant_order} to project it
 * on read and nested-selects {@code plant_order.id} from it on write, so the name never persists on
 * this table and cannot drift from the FK. The multi-valued {@code commonNames} become the
 * {@link PlantFamilyCommonNameDbo} child table, loaded in one batched query.
 *
 * <p>Fields are camelCase; MyBatis translates snake_case columns across on read
 * ({@code taxonomic_family} → {@code taxonomicFamily}). {@code id} is a DB-generated identity.
 */
@DboSchema(table = "plant_family", primaryKey = "id", unique = {"name"},
           foreignKeys = @Fk(columns = "order_id", references = "plant_order(id)"),
           entity = PlantFamily.class)
final class PlantFamilyDbo implements Dbo {
    Long id;              // numeric identity anchor (BIGINT IDENTITY); null before insert.
    String name;
    String orderName;     // JOIN projection (read) / nested-select key (write); stored as order_id
    String taxonomicFamily;
    String descriptionPreschool;
    String descriptionElementary;
    String descriptionSecondary;
    String descriptionUniversity;

    static PlantFamilyDbo from(PlantFamily f) {
        PlantFamilyDbo d = new PlantFamilyDbo();
        d.name = f.name().value();
        d.orderName = f.orderName().value();
        d.taxonomicFamily = f.family().value();
        Description desc = f.description();
        d.descriptionPreschool = desc.preschool();
        d.descriptionElementary = desc.elementary();
        d.descriptionSecondary = desc.secondary();
        d.descriptionUniversity = desc.university();
        Observer.forClass(PlantFamilyDbo.class)
                .arguments("from", i -> i.observable(d, "dbo"))
                .throwWhenInvalid();
        return d;
    }

    PlantFamily toEntity(Set<CommonName> commonNames) {
        return new PlantFamily(
                PlantFamilyName.of(name),
                PlantOrderName.of(orderName),
                TaxonomicFamily.of(taxonomicFamily),
                new Description(
                        descriptionPreschool,
                        descriptionElementary,
                        descriptionSecondary,
                        descriptionUniversity),
                commonNames);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return c -> c
                .notNull(name, "name").kebabFormat(name, "name").maxLength(name, 64, "name")
                .notNull(orderName, "orderName").kebabFormat(orderName, "orderName")
                .notBlank(taxonomicFamily, "taxonomicFamily").maxLength(taxonomicFamily, 128, "taxonomicFamily")
                .notBlank(descriptionPreschool, "descriptionPreschool")
                .notBlank(descriptionElementary, "descriptionElementary")
                .notBlank(descriptionSecondary, "descriptionSecondary")
                .notBlank(descriptionUniversity, "descriptionUniversity");
    }
}
