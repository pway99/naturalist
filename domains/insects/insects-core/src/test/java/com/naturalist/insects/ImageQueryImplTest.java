package com.naturalist.insects;

import com.naturalist.data.EntityQuery;
import com.naturalist.data.EntityQueryContractTest;
import com.naturalist.data.NaturalistDatabaseExtension;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.insects.InsectEntityCollections.ImageCollection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImageQueryImplTest
        implements EntityQueryContractTest<InsectImageId, InsectImage, ImageCollection> {

    @RegisterExtension
    NaturalistDatabaseExtension db = NaturalistDatabaseExtension.create();

    InsectImageRepositoryMock repository = new InsectImageRepositoryMock(db);
    FamilyRepositoryMock familyRepository = new FamilyRepositoryMock(db);
    GenusRepositoryMock genusRepository = new GenusRepositoryMock(db);
    SpeciesRepositoryMock speciesRepository = new SpeciesRepositoryMock(db);
    InsectQuery.FamilyQuery familyQuery = new FamilyQueryImpl(familyRepository);
    InsectQuery.GenusQuery genusQuery = new GenusQueryImpl(genusRepository, familyQuery);
    InsectQuery.SpeciesQuery speciesQuery = new SpeciesQueryImpl(speciesRepository, genusQuery);
    InsectQuery.ImageQuery query = new ImageQueryImpl(
            repository, speciesQuery, genusQuery, familyQuery);

    @Override
    public EntityQuery<InsectImageId, InsectImage, ImageCollection> query() {
        return query;
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

    @Test
    void forParentName_returnsAllImagesForThatGenus() {
        ImageCollection collection =
                query.forParentName(TestInsectsIdentifiers.InsectGenus.Empoasca.name);

        assertThat(collection.size()).isGreaterThanOrEqualTo(2);
        assertThat(collection.stream())
                .allMatch(image -> image.parentName()
                        .equals(TestInsectsIdentifiers.InsectGenus.Empoasca.name));
        assertThat(collection.stream().map(InsectImage::id))
                .contains(
                        TestInsectsIdentifiers.InsectGenus.Empoasca.Images.Img9047.id,
                        TestInsectsIdentifiers.InsectGenus.Empoasca.Images.Img9048.id);
    }

    @Test
    void forParentName_speciesWithNoImages_returnsEmpty() {
        ImageCollection collection =
                query.forParentName(TestInsectsIdentifiers.InsectSpecies.HippodamiaConvergens.name);

        assertThat(collection).isNotNull();
        assertThat(collection.isEmpty()).isTrue();
    }

    @Test
    void forParentName_unknownSpecies_returnsEmpty() {
        ImageCollection collection =
                query.forParentName(TestInsectsIdentifiers.InsectSpecies.NotFound.name);

        assertThat(collection.isEmpty()).isTrue();
    }

    @Test
    void forParentName_rejectsNull() {
        assertThatThrownBy(() -> query.forParentName(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("parentName");
    }

    @Test
    void forParentName_acceptsSpeciesName() {
        ImageCollection collection =
                query.forParentName(TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name);

        assertThat(collection.size()).isGreaterThanOrEqualTo(1);
        assertThat(collection.stream())
                .allMatch(image -> image.parentName()
                        .equals(TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name));
    }

    @Test
    void forParentName_acceptsFamilyName() {
        ImageCollection collection =
                query.forParentName(TestInsectsIdentifiers.InsectFamily.Cicadellidae.name);

        assertThat(collection).isNotNull();
        assertThat(collection.isEmpty()).isTrue();
    }
}
