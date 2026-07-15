package com.naturalist.insects;

import com.naturalist.data.NaturalistDatabaseExtension;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.observability.Observer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaxonViewQueryImplTest {
    static final Observer observer = Observer.forClass(TaxonViewQueryImplTest.class);

    @RegisterExtension
    NaturalistDatabaseExtension db = NaturalistDatabaseExtension.create();

    SpeciesRepositoryMock speciesRepository = new SpeciesRepositoryMock(db);
    InsectImageRepositoryMock imageRepository = new InsectImageRepositoryMock(db);
    GenusRepositoryMock genusRepository = new GenusRepositoryMock(db);
    FamilyRepositoryMock familyRepository = new FamilyRepositoryMock(db);
    OrderRepositoryMock orderRepository = new OrderRepositoryMock(db);

    InsectQuery.FamilyQuery familyQuery = new FamilyQueryImpl(familyRepository);
    InsectQuery.GenusQuery genusQuery = new GenusQueryImpl(genusRepository, familyQuery);
    InsectQuery.SpeciesQuery speciesQuery = new SpeciesQueryImpl(speciesRepository, genusQuery);
    InsectQuery.ImageQuery imageQuery = new ImageQueryImpl(
            imageRepository, speciesQuery, genusQuery, familyQuery);
    InsectQuery.OrderQuery orderQuery = new OrderQueryImpl(orderRepository);

    InsectQuery.TaxonViewQuery taxonViewQuery =
            new TaxonViewQueryImpl(new InsectTaxonViewFactory(
                    speciesQuery, imageQuery, genusQuery, familyQuery, orderQuery));

    @Test
    void getByName_knownSpecies_returnsSpeciesView() {
        Optional<InsectTaxonView> view =
                taxonViewQuery.getByName(TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name);

        assertThat(view).isPresent();
        assertThat(view.get()).isInstanceOf(InsectSpeciesView.class);
        assertThat(observer.observable(view.get(), "taxonView").violations()).isEmpty();
        assertThat(view.get().name())
                .isEqualTo(TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name);
    }

    @Test
    void getByName_knownGenus_returnsGenusView() {
        Optional<InsectTaxonView> view =
                taxonViewQuery.getByName(TestInsectsIdentifiers.InsectGenus.Empoasca.name);

        assertThat(view).isPresent();
        assertThat(view.get()).isInstanceOf(InsectGenusView.class);
        assertThat(observer.observable(view.get(), "taxonView").violations()).isEmpty();
        assertThat(view.get().name())
                .isEqualTo(TestInsectsIdentifiers.InsectGenus.Empoasca.name);
    }

    @Test
    void getByName_knownFamily_returnsFamilyView() {
        Optional<InsectTaxonView> view =
                taxonViewQuery.getByName(TestInsectsIdentifiers.InsectFamily.Tachinidae.name);

        assertThat(view).isPresent();
        assertThat(view.get()).isInstanceOf(InsectFamilyView.class);
        assertThat(observer.observable(view.get(), "taxonView").violations()).isEmpty();
        assertThat(view.get().name())
                .isEqualTo(TestInsectsIdentifiers.InsectFamily.Tachinidae.name);
    }

    @Test
    void getByName_unknownSpecies_returnsEmptyOptional() {
        Optional<InsectTaxonView> view =
                taxonViewQuery.getByName(TestInsectsIdentifiers.InsectSpecies.NotFound.name);

        assertThat(view).isEmpty();
    }

    @Test
    void getByName_subspecies_returnsEmptyOptional() {
        Optional<InsectTaxonView> view =
                taxonViewQuery.getByName(InsectSubspeciesName.of("battus-philenor-hirsuta"));

        assertThat(view).isEmpty();
    }

    @Test
    void getByName_rejectsNull() {
        assertThatThrownBy(() -> taxonViewQuery.getByName(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("name");
    }
}
