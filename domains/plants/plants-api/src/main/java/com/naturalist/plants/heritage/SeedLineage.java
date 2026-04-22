package com.naturalist.plants.heritage;

import com.naturalist.ddd.NamedEntity;
import com.naturalist.fieldnotes.Description;
import com.naturalist.observability.Constraints;
import com.naturalist.plants.cultivar.CultivarName;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * The generational history and provenance of an irreplaceable seed-saved variety.
 * <p>
 * {@code SeedLineage} is a separate aggregate root from {@link com.naturalist.plants.cultivar.Cultivar}.
 * Not every cultivar has a lineage — only heritage varieties under active seed saving
 * programs. The lineage tracks who originated the selection, where, for how many
 * generations, and what criteria guide ongoing selection.
 * <p>
 * <b>Nick's Italian Pear</b> is the inaugural and highest-priority lineage in the
 * system. This paste tomato variety was maintained for 50+ years in coastal California,
 * selecting for eating quality and sauce performance. The 2026 season at
 * Oak Vista (Chico) is Generation 1 of the Chico climate adaptation program —
 * selecting for earliest ripening, highest production, and best flavour under
 * Sacramento Valley heat conditions (100-110F summer peaks).
 * <p>
 * Domain invariant: a {@code SeedLineage} must always reference a cultivar that is
 * {@link com.naturalist.plants.cultivar.VarietyType#OPEN_POLLINATED}. This constraint
 * is enforced at the service layer, not in the record invariants, because it requires
 * cross-entity validation.
 * <p>
 * {@link Provenance} captures the origin chain. {@code selectionCriteria} documents
 * what traits to select for when saving seed each season — this is the breeding
 * program encoded as domain knowledge.
 */
public record SeedLineage(
        SeedLineageName name,
        CultivarName cultivarName,
        Provenance provenance,
        Description description,
        int adaptationStartYear,
        @Nullable String selectionCriteria,
        @Nullable String notes
) implements NamedEntity<SeedLineageName> {

    /**
     * Whether this lineage has an active local adaptation program — selecting
     * seed each season for performance in the current growing environment.
     */
    public boolean hasActiveAdaptationProgram() {
        return adaptationStartYear > 0;
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityName(name, "name")
                .notNull(this, SeedLineage::cultivarName, "cultivarName")
                .valueObject(this, SeedLineage::provenance, "provenance")
                .notNull(this, SeedLineage::description, "description");
    }
}
