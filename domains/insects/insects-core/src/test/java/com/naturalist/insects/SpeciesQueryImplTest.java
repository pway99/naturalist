package com.naturalist.insects;

import com.naturalist.data.NamedEntityQuery;
import com.naturalist.data.NaturalistDatabaseExtension;
import com.naturalist.insects.InsectEntityCollections.SpeciesCollection;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;

class SpeciesQueryImplTest
        implements NamedEntityQueryContractTest<InsectSpeciesName, InsectSpecies, SpeciesCollection> {

    @RegisterExtension
    NaturalistDatabaseExtension db = NaturalistDatabaseExtension.create();

    SpeciesRepositoryMock repository = new SpeciesRepositoryMock(db);
    InsectQuery.SpeciesQuery query = new SpeciesQueryImpl(repository);

    @Override
    public NamedEntityQuery<InsectSpeciesName, InsectSpecies, SpeciesCollection> query() {
        return query;
    }

    @Override
    public InsectSpeciesName notFoundName() {
        return TestInsectsIdentifiers.InsectSpecies.NotFound.name;
    }

    @Override
    public List<InsectSpeciesName> knownEntityNames() {
        return List.of(
                TestInsectsIdentifiers.InsectSpecies.PotatoLeafhopper.name,
                TestInsectsIdentifiers.InsectSpecies.TachinidFly.name,
                TestInsectsIdentifiers.InsectSpecies.BraconidWasp.name);
    }
}
