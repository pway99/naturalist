package com.naturalist.insects;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.insects.InsectEntityCollections.ImageCollection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImageQueryImplTest {

    @RegisterExtension
    NaturalistDatabase db = NaturalistDatabase.create();

    InsectImageRepositoryMock repository = new InsectImageRepositoryMock(db);
    InsectQuery.ImageQuery query = new ImageQueryImpl(repository);

    @Test
    void getByName_known() {
        Optional<InsectImage> image =
                query.getByName(TestInsectsIdentifiers.InsectSpecies.PotatoLeafhopper.Images.Img9047.name);

        assertThat(image).isPresent();
        assertThat(image.get().name())
                .isEqualTo(TestInsectsIdentifiers.InsectSpecies.PotatoLeafhopper.Images.Img9047.name);
        assertThat(image.get().insectSpeciesName())
                .isEqualTo(TestInsectsIdentifiers.InsectSpecies.PotatoLeafhopper.name);
    }

    @Test
    void getByName_notFound() {
        Optional<InsectImage> image =
                query.getByName(TestInsectsIdentifiers.InsectSpecies.NotFound.imageName);

        assertThat(image).isEmpty();
    }

    @Test
    void getByName_rejectsNull() {
        assertThatThrownBy(() -> query.getByName(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("name");
    }

    @Test
    void findByNameSet_multiple() {
        Set<InsectImageName> names = Set.of(
                TestInsectsIdentifiers.InsectSpecies.PotatoLeafhopper.Images.Img9047.name,
                TestInsectsIdentifiers.InsectSpecies.PotatoLeafhopper.Images.Img9048.name);

        ImageCollection collection = query.findByNameSet(names);

        assertThat(collection.size()).isEqualTo(2);
        assertThat(collection.stream().map(InsectImage::name))
                .containsExactlyInAnyOrderElementsOf(names);
    }

    @Test
    void findByNameSet_partialMatch_returnsOnlyKnown() {
        Set<InsectImageName> names = Set.of(
                TestInsectsIdentifiers.InsectSpecies.PotatoLeafhopper.Images.Img9047.name,
                TestInsectsIdentifiers.InsectSpecies.NotFound.imageName);

        ImageCollection collection = query.findByNameSet(names);

        assertThat(collection.size()).isEqualTo(1);
        assertThat(collection.stream().findFirst().orElseThrow().name())
                .isEqualTo(TestInsectsIdentifiers.InsectSpecies.PotatoLeafhopper.Images.Img9047.name);
    }

    @Test
    void findByNameSet_rejectsNullSet() {
        assertThatThrownBy(() -> query.findByNameSet(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("names");
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
