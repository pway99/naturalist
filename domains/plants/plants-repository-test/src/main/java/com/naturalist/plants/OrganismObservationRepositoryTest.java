package com.naturalist.plants;

import com.naturalist.data.EntityRepositoryTest;
import com.naturalist.data.TestEntitySource;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.naturalist.NaturalistName;
import com.naturalist.observation.OrganismObservation;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Behavioral contract for {@link PlantRepository.FieldObservationRepository}.
 * Mirrors the insects field-observation contract, adapted to plant ranks.
 */
interface OrganismObservationRepositoryTest
        extends EntityRepositoryTest<PlantObservationId, OrganismObservation<PlantObservationId, PlantRankName>> {

    NaturalistName PATRICK = NaturalistName.of("patrick-way");
    NaturalistName DELIA = NaturalistName.of("delia-durrell");

    @Override
    PlantRepository.FieldObservationRepository repository();

    @Override
    default TestEntitySource<PlantObservationId, OrganismObservation<PlantObservationId, PlantRankName>> source() {
        return db.getNamed(OrganismObservationTestEntitySource.class);
    }

    @Override
    default PlantObservationId notFoundName() {
        return TestPlantsIdentifiers.FieldObservations.NotFound.id;
    }

    @Override
    default List<PlantObservationId> knownEntityNames() {
        return List.of(
                TestPlantsIdentifiers.FieldObservations.PatrickTomato,
                TestPlantsIdentifiers.FieldObservations.DeliaTomato);
    }

    @Override
    default OrganismObservation<PlantObservationId, PlantRankName> newEntity() {
        return new OrganismObservation<>(
                PlantObservationId.create(),
                PATRICK,
                TestPlantsIdentifiers.PlantGenera.Trifolium.name,
                Instant.parse("2026-07-10T09:00:00Z"),
                "new observation",
                null,
                null);
    }

    @Override
    default OrganismObservation<PlantObservationId, PlantRankName> ghostEntity() {
        return new OrganismObservation<>(
                PlantObservationId.create(),
                PATRICK,
                TestPlantsIdentifiers.PlantGenera.Trifolium.name,
                Instant.parse("2026-07-11T09:00:00Z"),
                null,
                null,
                null);
    }

    @Override
    default OrganismObservation<PlantObservationId, PlantRankName> modifiedEntity(
            OrganismObservation<PlantObservationId, PlantRankName> original) {
        return new OrganismObservation<>(
                original.id(),
                DELIA,
                PlantSpeciesName.of("solanum-lycopersicum"),
                Instant.parse("2026-07-12T09:00:00Z"),
                "changed",
                "north bed",
                null);
    }

    @Test
    default void getByNaturalist_rejectsNull() {
        assertThatThrownBy(() -> repository().getByNaturalist(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("observedBy");
    }

    @Test
    default void getByNaturalist_returnsOnlyThatNaturalistsObservations() {
        var results = repository().getByNaturalist(PATRICK);
        assertThat(results).isNotEmpty();
        assertThat(results).allMatch(o -> o.observedBy().equals(PATRICK));
    }

    @Test
    default void getByNaturalistAndSubjects_matchesNaturalistAndSubject() {
        var results = repository().getByNaturalistAndSubjects(
                PATRICK, Set.of(PlantSpeciesName.of("solanum-lycopersicum")));
        assertThat(results).isNotEmpty();
        assertThat(results).allMatch(o -> o.observedBy().equals(PATRICK)
                && o.subject().equals(PlantSpeciesName.of("solanum-lycopersicum")));
    }

    @Test
    default void getByNaturalistAndSubjects_excludesOtherNaturalists() {
        // delia also observed solanum-lycopersicum; patrick's query must not return hers.
        var results = repository().getByNaturalistAndSubjects(
                PATRICK, Set.of(PlantSpeciesName.of("solanum-lycopersicum")));
        assertThat(results).noneMatch(o -> o.observedBy().equals(DELIA));
    }

    @Test
    default void getByNaturalistAndSubjects_rejectsNullNaturalist() {
        assertThatThrownBy(() -> repository().getByNaturalistAndSubjects(null, Set.of()))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("observedBy");
    }
}
