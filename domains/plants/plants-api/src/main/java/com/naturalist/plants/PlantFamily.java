package com.naturalist.plants;

import com.naturalist.ddd.NamedEntity;
import com.naturalist.fieldnotes.CommonName;
import com.naturalist.fieldnotes.Description;
import com.naturalist.observability.Constraints;
import com.naturalist.taxonomy.TaxonomicFamily;
import com.naturalist.taxonomy.TaxonomicOrder;

import java.util.Set;
import java.util.function.Consumer;

/**
 * A catalogued plant family — the rank between {@link com.naturalist.taxonomy.TaxonomicOrder}
 * and {@link com.naturalist.plants.PlantGenus} in the Linnaean hierarchy.
 * <p>
 * Family-rank records are first-class catalog citizens, not placeholders for
 * unfinished species identifications. A naturalist who recognises a Lamiaceae
 * herb without resolving the genus has a permanent home for that observation
 * here. As identification firms, a {@code PlantGenus} record is added alongside
 * this family record, and eventually a {@link Plant} record alongside that —
 * the family record is never replaced or migrated.
 * <p>
 * Each family carries the four-level Durrell {@link Description}, locale-tagged
 * {@link CommonName}s for findability, and the family's place in the Linnaean
 * order/family hierarchy. The slug identity is derived from the family epithet;
 * vernacular names live in
 * {@code commonNames} and are findable but not authoritative.
 */
public record PlantFamily(
        PlantFamilyName name,
        TaxonomicOrder order,
        TaxonomicFamily family,
        Description description,
        Set<CommonName> commonNames
) implements NamedEntity<PlantFamilyName> {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityName(name, "name")
                .namedValue(order, "order")
                .namedValue(family, "family")
                .valueObject(description, "description")
                .notNull(commonNames, "commonNames");
    }
}
