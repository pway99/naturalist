package com.naturalist.insects;

import com.naturalist.data.NaturalistDatabaseExtension;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.observability.Observer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InsectAggregateQueryImplTest {
    static final Observer observer = Observer.forClass(InsectAggregateQueryImplTest.class);

    @RegisterExtension
    NaturalistDatabaseExtension db = NaturalistDatabaseExtension.create();

    SpeciesRepositoryMock speciesRepository = new SpeciesRepositoryMock(db);
    InsectImageRepositoryMock imageRepository = new InsectImageRepositoryMock(db);
    GenusRepositoryMock genusRepository = new GenusRepositoryMock(db);
    FamilyRepositoryMock familyRepository = new FamilyRepositoryMock(db);
    OrderRepositoryMock orderRepository = new OrderRepositoryMock(db);

    InsectQuery.SpeciesQuery speciesQuery = new SpeciesQueryImpl(speciesRepository);
    InsectQuery.ImageQuery imageQuery = new ImageQueryImpl(imageRepository);
    InsectQuery.GenusQuery genusQuery = new GenusQueryImpl(genusRepository);
    InsectQuery.FamilyQuery familyQuery = new FamilyQueryImpl(familyRepository);
    InsectQuery.OrderQuery orderQuery = new OrderQueryImpl(orderRepository);

    InsectQuery.InsectAggregateQuery aggregateQuery =
            new InsectAggregateQueryImpl(new InsectAggregateFactory(
                    speciesQuery, imageQuery, genusQuery, familyQuery, orderQuery));

    @Test
    void getByName_knownSpecies_returnsSpeciesAggregate() {
        Optional<InsectAggregate> aggregate =
                aggregateQuery.getByName(TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name);

        assertThat(aggregate).isPresent();
        assertThat(aggregate.get()).isInstanceOf(InsectSpeciesAggregate.class);
        assertThat(observer.observable(aggregate.get(), "insectAggregate").violations()).isEmpty();
        assertThat(aggregate.get().name())
                .isEqualTo(TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name);
    }

    @Test
    void getByName_knownGenus_returnsGenusAggregate() {
        Optional<InsectAggregate> aggregate =
                aggregateQuery.getByName(TestInsectsIdentifiers.InsectGenus.Empoasca.name);

        assertThat(aggregate).isPresent();
        assertThat(aggregate.get()).isInstanceOf(InsectGenusAggregate.class);
        assertThat(observer.observable(aggregate.get(), "insectAggregate").violations()).isEmpty();
        assertThat(aggregate.get().name())
                .isEqualTo(TestInsectsIdentifiers.InsectGenus.Empoasca.name);
    }

    @Test
    void getByName_knownFamily_returnsFamilyAggregate() {
        Optional<InsectAggregate> aggregate =
                aggregateQuery.getByName(TestInsectsIdentifiers.InsectFamily.Tachinidae.name);

        assertThat(aggregate).isPresent();
        assertThat(aggregate.get()).isInstanceOf(InsectFamilyAggregate.class);
        assertThat(observer.observable(aggregate.get(), "insectAggregate").violations()).isEmpty();
        assertThat(aggregate.get().name())
                .isEqualTo(TestInsectsIdentifiers.InsectFamily.Tachinidae.name);
    }

    @Test
    void getByName_unknownSpecies_returnsEmptyOptional() {
        Optional<InsectAggregate> aggregate =
                aggregateQuery.getByName(TestInsectsIdentifiers.InsectSpecies.NotFound.name);

        assertThat(aggregate).isEmpty();
    }

    @Test
    void getByName_subspecies_returnsEmptyOptional() {
        Optional<InsectAggregate> aggregate =
                aggregateQuery.getByName(InsectSubspeciesName.of("battus-philenor-hirsuta"));

        assertThat(aggregate).isEmpty();
    }

    @Test
    void getByName_rejectsNull() {
        assertThatThrownBy(() -> aggregateQuery.getByName(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("name");
    }
}
