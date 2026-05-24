package com.naturalist.insects;

import com.naturalist.data.NaturalistDatabaseExtension;
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
    NaturalistDatabaseExtension db = NaturalistDatabaseExtension.create();

    SpeciesRepositoryMock speciesRepository = new SpeciesRepositoryMock(db);
    InsectImageRepositoryMock imageRepository = new InsectImageRepositoryMock(db);
    GenusRepositoryMock genusRepository = new GenusRepositoryMock(db);
    FamilyRepositoryMock familyRepository = new FamilyRepositoryMock(db);

    InsectQuery.SpeciesQuery speciesQuery = new SpeciesQueryImpl(speciesRepository);
    InsectQuery.ImageQuery imageQuery = new ImageQueryImpl(imageRepository);
    InsectQuery.GenusQuery genusQuery = new GenusQueryImpl(genusRepository);
    InsectQuery.FamilyQuery familyQuery = new FamilyQueryImpl(familyRepository);

    InsectAggregateFactory factory =
            new InsectAggregateFactory(speciesQuery, imageQuery, genusQuery, familyQuery);

    @Test
    void buildByName_speciesWithImages_returnsSpeciesAggregateWithImagesAndReferentialIntegrity() {
        Optional<InsectAggregate> aggregate =
                factory.buildByName(TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name);

        assertThat(aggregate).isPresent();
        assertThat(aggregate.get()).isInstanceOf(InsectSpeciesAggregate.class);
        InsectSpeciesAggregate value = (InsectSpeciesAggregate) aggregate.get();

        assertThat(value.species().name())
                .isEqualTo(TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name);
        assertThat(value.name())
                .isEqualTo(TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name);

        assertThat(value.images().size()).isGreaterThanOrEqualTo(1);
        assertThat(value.images().stream())
                .as("every image carries the root species name (factory-owned referential integrity)")
                .allMatch(image -> image.parentName().equals(value.species().name()));
        assertThat(value.images().stream().map(InsectImage::name))
                .contains(TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.Images.PipevineSwallowtail.name);

        assertThat(observer.observable(value, "insectAggregate").violations()).isEmpty();
    }

    @Test
    void buildByName_speciesWithoutImages_returnsAggregateWithEmptyImageCollection() {
        Optional<InsectAggregate> aggregate =
                factory.buildByName(TestInsectsIdentifiers.InsectSpecies.TachinidFly.name);

        assertThat(aggregate).isPresent();
        assertThat(aggregate.get()).isInstanceOf(InsectSpeciesAggregate.class);
        InsectSpeciesAggregate value = (InsectSpeciesAggregate) aggregate.get();
        assertThat(value.species().name())
                .isEqualTo(TestInsectsIdentifiers.InsectSpecies.TachinidFly.name);
        assertThat(value.images().isEmpty()).isTrue();
        assertThat(observer.observable(value, "insectAggregate").violations()).isEmpty();
    }

    @Test
    void buildByName_genusWithImages_returnsGenusAggregateWithImagesAndReferentialIntegrity() {
        Optional<InsectAggregate> aggregate =
                factory.buildByName(TestInsectsIdentifiers.InsectGenus.Empoasca.name);

        assertThat(aggregate).isPresent();
        assertThat(aggregate.get()).isInstanceOf(InsectGenusAggregate.class);
        InsectGenusAggregate value = (InsectGenusAggregate) aggregate.get();

        assertThat(value.genus().name())
                .isEqualTo(TestInsectsIdentifiers.InsectGenus.Empoasca.name);
        assertThat(value.name())
                .isEqualTo(TestInsectsIdentifiers.InsectGenus.Empoasca.name);

        assertThat(value.images().size()).isGreaterThanOrEqualTo(1);
        assertThat(value.images().stream())
                .as("every image carries the root genus name (factory-owned referential integrity)")
                .allMatch(image -> image.parentName().equals(value.genus().name()));
        assertThat(value.images().stream().map(InsectImage::name))
                .contains(TestInsectsIdentifiers.InsectGenus.Empoasca.Images.Img9047.name);

        assertThat(observer.observable(value, "insectAggregate").violations()).isEmpty();
    }

    @Test
    void buildByName_genusWithoutImages_returnsAggregateWithEmptyImageCollection() {
        Optional<InsectAggregate> aggregate =
                factory.buildByName(TestInsectsIdentifiers.InsectGenus.Halictus.name);

        assertThat(aggregate).isPresent();
        assertThat(aggregate.get()).isInstanceOf(InsectGenusAggregate.class);
        InsectGenusAggregate value = (InsectGenusAggregate) aggregate.get();
        assertThat(value.genus().name())
                .isEqualTo(TestInsectsIdentifiers.InsectGenus.Halictus.name);
        assertThat(value.images().isEmpty()).isTrue();
        assertThat(observer.observable(value, "insectAggregate").violations()).isEmpty();
    }

    @Test
    void buildByName_familyWithoutImages_returnsFamilyAggregateWithEmptyImageCollection() {
        Optional<InsectAggregate> aggregate =
                factory.buildByName(TestInsectsIdentifiers.InsectFamily.Tachinidae.name);

        assertThat(aggregate).isPresent();
        assertThat(aggregate.get()).isInstanceOf(InsectFamilyAggregate.class);
        InsectFamilyAggregate value = (InsectFamilyAggregate) aggregate.get();

        assertThat(value.family().name())
                .isEqualTo(TestInsectsIdentifiers.InsectFamily.Tachinidae.name);
        assertThat(value.name())
                .isEqualTo(TestInsectsIdentifiers.InsectFamily.Tachinidae.name);
        assertThat(value.images().isEmpty()).isTrue();
        assertThat(observer.observable(value, "insectAggregate").violations()).isEmpty();
    }

    @Test
    void buildByName_unknownSpecies_returnsEmptyOptional() {
        Optional<InsectAggregate> aggregate =
                factory.buildByName(TestInsectsIdentifiers.InsectSpecies.NotFound.name);

        assertThat(aggregate).isEmpty();
    }

    @Test
    void buildByName_unknownGenus_returnsEmptyOptional() {
        Optional<InsectAggregate> aggregate =
                factory.buildByName(TestInsectsIdentifiers.InsectGenus.NotFound.name);

        assertThat(aggregate).isEmpty();
    }

    @Test
    void buildByName_unknownFamily_returnsEmptyOptional() {
        Optional<InsectAggregate> aggregate =
                factory.buildByName(TestInsectsIdentifiers.InsectFamily.NotFound.name);

        assertThat(aggregate).isEmpty();
    }

    @Test
    void buildByName_subspecies_alwaysReturnsEmptyOptional() {
        Optional<InsectAggregate> aggregate =
                factory.buildByName(InsectSubspeciesName.of("battus-philenor-hirsuta"));

        assertThat(aggregate)
                .as("no InsectSubspecies entity exists yet — subspecies-rank requests are a graceful no-op")
                .isEmpty();
    }

    @Test
    void buildByName_rejectsNull() {
        assertThatThrownBy(() -> factory.buildByName(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("name");
    }

    @Test
    void constructor_rejectsNullSpeciesQuery() {
        assertThatThrownBy(() -> new InsectAggregateFactory(null, imageQuery, genusQuery, familyQuery))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("speciesQuery");
    }

    @Test
    void constructor_rejectsNullImageQuery() {
        assertThatThrownBy(() -> new InsectAggregateFactory(speciesQuery, null, genusQuery, familyQuery))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("imageQuery");
    }

    @Test
    void constructor_rejectsNullGenusQuery() {
        assertThatThrownBy(() -> new InsectAggregateFactory(speciesQuery, imageQuery, null, familyQuery))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("genusQuery");
    }

    @Test
    void constructor_rejectsNullFamilyQuery() {
        assertThatThrownBy(() -> new InsectAggregateFactory(speciesQuery, imageQuery, genusQuery, null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("familyQuery");
    }

    @Test
    void constructor_collectsAllViolationsInSinglePass() {
        assertThatThrownBy(() -> new InsectAggregateFactory(null, null, null, null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("speciesQuery", "imageQuery", "genusQuery", "familyQuery");
    }
}
