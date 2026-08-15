package com.naturalist.plants.heritage;

import com.naturalist.RandomValue;
import com.naturalist.fieldnotes.Description;
import com.naturalist.observability.InvariantObservation;
import com.naturalist.observability.MethodObserver;
import com.naturalist.observability.Observer;
import com.naturalist.plants.cultivar.CultivarName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SeedLineageTest {

    private static final Observer observer = Observer.forClass(SeedLineageTest.class);

    @Test
    void fullyPopulatedLineageIsValid() {
        MethodObserver mo = observer.forMethod("fullyPopulatedLineageIsValid");
        SeedLineage lineage = lineage(2026);

        InvariantObservation result = mo.namedEntity(lineage, "lineage");

        assertThat(result.violations()).isEmpty();
    }

    @Test
    void allNullComponentsReportEveryViolation() {
        MethodObserver mo = observer.forMethod("allNullComponentsReportEveryViolation");
        SeedLineage lineage = new SeedLineage(null, null, null, null, 0, null, null);

        InvariantObservation result = mo.namedEntity(lineage, "lineage");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .containsExactlyInAnyOrder(
                        ".lineage.name",
                        ".lineage.cultivarName",
                        ".lineage.provenance",
                        ".lineage.description");
    }

    @Test
    void adaptationStartYearZeroMeansNoActiveProgram() {
        // 0 is the encoding for "no active adaptation program", not a missing
        // value — hasActiveAdaptationProgram() is the read surface for it.
        assertThat(lineage(0).hasActiveAdaptationProgram()).isFalse();
        assertThat(lineage(2026).hasActiveAdaptationProgram()).isTrue();
    }

    private static SeedLineage lineage(int adaptationStartYear) {
        return new SeedLineage(
                SeedLineageName.of("italian-pear-nicks"),
                CultivarName.of("italian-pear-nicks"),
                new Provenance("Nick's family", "Calabria, Italy", 50, null),
                description(),
                adaptationStartYear,
                "earliest ripening, best flavour under heat",
                null);
    }

    private static Description description() {
        return new Description(
                RandomValue.string(), RandomValue.string(),
                RandomValue.string(), RandomValue.string());
    }
}
