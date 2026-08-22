package com.naturalist.insects;

import com.naturalist.data.EntityCommand;
import com.naturalist.data.EntityCommandContractTest;
import com.naturalist.data.EntityQuery;
import com.naturalist.data.NaturalistTestExtension;
import com.naturalist.data.TestEntitySource;
import com.naturalist.insects.InsectEntityCollections.ObservationCollection;
import com.naturalist.naturalist.NaturalistName;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.Instant;
import java.util.List;
import com.naturalist.observation.OrganismObservation;
import com.naturalist.observation.Identification;

class InsectObservationCommandImplTest
        implements EntityCommandContractTest<InsectObservationId, OrganismObservation<InsectObservationId, InsectRankName>, ObservationCollection> {

    @RegisterExtension
    NaturalistTestExtension db = NaturalistTestExtension.create();

    InsectObservationRepositoryMock repository = new InsectObservationRepositoryMock(db);
    InsectCommand.ObservationCommand command = new InsectObservationCommandImpl(repository);
    InsectQuery.ObservationQuery query = new InsectObservationQueryImpl(repository);

    @Override
    public EntityCommand<InsectObservationId, OrganismObservation<InsectObservationId, InsectRankName>> command() {
        return command;
    }

    @Override
    public EntityQuery<InsectObservationId, OrganismObservation<InsectObservationId, InsectRankName>, ObservationCollection> query() {
        return query;
    }

    @Override
    public TestEntitySource<InsectObservationId, OrganismObservation<InsectObservationId, InsectRankName>> source() {
        return db.getNamed(InsectObservationTestEntitySource.class);
    }

    @Override
    public InsectObservationId notFoundName() {
        return TestInsectsIdentifiers.Observation.NotFound.id;
    }

    @Override
    public List<InsectObservationId> knownEntityNames() {
        return List.of(
                TestInsectsIdentifiers.Observation.PatrickBattus,
                TestInsectsIdentifiers.Observation.PatrickEmpoasca);
    }

    @Override
    public OrganismObservation<InsectObservationId, InsectRankName> newEntity() {
        return new OrganismObservation<InsectObservationId, InsectRankName>(
                InsectObservationId.create(),
                NaturalistName.of("patrick-way"),
                TestInsectsIdentifiers.InsectGenus.Empoasca.name,
                Instant.parse("2026-06-10T08:00:00Z"),
                "new",
                null, null);
    }

    @Override
    public OrganismObservation<InsectObservationId, InsectRankName> ghostEntity() {
        return new OrganismObservation<InsectObservationId, InsectRankName>(
                InsectObservationId.create(),
                NaturalistName.of("patrick-way"),
                TestInsectsIdentifiers.InsectGenus.Empoasca.name,
                Instant.parse("2026-06-11T08:00:00Z"),
                null,
                null, null);
    }

    @Override
    public OrganismObservation<InsectObservationId, InsectRankName> modifiedEntity(OrganismObservation<InsectObservationId, InsectRankName> original) {
        return new OrganismObservation<InsectObservationId, InsectRankName>(
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
