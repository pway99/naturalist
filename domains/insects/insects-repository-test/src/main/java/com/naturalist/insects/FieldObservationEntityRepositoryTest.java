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

interface FieldObservationEntityRepositoryTest
        extends EntityRepositoryTest<FieldObservationId, FieldObservation> {

    NaturalistName PATRICK = NaturalistName.of("patrick-way");
    NaturalistName DELIA = NaturalistName.of("delia-durrell");

    @Override
    InsectRepository.FieldObservationRepository repository();

    @Override
    default TestEntitySource<FieldObservationId, FieldObservation> source() {
        return db.getNamed(FieldObservationTestEntitySource.class);
    }

    @Override
    default FieldObservationId notFoundName() {
        return TestInsectsIdentifiers.FieldObservation.NotFound.id;
    }

    @Override
    default List<FieldObservationId> knownEntityNames() {
        return List.of(
                TestInsectsIdentifiers.FieldObservation.PatrickBattus,
                TestInsectsIdentifiers.FieldObservation.DeliaBattus);
    }

    @Override
    default FieldObservation newEntity() {
        return new FieldObservation(
                FieldObservationId.create(),
                PATRICK,
                TestInsectsIdentifiers.InsectGenus.Empoasca.name,
                Instant.parse("2026-06-01T08:00:00Z"),
                "new observation",
                null, null);
    }

    @Override
    default FieldObservation ghostEntity() {
        return new FieldObservation(
                FieldObservationId.create(),
                PATRICK,
                TestInsectsIdentifiers.InsectGenus.Empoasca.name,
                Instant.parse("2026-06-02T08:00:00Z"),
                null,
                null, null);
    }

    @Override
    default FieldObservation modifiedEntity(FieldObservation original) {
        return new FieldObservation(
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
