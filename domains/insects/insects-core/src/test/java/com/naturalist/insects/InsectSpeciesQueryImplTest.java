package com.naturalist.insects;

import com.naturalist.data.EntityQuery;
import com.naturalist.data.EntityQueryContractTest;
import com.naturalist.data.NaturalistTestExtension;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.insects.InsectEntityCollections.SpeciesCollection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InsectSpeciesQueryImplTest
        implements EntityQueryContractTest<InsectSpeciesName, InsectSpecies, SpeciesCollection> {

    @RegisterExtension
    NaturalistTestExtension nte = NaturalistTestExtension.create();

    InsectSpeciesRepositoryMock repository = new InsectSpeciesRepositoryMock(nte);
    InsectFamilyRepositoryMock familyRepository = new InsectFamilyRepositoryMock(nte);
    InsectGenusRepositoryMock genusRepository = new InsectGenusRepositoryMock(nte);
    InsectQuery.GenusQuery genusQuery = new InsectGenusQueryImpl(genusRepository, new InsectFamilyQueryImpl(familyRepository));
    InsectQuery.SpeciesQuery query = new InsectSpeciesQueryImpl(repository, genusQuery);

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
                TestInsectsIdentifiers.InsectSpecies.ColiasEurytheme.name,
                TestInsectsIdentifiers.InsectSpecies.HippodamiaConvergens.name);
    }

    @Test
    void forGenusName_rejectsNull() {
        assertThatThrownBy(() -> query.forGenusName(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("genusName");
    }

    @Test
    void forFamilyName_rejectsNull() {
        assertThatThrownBy(() -> query.forFamilyName(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("familyName");
    }

    @Test
    void forFamilyName_returnsSpeciesWithMatchingFamilyName() {
        SpeciesCollection collection =
                query.forFamilyName(TestInsectsIdentifiers.InsectFamily.Papilionidae.name);

        assertThat(collection.stream())
                .extracting(s -> s.name().value())
                .contains("battus-philenor");
    }
}
