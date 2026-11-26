package com.naturalist.plants;

import com.naturalist.data.NaturalistTestExtension;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.test.context.Wired;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class PlantQueryFindByNameTest {

    @RegisterExtension
    final NaturalistTestExtension nte = NaturalistTestExtension.create();

    @Wired
    PlantQuery plantQuery;

    private SimpleMeterRegistry meters;

    @BeforeEach
    void addMeters() {
        meters = new SimpleMeterRegistry();
        Metrics.addRegistry(meters);
    }

    @AfterEach
    void removeMeters() {
        Metrics.removeRegistry(meters);
    }

    @Test
    void findByName_knownSpecies_isExact() {
        PlantMatch match = plantQuery.findByName(PlantSpeciesName.of("aristolochia-californica")).orElseThrow();
        assertThat(match.exact()).isTrue();
        assertThat(match.found()).isEqualTo(PlantSpeciesName.of("aristolochia-californica"));
        assertThat(match.plant().hasSpecies()).isTrue();
    }

    @Test
    void findByName_missingSpecies_answersItsGenus() {
        PlantMatch match = plantQuery.findByName(PlantSpeciesName.of("aristolochia-tomentosa")).orElseThrow();
        assertThat(match.exact()).isFalse();
        assertThat(match.asked()).isEqualTo(PlantSpeciesName.of("aristolochia-tomentosa"));
        assertThat(match.found()).isEqualTo(PlantGenusName.of("aristolochia"));
        assertThat(match.plant().hasGenus()).isTrue();
        assertThat(match.plant().hasSpecies()).isFalse();
    }

    @Test
    void findByName_missingSpeciesAndGenus_isEmpty() {
        assertThat(plantQuery.findByName(PlantSpeciesName.of("unobtainium-vine"))).isEmpty();
    }

    @Test
    void findByName_speciesSlugWithoutEpithet_triesTheWholeSlugAsAGenus() {
        assertThat(plantQuery.findByName(PlantSpeciesName.of("aristolochia")).orElseThrow().found())
                .isEqualTo(PlantGenusName.of("aristolochia"));
    }

    @Test
    void findByName_missingGenus_isEmpty_noClimb() {
        assertThat(plantQuery.findByName(PlantGenusName.of("unobtainium"))).isEmpty();
    }

    @Test
    void findByName_fallback_isCountedAtInfo() {
        plantQuery.findByName(PlantSpeciesName.of("aristolochia-tomentosa"));

        Counter counter = meters.find("naturalist.invariant.check")
                .tag("name", "PlantQueryImpl.findByName.exact").tag("outcome", "violated").counter();
        assertThat(counter.count()).isEqualTo(1.0);
        assertThat(counter.getId().getTag("level")).isEqualTo("info");
    }

    @Test
    void findByName_null_throws() {
        assertThat(catchThrowable(() -> plantQuery.findByName(null)))
                .isInstanceOf(InvariantViolationException.class);
    }

    @Test
    void getByName_missingSpecies_staysEmpty() {
        assertThat(plantQuery.getByName(PlantSpeciesName.of("aristolochia-tomentosa"))).isEmpty();
    }
}
