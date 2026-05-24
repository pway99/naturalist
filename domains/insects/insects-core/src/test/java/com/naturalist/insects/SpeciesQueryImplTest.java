package com.naturalist.insects;

import com.naturalist.data.EntityQuery;
import com.naturalist.data.EntityQueryContractTest;
import com.naturalist.data.NaturalistDatabaseExtension;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.insects.InsectEntityCollections.SpeciesCollection;
import com.naturalist.taxonomy.TaxonomicGenus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SpeciesQueryImplTest
        implements EntityQueryContractTest<InsectSpeciesName, InsectSpecies, SpeciesCollection> {

    @RegisterExtension
    NaturalistDatabaseExtension db = NaturalistDatabaseExtension.create();

    SpeciesRepositoryMock repository = new SpeciesRepositoryMock(db);
    InsectQuery.SpeciesQuery query = new SpeciesQueryImpl(repository);

    @Override
    public EntityQuery<InsectSpeciesName, InsectSpecies, SpeciesCollection> query() {
        return query;
    }

    @Override
    public InsectSpeciesName notFoundName() {
        return TestInsectsIdentifiers.InsectSpecies.NotFound.name;
    }

    @Override
    public List<InsectSpeciesName> knownEntityNames() {
        return List.of(
                TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name,
                TestInsectsIdentifiers.InsectSpecies.TachinidFly.name,
                TestInsectsIdentifiers.InsectSpecies.BraconidWasp.name);
    }

    @Test
    void forGenusEpithet_rejectsNull() {
        assertThatThrownBy(() -> query.forGenusEpithet(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("genusEpithet");
    }

    @Test
    void forGenusEpithet_returnsSpeciesWithMatchingTaxonomyGenus() {
        SpeciesCollection collection =
                query.forGenusEpithet(TaxonomicGenus.of("Battus"));

        assertThat(collection.stream())
                .extracting(s -> s.name().value())
                .contains("battus-philenor");
    }
}
