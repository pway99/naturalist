package com.naturalist.insects;

import com.naturalist.observation.OrganismImage;

import com.naturalist.data.EntityCommand;
import com.naturalist.data.EntityCommandContractTest;
import com.naturalist.data.EntityQuery;
import com.naturalist.data.FileName;
import com.naturalist.data.NaturalistDatabaseExtension;
import com.naturalist.data.TestEntitySource;
import com.naturalist.insects.InsectEntityCollections.ImageCollection;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.Instant;
import java.util.List;

class InsectImageCommandImplTest
        implements EntityCommandContractTest<InsectImageId, OrganismImage<InsectImageId, InsectObservationId, InsectRankName>, ImageCollection> {

    @RegisterExtension
    NaturalistDatabaseExtension db = NaturalistDatabaseExtension.create();

    InsectImageRepositoryMock repository = new InsectImageRepositoryMock(db);
    InsectFamilyRepositoryMock familyRepository = new InsectFamilyRepositoryMock(db);
    InsectGenusRepositoryMock genusRepository = new InsectGenusRepositoryMock(db);
    InsectSpeciesRepositoryMock speciesRepository = new InsectSpeciesRepositoryMock(db);
    InsectQuery.FamilyQuery familyQuery = new InsectFamilyQueryImpl(familyRepository);
    InsectQuery.GenusQuery genusQuery = new InsectGenusQueryImpl(genusRepository, familyQuery);
    InsectQuery.SpeciesQuery speciesQuery = new InsectSpeciesQueryImpl(speciesRepository, genusQuery);
    InsectCommand.ImageCommand command = new InsectImageCommandImpl(repository);
    InsectQuery.ImageQuery query = new InsectImageQueryImpl(
            repository, speciesQuery, genusQuery, familyQuery);

    @Override
    public EntityCommand<InsectImageId, OrganismImage<InsectImageId, InsectObservationId, InsectRankName>> command() {
        return command;
    }

    @Override
    public EntityQuery<InsectImageId, OrganismImage<InsectImageId, InsectObservationId, InsectRankName>, ImageCollection> query() {
        return query;
    }

    @Override
    public TestEntitySource<InsectImageId, OrganismImage<InsectImageId, InsectObservationId, InsectRankName>> source() {
        return db.getNamed(InsectImageTestEntitySource.class);
    }

    @Override
    public InsectImageId notFoundName() {
        return TestInsectsIdentifiers.InsectSpecies.NotFound.imageId;
    }

    @Override
    public List<InsectImageId> knownEntityNames() {
        return List.of(
                TestInsectsIdentifiers.InsectGenus.Empoasca.Images.Img9047.id,
                TestInsectsIdentifiers.InsectGenus.Empoasca.Images.Img9048.id);
    }

    @Override
    public OrganismImage<InsectImageId, InsectObservationId, InsectRankName> newEntity() {
        return new OrganismImage<InsectImageId, InsectObservationId, InsectRankName>(
                InsectImageId.create(),
                TestInsectsIdentifiers.InsectGenus.Empoasca.name,
                Instant.parse("2026-04-20T12:00:00Z"),
                FileName.of("IMG_TEST_NEW.HEIC"),
                null);
    }

    @Override
    public OrganismImage<InsectImageId, InsectObservationId, InsectRankName> ghostEntity() {
        return new OrganismImage<InsectImageId, InsectObservationId, InsectRankName>(
                InsectImageId.create(),
                TestInsectsIdentifiers.InsectGenus.Empoasca.name,
                Instant.parse("2026-04-21T12:00:00Z"),
                FileName.of("IMG_TEST_GHOST.HEIC"),
                null);
    }

    @Override
    public OrganismImage<InsectImageId, InsectObservationId, InsectRankName> modifiedEntity(OrganismImage<InsectImageId, InsectObservationId, InsectRankName> original) {
        return new OrganismImage<InsectImageId, InsectObservationId, InsectRankName>(
                original.id(),
                TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name,
                Instant.parse("2026-05-01T08:30:00Z"),
                FileName.of("IMG_TEST_MODIFIED.HEIC"),
                null);
    }

    @org.junit.jupiter.api.Test
    void image_withObservationId_roundTrips() {
        InsectObservationId obs = TestInsectsIdentifiers.Observation.PatrickBattus;
        OrganismImage<InsectImageId, InsectObservationId, InsectRankName> img = new OrganismImage<InsectImageId, InsectObservationId, InsectRankName>(
                InsectImageId.create(),
                TestInsectsIdentifiers.InsectGenus.Empoasca.name,
                java.time.Instant.parse("2026-06-20T08:00:00Z"),
                com.naturalist.data.FileName.of("IMG_OBS.HEIC"),
                obs);
        command.insert(img);
        OrganismImage<InsectImageId, InsectObservationId, InsectRankName> found = query.getByName(img.id()).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(found.observationId()).isEqualTo(obs);
    }
}
