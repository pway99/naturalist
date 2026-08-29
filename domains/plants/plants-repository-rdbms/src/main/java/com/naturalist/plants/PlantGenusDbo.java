package com.naturalist.plants;

import com.naturalist.fieldnotes.CommonName;
import com.naturalist.fieldnotes.Description;
import com.naturalist.observability.Constraints;
import com.naturalist.observability.Observer;
import com.naturalist.persistence.Dbo;
import com.naturalist.persistence.DboSchema;
import com.naturalist.persistence.Fk;
import com.naturalist.taxonomy.TaxonomicFamily;
import com.naturalist.taxonomy.TaxonomicGenus;

import java.util.Set;
import java.util.function.Consumer;

/**
 * Parent row of the {@link PlantGenus} rank record. The upward reference to the parent
 * {@link PlantFamily} is stored as {@code family_id} (FK-enforced); the DBO carries the family's
 * {@code name} in {@link #familyName} for the mapper's JOIN projection (read) and nested-select
 * (write). Separately, the record carries a locally-redundant {@link TaxonomicFamily} epithet —
 * the {@code taxonomic_family} column — so a catalog chain check need not resolve the parent; that
 * is a distinct value from {@link #familyName} (the parent's slug). The {@code commonNames} become
 * the {@link PlantGenusCommonNameDbo} child table.
 */
@DboSchema(table = "plant_genus", primaryKey = "id", unique = {"name"},
           foreignKeys = @Fk(columns = "family_id", references = "plant_family(id)"),
           entity = PlantGenus.class)
final class PlantGenusDbo implements Dbo {
    Long id;              // numeric identity anchor (BIGINT IDENTITY); null before insert.
    String name;
    String familyName;    // JOIN projection (read) / nested-select key (write); stored as family_id
    String taxonomicFamily;
    String taxonomicGenus;
    String descriptionPreschool;
    String descriptionElementary;
    String descriptionSecondary;
    String descriptionUniversity;

    static PlantGenusDbo from(PlantGenus g) {
        PlantGenusDbo d = new PlantGenusDbo();
        d.name = g.name().value();
        d.familyName = g.familyName().value();
        d.taxonomicFamily = g.family().value();
        d.taxonomicGenus = g.genus().value();
        Description desc = g.description();
        d.descriptionPreschool = desc.preschool();
        d.descriptionElementary = desc.elementary();
        d.descriptionSecondary = desc.secondary();
        d.descriptionUniversity = desc.university();
        Observer.forClass(PlantGenusDbo.class)
                .arguments("from", i -> i.observable(d, "dbo"))
                .throwWhenInvalid();
        return d;
    }

    PlantGenus toEntity(Set<CommonName> commonNames) {
        return new PlantGenus(
                PlantGenusName.of(name),
                PlantFamilyName.of(familyName),
                TaxonomicFamily.of(taxonomicFamily),
                TaxonomicGenus.of(taxonomicGenus),
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
                .notNull(familyName, "familyName").kebabFormat(familyName, "familyName")
                .notBlank(taxonomicFamily, "taxonomicFamily").maxLength(taxonomicFamily, 128, "taxonomicFamily")
                .notBlank(taxonomicGenus, "taxonomicGenus").maxLength(taxonomicGenus, 128, "taxonomicGenus")
                .notBlank(descriptionPreschool, "descriptionPreschool")
                .notBlank(descriptionElementary, "descriptionElementary")
                .notBlank(descriptionSecondary, "descriptionSecondary")
                .notBlank(descriptionUniversity, "descriptionUniversity");
    }
}
