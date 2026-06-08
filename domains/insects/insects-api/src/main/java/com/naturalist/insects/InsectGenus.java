package com.naturalist.insects;

import com.naturalist.clades.Clade;
import com.naturalist.ddd.NamedEntity;
import com.naturalist.fieldnotes.CommonName;
import com.naturalist.fieldnotes.Description;
import com.naturalist.observability.Constraints;
import com.naturalist.taxonomy.LinnaeanGenus;
import com.naturalist.taxonomy.TaxonomicGenus;
import org.jspecify.annotations.Nullable;

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
 * parent {@link InsectFamily}. When the order is needed for display, the
 * consumer resolves the parent family entity — the genus carries only the
 * direct family FK.
 * <p>
 * Per-stage data lives on the {@link com.naturalist.insects.lifestage.LifeStage}
 * records keyed by {@code (name, stageKind)}, queried via
 * {@link com.naturalist.insects.lifestage.InsectLifeStageQuery}.
 */
public record InsectGenus(
        InsectGenusName name,
        InsectFamilyName familyName,
        TaxonomicGenus genus,
        Description description,
        Set<CommonName> commonNames,
        @Nullable Clade placedIn
) implements NamedEntity<InsectGenusName>, LinnaeanGenus<InsectFamilyName> {

    public InsectGenus withPlacedIn(@Nullable Clade value) {
        return new InsectGenus(name, familyName, genus, description, commonNames, value);
    }

    /** True iff this genus's family FK equals the given family name. */
    public boolean belongsToFamily(InsectFamilyName familyName) {
        return this.familyName.equals(familyName);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityName(name, "name")
                .entityName(familyName, "familyName")
                .namedValue(genus, "genus")
                .valueObject(description, "description")
                .notNull(commonNames, "commonNames");
    }
}
