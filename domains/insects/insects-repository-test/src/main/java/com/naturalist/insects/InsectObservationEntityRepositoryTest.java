package com.naturalist.insects;

import com.naturalist.data.EntityRepositoryTest;
import com.naturalist.data.TestEntitySource;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.naturalist.NaturalistName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.naturalist.observation.OrganismObservation;
import com.naturalist.observation.Identification;

interface InsectObservationEntityRepositoryTest
        extends EntityRepositoryTest<InsectObservationId, OrganismObservation<InsectObservationId, InsectRankName>> {

    NaturalistName PATRICK = NaturalistName.of("patrick-way");
    NaturalistName DELIA = NaturalistName.of("delia-durrell");

    @Override
    InsectRepository.InsectObservationRepository repository();

    @Override
    default TestEntitySource<InsectObservationId, OrganismObservation<InsectObservationId, InsectRankName>> source() {
        return db.getNamed(InsectObservationTestEntitySource.class);
    }

    @Override
    default InsectObservationId notFoundName() {
        return TestInsectsIdentifiers.Observation.NotFound.id;
    }

    @Override
    default List<InsectObservationId> knownEntityNames() {
        return List.of(
                TestInsectsIdentifiers.Observation.PatrickBattus,
                TestInsectsIdentifiers.Observation.DeliaBattus);
    }

    @Override
    default OrganismObservation<InsectObservationId, InsectRankName> newEntity() {
        return new OrganismObservation<InsectObservationId, InsectRankName>(
                InsectObservationId.create(),
                PATRICK,
                TestInsectsIdentifiers.InsectGenus.Empoasca.name,
                Instant.parse("2026-06-01T08:00:00Z"),
                "new observation",
                null, null);
    }

    @Override
    default OrganismObservation<InsectObservationId, InsectRankName> ghostEntity() {
        return new OrganismObservation<InsectObservationId, InsectRankName>(
                InsectObservationId.create(),
                PATRICK,
                TestInsectsIdentifiers.InsectGenus.Empoasca.name,
                Instant.parse("2026-06-02T08:00:00Z"),
                null,
                null, null);
    }

    @Override
    default OrganismObservation<InsectObservationId, InsectRankName> modifiedEntity(OrganismObservation<InsectObservationId, InsectRankName> original) {
        return new OrganismObservation<InsectObservationId, InsectRankName>(
                original.id(),
                DELIA,
                TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name,
                Instant.parse("2026-06-03T08:00:00Z"),
                "changed",
                "Oak Vista, Chico, CA",
                new Identification(0.77, "changed evidence",
                        List.of(new Identification.Candidate(
                                "Papilio rutulus", "Western Tiger Swallowtail", 0.15))));
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
                PATRICK, java.util.Set.of(TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name));
        assertThat(results).isNotEmpty();
        assertThat(results).allMatch(o -> o.observedBy().equals(PATRICK)
                && o.subject().equals(TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name));
    }

    @Test
    default void getByNaturalistAndSubjects_excludesOtherNaturalists() {
        // delia also observed battus-philenor; patrick's query must not return it.
        var results = repository().getByNaturalistAndSubjects(
                PATRICK, java.util.Set.of(TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name));
        assertThat(results).noneMatch(o -> o.observedBy().equals(DELIA));
    }

    @Test
    default void getByNaturalistAndSubjects_rejectsNullNaturalist() {
        assertThatThrownBy(() -> repository().getByNaturalistAndSubjects(null, java.util.Set.of()))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("observedBy");
    }
}
