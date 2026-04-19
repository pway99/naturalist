package com.naturalist.insects;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.insects.InsectEntityCollections.SpeciesCollection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SpeciesQueryImplTest {

    @RegisterExtension
    NaturalistDatabase db = NaturalistDatabase.create();

    SpeciesRepositoryMock repository = new SpeciesRepositoryMock(db);
    InsectQuery.SpeciesQuery query = new SpeciesQueryImpl(repository);

    @Test
    void getByName_known() {
        Optional<InsectSpecies> species =
                query.getByName(TestInsectsIdentifiers.InsectSpecies.PotatoLeafhopper.name);

        assertThat(species).isPresent();
        assertThat(species.get().name())
                .isEqualTo(TestInsectsIdentifiers.InsectSpecies.PotatoLeafhopper.name);
    }

    @Test
    void getByName_notFound() {
        Optional<InsectSpecies> species =
                query.getByName(TestInsectsIdentifiers.InsectSpecies.NotFound.name);

        assertThat(species).isEmpty();
    }

    @Test
    void getByName_rejectsNull() {
        assertThatThrownBy(() -> query.getByName(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("name");
    }

    @Test
    void findByNameSet_multiple() {
        Set<InsectSpeciesName> names = Set.of(
                TestInsectsIdentifiers.InsectSpecies.PotatoLeafhopper.name,
                TestInsectsIdentifiers.InsectSpecies.TachinidFly.name,
                TestInsectsIdentifiers.InsectSpecies.BraconidWasp.name);

        SpeciesCollection collection = query.findByNameSet(names);

        assertThat(collection).isNotNull();
        assertThat(collection.size()).isEqualTo(3);
        assertThat(collection.stream().map(InsectSpecies::name))
                .containsExactlyInAnyOrderElementsOf(names);
    }

    @Test
    void findByNameSet_partialMatch_returnsOnlyKnown() {
        Set<InsectSpeciesName> names = Set.of(
                TestInsectsIdentifiers.InsectSpecies.PotatoLeafhopper.name,
                TestInsectsIdentifiers.InsectSpecies.NotFound.name);

        SpeciesCollection collection = query.findByNameSet(names);

        assertThat(collection.size()).isEqualTo(1);
        assertThat(collection.stream().findFirst().orElseThrow().name())
                .isEqualTo(TestInsectsIdentifiers.InsectSpecies.PotatoLeafhopper.name);
    }

    @Test
    void findByNameSet_emptySet_returnsEmptyCollection() {
        SpeciesCollection collection = query.findByNameSet(Set.of());

        assertThat(collection).isNotNull();
        assertThat(collection.isEmpty()).isTrue();
    }

    @Test
    void findByNameSet_rejectsNullSet() {
        assertThatThrownBy(() -> query.findByNameSet(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("names");
    }
}
