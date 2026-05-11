package com.naturalist.insects;

import com.naturalist.ddd.NamedEntity;
import com.naturalist.fieldnotes.CommonName;
import com.naturalist.fieldnotes.Description;
import com.naturalist.insects.lifestage.AdultStage;
import com.naturalist.insects.lifestage.EggStage;
import com.naturalist.insects.lifestage.LarvaStage;
import com.naturalist.insects.lifestage.PupaStage;
import com.naturalist.observability.Constraints;
import com.naturalist.taxonomy.LinnaeanFamily;
import com.naturalist.taxonomy.TaxonomicFamily;
import com.naturalist.taxonomy.TaxonomicOrder;
import org.jspecify.annotations.Nullable;

import java.util.Set;
import java.util.function.Consumer;

/**
 * A catalogued insect family — the rank between {@code TaxonomicOrder} and
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
 */
public record InsectFamily(
        InsectFamilyName name,
        TaxonomicOrder order,
        TaxonomicFamily family,
        Description description,
        Set<CommonName> commonNames,
        @Nullable EggStage egg,
        @Nullable LarvaStage larva,
        @Nullable PupaStage pupa,
        @Nullable AdultStage adult
) implements NamedEntity<InsectFamilyName>, LinnaeanFamily {

    public InsectFamily withEgg(@Nullable EggStage value) {
        return new InsectFamily(name, order, family, description, commonNames,
                value, larva, pupa, adult);
    }

    public InsectFamily withLarva(@Nullable LarvaStage value) {
        return new InsectFamily(name, order, family, description, commonNames,
                egg, value, pupa, adult);
    }

    public InsectFamily withPupa(@Nullable PupaStage value) {
        return new InsectFamily(name, order, family, description, commonNames,
                egg, larva, value, adult);
    }

    public InsectFamily withAdult(@Nullable AdultStage value) {
        return new InsectFamily(name, order, family, description, commonNames,
                egg, larva, pupa, value);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> {
            i.entityName(name, "name")
                    .namedValue(order, "order")
                    .namedValue(family, "family")
                    .valueObject(description, "description")
                    .notNull(commonNames, "commonNames");
            if (egg != null) i.namedEntity(this, InsectFamily::egg, "egg");
            if (larva != null) i.namedEntity(this, InsectFamily::larva, "larva");
            if (pupa != null) i.namedEntity(this, InsectFamily::pupa, "pupa");
            if (adult != null) i.namedEntity(this, InsectFamily::adult, "adult");
        };
    }
}
