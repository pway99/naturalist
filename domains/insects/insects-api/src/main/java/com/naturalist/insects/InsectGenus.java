package com.naturalist.insects;

import com.naturalist.clades.Clade;
import com.naturalist.ddd.NamedEntity;
import com.naturalist.fieldnotes.CommonName;
import com.naturalist.fieldnotes.Description;
import com.naturalist.insects.lifestage.AdultStage;
import com.naturalist.insects.lifestage.EggStage;
import com.naturalist.insects.lifestage.LarvaStage;
import com.naturalist.insects.lifestage.PupaStage;
import com.naturalist.observability.Constraints;
import com.naturalist.taxonomy.LinnaeanGenus;
import com.naturalist.taxonomy.TaxonomicFamily;
import com.naturalist.taxonomy.TaxonomicGenus;
import com.naturalist.taxonomy.TaxonomicOrder;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * A catalogued insect genus — the rank between {@link InsectFamily} and
 * {@link InsectSpecies} in the Linnaean hierarchy.
 * <p>
 * Genus-rank records are first-class catalog citizens — a naturalist who
 * recognises a {@code Halictus} sweat bee without resolving the species has
 * a permanent home for that observation here. As identification firms, an
 * {@link InsectSpecies} record is added alongside this genus record; the
 * genus record is never replaced or migrated.
 * <p>
 * The {@link #familyName} component is the upward typed reference to the
 * parent {@link InsectFamily}. The redundant {@link #family} epithet is
 * carried locally so a catalog-assembly chain check (this genus's
 * {@code family} epithet must equal its resolved parent family's
 * {@code family} epithet) does not require resolving the parent.
 * {@link #order} is similarly local for self-sufficient display without
 * resolving the family record.
 */
public record InsectGenus(
        InsectGenusName name,
        InsectFamilyName familyName,
        TaxonomicOrder order,
        TaxonomicFamily family,
        TaxonomicGenus genus,
        Description description,
        Set<CommonName> commonNames,
        @Nullable Clade placedIn,
        @Nullable EggStage egg,
        @Nullable LarvaStage larva,
        @Nullable PupaStage pupa,
        @Nullable AdultStage adult
) implements NamedEntity<InsectGenusName>, LinnaeanGenus<InsectFamilyName> {

    public Optional<Clade> placedInOptional() {
        return Optional.ofNullable(placedIn);
    }

    public InsectGenus withPlacedIn(@Nullable Clade value) {
        return new InsectGenus(name, familyName, order, family, genus,
                description, commonNames, value, egg, larva, pupa, adult);
    }

    public InsectGenus withEgg(@Nullable EggStage value) {
        return new InsectGenus(name, familyName, order, family, genus,
                description, commonNames, placedIn, value, larva, pupa, adult);
    }

    public InsectGenus withLarva(@Nullable LarvaStage value) {
        return new InsectGenus(name, familyName, order, family, genus,
                description, commonNames, placedIn, egg, value, pupa, adult);
    }

    public InsectGenus withPupa(@Nullable PupaStage value) {
        return new InsectGenus(name, familyName, order, family, genus,
                description, commonNames, placedIn, egg, larva, value, adult);
    }

    public InsectGenus withAdult(@Nullable AdultStage value) {
        return new InsectGenus(name, familyName, order, family, genus,
                description, commonNames, placedIn, egg, larva, pupa, value);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityName(name, "name")
                .entityName(familyName, "familyName")
                .namedValue(order, "order")
                .namedValue(family, "family")
                .namedValue(genus, "genus")
                .valueObject(description, "description")
                .notNull(commonNames, "commonNames")
                .namedEntityOrNull(egg, "egg")
                .namedEntityOrNull(larva, "larva")
                .namedEntityOrNull(pupa, "pupa")
                .namedEntityOrNull(adult, "adult");
    }
}
