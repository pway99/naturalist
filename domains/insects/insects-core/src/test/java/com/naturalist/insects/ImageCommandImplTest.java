package com.naturalist.insects;

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

class ImageCommandImplTest
        implements EntityCommandContractTest<InsectImageId, InsectImage, ImageCollection> {

    @RegisterExtension
    NaturalistDatabaseExtension db = NaturalistDatabaseExtension.create();

    InsectImageRepositoryMock repository = new InsectImageRepositoryMock(db);
    FamilyRepositoryMock familyRepository = new FamilyRepositoryMock(db);
    GenusRepositoryMock genusRepository = new GenusRepositoryMock(db);
    SpeciesRepositoryMock speciesRepository = new SpeciesRepositoryMock(db);
    InsectQuery.FamilyQuery familyQuery = new FamilyQueryImpl(familyRepository);
    InsectQuery.GenusQuery genusQuery = new GenusQueryImpl(genusRepository, familyQuery);
    InsectQuery.SpeciesQuery speciesQuery = new SpeciesQueryImpl(speciesRepository, genusQuery);
    InsectCommand.ImageCommand command = new ImageCommandImpl(repository);
    InsectQuery.ImageQuery query = new ImageQueryImpl(
            repository, speciesQuery, genusQuery, familyQuery);

    @Override
    public EntityCommand<InsectImageId, InsectImage> command() {
        return command;
    }

    @Override
    public EntityQuery<InsectImageId, InsectImage, ImageCollection> query() {
        return query;
    }

    @Override
    public TestEntitySource<InsectImageId, InsectImage> source() {
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
    public InsectImage newEntity() {
        return new InsectImage(
                InsectImageId.create(),
                TestInsectsIdentifiers.InsectGenus.Empoasca.name,
                Instant.parse("2026-04-20T12:00:00Z"),
                FileName.of("IMG_TEST_NEW.HEIC"),
                null);
    }

    @Override
    public InsectImage ghostEntity() {
        return new InsectImage(
                InsectImageId.create(),
                TestInsectsIdentifiers.InsectGenus.Empoasca.name,
                Instant.parse("2026-04-21T12:00:00Z"),
                FileName.of("IMG_TEST_GHOST.HEIC"),
                null);
    }

    @Override
    public InsectImage modifiedEntity(InsectImage original) {
        return new InsectImage(
                original.id(),
                TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name,
                Instant.parse("2026-05-01T08:30:00Z"),
                FileName.of("IMG_TEST_MODIFIED.HEIC"),
                null);
    }

    @org.junit.jupiter.api.Test
    void image_withObservationId_roundTrips() {
        InsectObservationId obs = TestInsectsIdentifiers.Observation.PatrickBattus;
        InsectImage img = new InsectImage(
                InsectImageId.create(),
                TestInsectsIdentifiers.InsectGenus.Empoasca.name,
                java.time.Instant.parse("2026-06-20T08:00:00Z"),
                com.naturalist.data.FileName.of("IMG_OBS.HEIC"),
                obs);
        command.insert(img);
        InsectImage found = query.getByName(img.id()).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(found.observationId()).isEqualTo(obs);
    }
}
