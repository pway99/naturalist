package com.naturalist.insects;

import com.naturalist.data.EntityCommand;
import com.naturalist.data.EntityCommandContractTest;
import com.naturalist.data.EntityQuery;
import com.naturalist.data.NaturalistDatabaseExtension;
import com.naturalist.data.TestEntitySource;
import com.naturalist.insects.InsectEntityCollections.FieldObservationCollection;
import com.naturalist.naturalist.NaturalistName;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.Instant;
import java.util.List;

class FieldObservationCommandImplTest
        implements EntityCommandContractTest<FieldObservationId, FieldObservation, FieldObservationCollection> {

    @RegisterExtension
    NaturalistDatabaseExtension db = NaturalistDatabaseExtension.create();

    FieldObservationRepositoryMock repository = new FieldObservationRepositoryMock(db);
    InsectCommand.FieldObservationCommand command = new FieldObservationCommandImpl(repository);
    InsectQuery.FieldObservationQuery query = new FieldObservationQueryImpl(repository);

    @Override
    public EntityCommand<FieldObservationId, FieldObservation> command() {
        return command;
    }

    @Override
    public EntityQuery<FieldObservationId, FieldObservation, FieldObservationCollection> query() {
        return query;
    }

    @Override
    public TestEntitySource<FieldObservationId, FieldObservation> source() {
        return db.getNamed(FieldObservationTestEntitySource.class);
    }

    @Override
    public FieldObservationId notFoundName() {
        return TestInsectsIdentifiers.FieldObservation.NotFound.id;
    }

    @Override
    public List<FieldObservationId> knownEntityNames() {
        return List.of(
                TestInsectsIdentifiers.FieldObservation.PatrickBattus,
                TestInsectsIdentifiers.FieldObservation.PatrickEmpoasca);
    }

    @Override
    public FieldObservation newEntity() {
        return new FieldObservation(
                FieldObservationId.create(),
                NaturalistName.of("patrick-way"),
                TestInsectsIdentifiers.InsectGenus.Empoasca.name,
                Instant.parse("2026-06-10T08:00:00Z"),
                "new",
                null, null);
    }

    @Override
    public FieldObservation ghostEntity() {
        return new FieldObservation(
                FieldObservationId.create(),
                NaturalistName.of("patrick-way"),
                TestInsectsIdentifiers.InsectGenus.Empoasca.name,
                Instant.parse("2026-06-11T08:00:00Z"),
                null,
                null, null);
    }

    @Override
    public FieldObservation modifiedEntity(FieldObservation original) {
        return new FieldObservation(
                original.id(),
                NaturalistName.of("delia-durrell"),
                TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name,
                Instant.parse("2026-06-12T08:00:00Z"),
                "changed",
                "Deer Creek, Butte County, CA",
                new Identification(0.91, "changed evidence",
                        List.of(new Identification.Candidate(
                                "Papilio zelicaon", "Anise Swallowtail", 0.08))));
    }
}
