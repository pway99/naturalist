package com.naturalist.plants;

import com.naturalist.ddd.NamedEntity;
import com.naturalist.fieldnotes.CommonName;
import com.naturalist.fieldnotes.Description;
import com.naturalist.observability.Constraints;
import com.naturalist.taxonomy.LinnaeanGenus;
import com.naturalist.taxonomy.TaxonomicFamily;
import com.naturalist.taxonomy.TaxonomicGenus;

import java.util.Set;
import java.util.function.Consumer;

/**
 * A catalogued plant genus — the rank between {@link PlantFamily} and
 * {@link PlantSpecies} in the Linnaean hierarchy.
 * <p>
 * Genus-rank records are first-class catalog citizens — a naturalist who
 * recognises a {@code Thymus} mat-forming herb without resolving the species
 * has a permanent home for that observation here. As identification firms, a
 * {@link PlantSpecies} record is added alongside this genus record; the genus record
 * is never replaced or migrated.
 * <p>
 * The {@link #familyName} component is the upward typed reference to the
 * parent {@link PlantFamily}. The redundant {@link #family} epithet is
 * carried locally so a catalog-assembly chain check (this genus's
 * {@code family} epithet must equal its resolved parent family's
 * {@code family} epithet) does not require resolving the parent.
 * The order is deliberately <em>not</em> carried here: it is two rungs up, which is a
 * copy rather than a chain check. Resolve it through the parent family.
 */
public record PlantGenus(
        PlantGenusName name,
        PlantFamilyName familyName,
        TaxonomicFamily family,
        TaxonomicGenus genus,
        Description description,
        Set<CommonName> commonNames
) implements NamedEntity<PlantGenusName>, LinnaeanGenus<PlantFamilyName> {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityName(name, "name")
                .entityName(familyName, "familyName")
                .namedValue(family, "family")
                .namedValue(genus, "genus")
                .valueObject(description, "description")
                .notNull(commonNames, "commonNames");
    }
}
