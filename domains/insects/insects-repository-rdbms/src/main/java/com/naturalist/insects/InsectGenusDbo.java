package com.naturalist.insects;

import com.naturalist.clades.Clade;
import com.naturalist.fieldnotes.CommonName;
import com.naturalist.fieldnotes.Description;
import com.naturalist.observability.Constraints;
import com.naturalist.observability.Observer;
import com.naturalist.persistence.Dbo;
import com.naturalist.persistence.DboSchema;
import com.naturalist.persistence.Fk;
import com.naturalist.taxonomy.TaxonomicGenus;

import java.util.Set;
import java.util.function.Consumer;

/**
 * Parent row of the {@link InsectGenus} rank record. The upward reference to the parent
 * {@link InsectFamily} is stored as {@code family_id} (FK-enforced); the DBO carries the family's
 * {@code name} in {@link #familyName} for the mapper's JOIN projection (read) and nested-select
 * (write). Unlike the plant genus, the insect genus carries no redundant {@code taxonomic_family}
 * epithet — only its own {@link TaxonomicGenus} in the {@code taxonomic_genus} column; the order is
 * reached by resolving the parent family. The {@code commonNames} become the
 * {@link InsectGenusCommonNameDbo} child table.
 *
 * <p>{@code placedIn} is a nullable {@code kernels/clades} slug ({@code placed_in}, no FK) and is not
 * validated. {@code id} is a DB-generated identity.
 */
@DboSchema(table = "insect_genus", primaryKey = "id", unique = {"name"},
           foreignKeys = @Fk(columns = "family_id", references = "insect_family(id)"),
           entity = InsectGenus.class)
final class InsectGenusDbo implements Dbo {
    Long id;              // numeric identity anchor (BIGINT IDENTITY); null before insert.
    String name;
    String familyName;    // JOIN projection (read) / nested-select key (write); stored as family_id
    String taxonomicGenus;
    String placedIn;      // clade slug (kernels/clades), nullable
    String descriptionPreschool;
    String descriptionElementary;
    String descriptionSecondary;
    String descriptionUniversity;

    static InsectGenusDbo from(InsectGenus g) {
        InsectGenusDbo d = new InsectGenusDbo();
        d.name = g.name().value();
        d.familyName = g.familyName().value();
        d.taxonomicGenus = g.genus().value();
        d.placedIn = g.placedIn() == null ? null : g.placedIn().slug();
        Description desc = g.description();
        d.descriptionPreschool = desc.preschool();
        d.descriptionElementary = desc.elementary();
        d.descriptionSecondary = desc.secondary();
        d.descriptionUniversity = desc.university();
        Observer.forClass(InsectGenusDbo.class)
                .arguments("from", i -> i.observable(d, "dbo"))
                .throwWhenInvalid();
        return d;
    }

    InsectGenus toEntity(Set<CommonName> commonNames) {
        return new InsectGenus(
                InsectGenusName.of(name),
                InsectFamilyName.of(familyName),
                TaxonomicGenus.of(taxonomicGenus),
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
                .notNull(familyName, "familyName").kebabFormat(familyName, "familyName")
                .notBlank(taxonomicGenus, "taxonomicGenus").maxLength(taxonomicGenus, 128, "taxonomicGenus")
                .notBlank(descriptionPreschool, "descriptionPreschool")
                .notBlank(descriptionElementary, "descriptionElementary")
                .notBlank(descriptionSecondary, "descriptionSecondary")
                .notBlank(descriptionUniversity, "descriptionUniversity");
    }
}
