package com.naturalist.insects;

import com.naturalist.data.NamedEntityQuery;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.insects.InsectEntityCollections.ImageCollection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImageQueryImplTest
        implements NamedEntityQueryContractTest<InsectImageName, InsectImage, ImageCollection> {

    @RegisterExtension
    NaturalistDatabase db = NaturalistDatabase.create();

    InsectImageRepositoryMock repository = new InsectImageRepositoryMock(db);
    InsectQuery.ImageQuery query = new ImageQueryImpl(repository);

    @Override
    public NamedEntityQuery<InsectImageName, InsectImage, ImageCollection> query() {
        return query;
    }

    @Override
    public InsectImageName notFoundName() {
        return TestInsectsIdentifiers.InsectSpecies.NotFound.imageName;
    }

    @Override
    public List<InsectImageName> knownEntityNames() {
        return List.of(
                TestInsectsIdentifiers.InsectSpecies.PotatoLeafhopper.Images.Img9047.name,
                TestInsectsIdentifiers.InsectSpecies.PotatoLeafhopper.Images.Img9048.name);
    }

    @Test
    void forSpeciesName_returnsAllImagesForThatSpecies() {
        ImageCollection collection =
                query.forSpeciesName(TestInsectsIdentifiers.InsectSpecies.PotatoLeafhopper.name);

        assertThat(collection.size()).isGreaterThanOrEqualTo(2);
        assertThat(collection.stream())
                .allMatch(image -> image.insectSpeciesName()
                        .equals(TestInsectsIdentifiers.InsectSpecies.PotatoLeafhopper.name));
        assertThat(collection.stream().map(InsectImage::name))
                .contains(
                        TestInsectsIdentifiers.InsectSpecies.PotatoLeafhopper.Images.Img9047.name,
                        TestInsectsIdentifiers.InsectSpecies.PotatoLeafhopper.Images.Img9048.name);
    }

    @Test
    void forSpeciesName_speciesWithNoImages_returnsEmpty() {
        ImageCollection collection =
                query.forSpeciesName(TestInsectsIdentifiers.InsectSpecies.TachinidFly.name);

        assertThat(collection).isNotNull();
        assertThat(collection.isEmpty()).isTrue();
    }

    @Test
    void forSpeciesName_unknownSpecies_returnsEmpty() {
        ImageCollection collection =
                query.forSpeciesName(TestInsectsIdentifiers.InsectSpecies.NotFound.name);

        assertThat(collection.isEmpty()).isTrue();
    }

    @Test
    void forSpeciesName_rejectsNull() {
        assertThatThrownBy(() -> query.forSpeciesName(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("speciesName");
    }
}
