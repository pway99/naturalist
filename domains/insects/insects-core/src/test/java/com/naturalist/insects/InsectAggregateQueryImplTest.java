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
    InsectQuery.SpeciesQuery speciesQuery = new SpeciesQueryImpl(speciesRepository);
    InsectQuery.ImageQuery imageQuery = new ImageQueryImpl(imageRepository);
    InsectQuery.InsectAggregateQuery aggregateQuery =
            new InsectAggregateQueryImpl(new InsectAggregateFactory(speciesQuery, imageQuery));

    @Test
    void getByName_known_returnsStructurallyValidAggregate() {
        Optional<InsectAggregate> aggregate =
                aggregateQuery.getByName(TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name);

        assertThat(aggregate).isPresent();
        assertThat(observer.observable(aggregate.get(), "insectAggregate").violations()).isEmpty();
        assertThat(aggregate.get().species().name())
                .isEqualTo(TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name);
    }

    @Test
    void getByName_notFound_returnsEmptyOptional() {
        Optional<InsectAggregate> aggregate =
                aggregateQuery.getByName(TestInsectsIdentifiers.InsectSpecies.NotFound.name);

        assertThat(aggregate).isEmpty();
    }

    @Test
    void getByName_rejectsNull() {
        assertThatThrownBy(() -> aggregateQuery.getByName(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("name");
    }
}
