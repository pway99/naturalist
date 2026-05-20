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
    InsectQuery.ImageQuery query = new ImageQueryImpl(repository);

    @Override
    public EntityQuery<InsectImageId, InsectImage, ImageCollection> query() {
        return query;
    }

    @Override
    public InsectImageId notFoundName() {
        return TestInsectsIdentifiers.InsectSpecies.NotFound.imageName;
    }

    @Override
    public List<InsectImageId> knownEntityNames() {
        return List.of(
                TestInsectsIdentifiers.InsectSpecies.PotatoLeafhopper.Images.Img9047.name,
                TestInsectsIdentifiers.InsectSpecies.PotatoLeafhopper.Images.Img9048.name);
    }

    @Test
    void forParentName_returnsAllImagesForThatSpecies() {
        ImageCollection collection =
                query.forParentName(TestInsectsIdentifiers.InsectSpecies.PotatoLeafhopper.name);

        assertThat(collection.size()).isGreaterThanOrEqualTo(2);
        assertThat(collection.stream())
                .allMatch(image -> image.parentName()
                        .equals(TestInsectsIdentifiers.InsectSpecies.PotatoLeafhopper.name));
        assertThat(collection.stream().map(InsectImage::name))
                .contains(
                        TestInsectsIdentifiers.InsectSpecies.PotatoLeafhopper.Images.Img9047.name,
                        TestInsectsIdentifiers.InsectSpecies.PotatoLeafhopper.Images.Img9048.name);
    }

    @Test
    void forParentName_speciesWithNoImages_returnsEmpty() {
        ImageCollection collection =
                query.forParentName(TestInsectsIdentifiers.InsectSpecies.TachinidFly.name);

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
    void forParentName_acceptsGenusName() {
        ImageCollection collection =
                query.forParentName(TestInsectsIdentifiers.InsectGenus.Empoasca.name);

        assertThat(collection).isNotNull();
        assertThat(collection.isEmpty()).isTrue();
    }

    @Test
    void forParentName_acceptsFamilyName() {
        ImageCollection collection =
                query.forParentName(TestInsectsIdentifiers.InsectFamily.Cicadellidae.name);

        assertThat(collection).isNotNull();
        assertThat(collection.isEmpty()).isTrue();
    }
}
