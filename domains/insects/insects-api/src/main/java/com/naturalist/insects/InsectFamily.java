package com.naturalist.insects;

import com.naturalist.clades.Clade;
import com.naturalist.ddd.NamedEntity;
import com.naturalist.fieldnotes.CommonName;
import com.naturalist.fieldnotes.Description;
import com.naturalist.observability.Constraints;
import com.naturalist.taxonomy.LinnaeanFamily;
import com.naturalist.taxonomy.TaxonomicFamily;
import org.jspecify.annotations.Nullable;

import java.util.Set;
import java.util.function.Consumer;

/**
 * A catalogued insect family — the rank between {@link InsectOrder} and
 * {@link com.naturalist.insects.InsectSpecies} in the Linnaean hierarchy.
 * <p>
 * Family-rank records are first-class catalog citizens, not placeholders for
 * unfinished species identifications. A naturalist who recognises a tachinid
 * fly without resolving the genus has a permanent home for that observation
 * here. As identification firms, an {@code InsectGenus} record is added
 * alongside this family record, and eventually an {@code InsectSpecies} record
 * alongside that — the family record is never replaced or migrated.
 * <p>
 * Each family carries the four-level Durrell {@link Description}, locale-tagged
 * {@link CommonName}s for findability, and the family's place in the Linnaean
 * order/family hierarchy. The slug identity is derived from the family epithet
 * via {@link LinnaeanFamily#familySlug()}; vernacular names live in
 * {@code commonNames} and are findable but not authoritative.
 * <p>
 * Per-stage data lives on the {@link com.naturalist.insects.lifestage.LifeStage}
 * records keyed by {@code (name, stageKind)}, queried via
 * {@link com.naturalist.insects.lifestage.InsectLifeStageQuery}.
 */
public record InsectFamily(
        InsectFamilyName name,
        InsectOrderName orderName,
        TaxonomicFamily family,
        Description description,
        Set<CommonName> commonNames,
        @Nullable Clade placedIn
) implements NamedEntity<InsectFamilyName>, LinnaeanFamily<InsectOrderName> {

    public InsectFamily withPlacedIn(@Nullable Clade value) {
        return new InsectFamily(name, orderName, family, description, commonNames,
                value);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityName(name, "name")
                .entityName(orderName, "orderName")
                .namedValue(family, "family")
                .valueObject(description, "description")
                .notNull(commonNames, "commonNames");
    }
}
