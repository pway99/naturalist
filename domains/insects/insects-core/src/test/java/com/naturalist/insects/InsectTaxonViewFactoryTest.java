package com.naturalist.insects;

import com.naturalist.data.NaturalistDatabaseExtension;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.observability.Observer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InsectTaxonViewFactoryTest {
    static final Observer observer = Observer.forClass(InsectTaxonViewFactoryTest.class);

    @RegisterExtension
    NaturalistDatabaseExtension db = NaturalistDatabaseExtension.create();

    SpeciesRepositoryMock speciesRepository = new SpeciesRepositoryMock(db);
    InsectImageRepositoryMock imageRepository = new InsectImageRepositoryMock(db);
    GenusRepositoryMock genusRepository = new GenusRepositoryMock(db);
    FamilyRepositoryMock familyRepository = new FamilyRepositoryMock(db);
    OrderRepositoryMock orderRepository = new OrderRepositoryMock(db);

    InsectQuery.ImageQuery imageQuery = new ImageQueryImpl(imageRepository);
    InsectQuery.FamilyQuery familyQuery = new FamilyQueryImpl(familyRepository);
    InsectQuery.GenusQuery genusQuery = new GenusQueryImpl(genusRepository, familyQuery);
    InsectQuery.SpeciesQuery speciesQuery = new SpeciesQueryImpl(speciesRepository, genusQuery);
    InsectQuery.OrderQuery orderQuery = new OrderQueryImpl(orderRepository);

    InsectTaxonViewFactory factory =
            new InsectTaxonViewFactory(speciesQuery, imageQuery, genusQuery, familyQuery, orderQuery);

    @Test
    void buildByName_speciesWithImages_returnsSpeciesViewWithImagesAndReferentialIntegrity() {
        Optional<InsectTaxonView> view =
                factory.buildByName(TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name);

        assertThat(view).isPresent();
        assertThat(view.get()).isInstanceOf(InsectSpeciesView.class);
        InsectSpeciesView value = (InsectSpeciesView) view.get();

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

        assertThat(observer.observable(value, "taxonView").violations()).isEmpty();
    }

    @Test
    void buildByName_speciesWithoutImages_returnsViewWithEmptyImageCollection() {
        Optional<InsectTaxonView> view =
                factory.buildByName(TestInsectsIdentifiers.InsectSpecies.HippodamiaConvergens.name);

        assertThat(view).isPresent();
        assertThat(view.get()).isInstanceOf(InsectSpeciesView.class);
        InsectSpeciesView value = (InsectSpeciesView) view.get();
        assertThat(value.species().name())
                .isEqualTo(TestInsectsIdentifiers.InsectSpecies.HippodamiaConvergens.name);
        assertThat(value.images().isEmpty()).isTrue();
        assertThat(observer.observable(value, "taxonView").violations()).isEmpty();
    }

    @Test
    void buildByName_genusWithImages_returnsGenusViewWithImagesAndReferentialIntegrity() {
        Optional<InsectTaxonView> view =
                factory.buildByName(TestInsectsIdentifiers.InsectGenus.Empoasca.name);

        assertThat(view).isPresent();
        assertThat(view.get()).isInstanceOf(InsectGenusView.class);
        InsectGenusView value = (InsectGenusView) view.get();

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

        assertThat(observer.observable(value, "taxonView").violations()).isEmpty();
    }

    @Test
    void buildByName_genusWithoutImages_returnsViewWithEmptyImageCollection() {
        Optional<InsectTaxonView> view =
                factory.buildByName(TestInsectsIdentifiers.InsectGenus.Halictus.name);

        assertThat(view).isPresent();
        assertThat(view.get()).isInstanceOf(InsectGenusView.class);
        InsectGenusView value = (InsectGenusView) view.get();
        assertThat(value.genus().name())
                .isEqualTo(TestInsectsIdentifiers.InsectGenus.Halictus.name);
        assertThat(value.images().isEmpty()).isTrue();
        assertThat(observer.observable(value, "taxonView").violations()).isEmpty();
    }

    @Test
    void buildByName_familyWithoutImages_returnsFamilyViewWithEmptyImageCollection() {
        Optional<InsectTaxonView> view =
                factory.buildByName(TestInsectsIdentifiers.InsectFamily.Tachinidae.name);

        assertThat(view).isPresent();
        assertThat(view.get()).isInstanceOf(InsectFamilyView.class);
        InsectFamilyView value = (InsectFamilyView) view.get();

        assertThat(value.family().name())
                .isEqualTo(TestInsectsIdentifiers.InsectFamily.Tachinidae.name);
        assertThat(value.name())
                .isEqualTo(TestInsectsIdentifiers.InsectFamily.Tachinidae.name);
        assertThat(value.images().isEmpty()).isTrue();
        assertThat(observer.observable(value, "taxonView").violations()).isEmpty();
    }

    @Test
    void buildByName_unknownSpecies_returnsEmptyOptional() {
        Optional<InsectTaxonView> view =
                factory.buildByName(TestInsectsIdentifiers.InsectSpecies.NotFound.name);

        assertThat(view).isEmpty();
    }

    @Test
    void buildByName_unknownGenus_returnsEmptyOptional() {
        Optional<InsectTaxonView> view =
                factory.buildByName(TestInsectsIdentifiers.InsectGenus.NotFound.name);

        assertThat(view).isEmpty();
    }

    @Test
    void buildByName_unknownFamily_returnsEmptyOptional() {
        Optional<InsectTaxonView> view =
                factory.buildByName(TestInsectsIdentifiers.InsectFamily.NotFound.name);

        assertThat(view).isEmpty();
    }

    @Test
    void buildByName_subspecies_alwaysReturnsEmptyOptional() {
        Optional<InsectTaxonView> view =
                factory.buildByName(InsectSubspeciesName.of("battus-philenor-hirsuta"));

        assertThat(view)
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
        assertThatThrownBy(() -> new InsectTaxonViewFactory(null, imageQuery, genusQuery, familyQuery, orderQuery))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("speciesQuery");
    }

    @Test
    void constructor_rejectsNullImageQuery() {
        assertThatThrownBy(() -> new InsectTaxonViewFactory(speciesQuery, null, genusQuery, familyQuery, orderQuery))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("imageQuery");
    }

    @Test
    void constructor_rejectsNullGenusQuery() {
        assertThatThrownBy(() -> new InsectTaxonViewFactory(speciesQuery, imageQuery, null, familyQuery, orderQuery))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("genusQuery");
    }

    @Test
    void constructor_rejectsNullFamilyQuery() {
        assertThatThrownBy(() -> new InsectTaxonViewFactory(speciesQuery, imageQuery, genusQuery, null, orderQuery))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("familyQuery");
    }

    @Test
    void constructor_rejectsNullOrderQuery() {
        assertThatThrownBy(() -> new InsectTaxonViewFactory(speciesQuery, imageQuery, genusQuery, familyQuery, null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("orderQuery");
    }

    @Test
    void constructor_collectsAllViolationsInSinglePass() {
        assertThatThrownBy(() -> new InsectTaxonViewFactory(null, null, null, null, null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("speciesQuery", "imageQuery", "genusQuery", "familyQuery", "orderQuery");
    }
}
