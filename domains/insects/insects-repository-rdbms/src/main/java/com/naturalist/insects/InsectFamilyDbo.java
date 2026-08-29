package com.naturalist.insects;

import com.naturalist.clades.Clade;
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
 * Parent row of the {@link InsectFamily} rank record. Its upward reference to the parent
 * {@link InsectOrder} is stored as the numeric {@code order_id} (FK-enforced); the DBO carries the
 * order's {@code name} in {@link #orderName} — the mapper JOINs {@code insect_order} to project it on
 * read and nested-selects {@code insect_order.id} from it on write, so the name never persists on this
 * table and cannot drift from the FK. The multi-valued {@code commonNames} become the
 * {@link InsectFamilyCommonNameDbo} child table, loaded in one batched query.
 *
 * <p>{@code placedIn} is a nullable {@code kernels/clades} slug ({@code placed_in}, no FK) and is not
 * validated. Fields are camelCase; MyBatis translates snake_case columns across on read
 * ({@code taxonomic_family} → {@code taxonomicFamily}). {@code id} is a DB-generated identity.
 */
@DboSchema(table = "insect_family", primaryKey = "id", unique = {"name"},
           foreignKeys = @Fk(columns = "order_id", references = "insect_order(id)"),
           entity = InsectFamily.class)
final class InsectFamilyDbo implements Dbo {
    Long id;              // numeric identity anchor (BIGINT IDENTITY); null before insert.
    String name;
    String orderName;     // JOIN projection (read) / nested-select key (write); stored as order_id
    String taxonomicFamily;
    String placedIn;      // clade slug (kernels/clades), nullable
    String descriptionPreschool;
    String descriptionElementary;
    String descriptionSecondary;
    String descriptionUniversity;

    static InsectFamilyDbo from(InsectFamily f) {
        InsectFamilyDbo d = new InsectFamilyDbo();
        d.name = f.name().value();
        d.orderName = f.orderName().value();
        d.taxonomicFamily = f.family().value();
        d.placedIn = f.placedIn() == null ? null : f.placedIn().slug();
        Description desc = f.description();
        d.descriptionPreschool = desc.preschool();
        d.descriptionElementary = desc.elementary();
        d.descriptionSecondary = desc.secondary();
        d.descriptionUniversity = desc.university();
        Observer.forClass(InsectFamilyDbo.class)
                .arguments("from", i -> i.observable(d, "dbo"))
                .throwWhenInvalid();
        return d;
    }

    InsectFamily toEntity(Set<CommonName> commonNames) {
        return new InsectFamily(
                InsectFamilyName.of(name),
                InsectOrderName.of(orderName),
                TaxonomicFamily.of(taxonomicFamily),
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
                .notNull(orderName, "orderName").kebabFormat(orderName, "orderName")
                .notBlank(taxonomicFamily, "taxonomicFamily").maxLength(taxonomicFamily, 128, "taxonomicFamily")
                .notBlank(descriptionPreschool, "descriptionPreschool")
                .notBlank(descriptionElementary, "descriptionElementary")
                .notBlank(descriptionSecondary, "descriptionSecondary")
                .notBlank(descriptionUniversity, "descriptionUniversity");
    }
}
