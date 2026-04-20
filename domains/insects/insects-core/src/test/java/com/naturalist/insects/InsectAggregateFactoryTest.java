package com.naturalist.insects;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.observability.Observer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InsectAggregateFactoryTest {
    static final Observer observer = Observer.forClass(InsectAggregateFactoryTest.class);

    @RegisterExtension
    NaturalistDatabase db = NaturalistDatabase.create();

    SpeciesRepositoryMock speciesRepository = new SpeciesRepositoryMock(db);
    InsectImageRepositoryMock imageRepository = new InsectImageRepositoryMock(db);
    InsectQuery.SpeciesQuery speciesQuery = new SpeciesQueryImpl(speciesRepository);
    InsectQuery.ImageQuery imageQuery = new ImageQueryImpl(imageRepository);
    InsectAggregateFactory factory = new InsectAggregateFactory(speciesQuery, imageQuery);

    @Test
    void buildByName_speciesWithImages_attachesAllImagesAndPreservesReferentialIntegrity() {
        Optional<InsectAggregate> aggregate =
                factory.buildByName(TestInsectsIdentifiers.InsectSpecies.PotatoLeafhopper.name);

        assertThat(aggregate).isPresent();
        InsectAggregate value = aggregate.get();

        assertThat(value.species().name())
                .isEqualTo(TestInsectsIdentifiers.InsectSpecies.PotatoLeafhopper.name);

        assertThat(value.images().size()).isGreaterThanOrEqualTo(2);
        assertThat(value.images().stream())
                .as("every image carries the root species name (factory-owned referential integrity)")
                .allMatch(image -> image.insectSpeciesName().equals(value.species().name()));
        assertThat(value.images().stream().map(InsectImage::name))
                .contains(
                        TestInsectsIdentifiers.InsectSpecies.PotatoLeafhopper.Images.Img9047.name,
                        TestInsectsIdentifiers.InsectSpecies.PotatoLeafhopper.Images.Img9048.name);

        assertThat(observer.observable(value, "insectAggregate").violations()).isEmpty();
    }

    @Test
    void buildByName_speciesWithoutImages_returnsAggregateWithEmptyImageCollection() {
        Optional<InsectAggregate> aggregate =
                factory.buildByName(TestInsectsIdentifiers.InsectSpecies.TachinidFly.name);

        assertThat(aggregate).isPresent();
        assertThat(aggregate.get().species().name())
                .isEqualTo(TestInsectsIdentifiers.InsectSpecies.TachinidFly.name);
        assertThat(aggregate.get().images().isEmpty()).isTrue();
        assertThat(observer.observable(aggregate.get(), "insectAggregate").violations()).isEmpty();
    }

    @Test
    void buildByName_unknownSpecies_returnsEmptyOptional() {
        Optional<InsectAggregate> aggregate =
                factory.buildByName(TestInsectsIdentifiers.InsectSpecies.NotFound.name);

        assertThat(aggregate).isEmpty();
    }

    @Test
    void buildByName_rejectsNull() {
        assertThatThrownBy(() -> factory.buildByName(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("name");
    }

    @Test
    void constructor_rejectsNullSpeciesQuery() {
        assertThatThrownBy(() -> new InsectAggregateFactory(null, imageQuery))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("speciesQuery");
    }

    @Test
    void constructor_rejectsNullImageQuery() {
        assertThatThrownBy(() -> new InsectAggregateFactory(speciesQuery, null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("imageQuery");
    }

    @Test
    void constructor_collectsAllViolationsInSinglePass() {
        assertThatThrownBy(() -> new InsectAggregateFactory(null, null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("speciesQuery", "imageQuery");
    }
}
