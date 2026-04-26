package com.naturalist.chemistry.compound;

import com.naturalist.chemistry.TestChemistryIdentifiers;
import com.naturalist.data.EntityQuery;
import com.naturalist.data.EntityQueryContractTest;
import com.naturalist.data.NaturalistDatabaseExtension;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;

class CompoundQueryImplTest
        implements EntityQueryContractTest<CompoundName, Compound, CompoundCollection> {

    @RegisterExtension
    NaturalistDatabaseExtension db = NaturalistDatabaseExtension.create();

    CompoundRepository.CompoundEntityRepository repository = new CompoundEntityRepositoryMock(db);
    CompoundQuery query = new CompoundQueryImpl(repository);

    @Override
    public EntityQuery<CompoundName, Compound, CompoundCollection> query() {
        return query;
    }

    @Override
    public CompoundName notFoundName() {
        return TestChemistryIdentifiers.Compounds.NotFound.name;
    }

    @Override
    public List<CompoundName> knownEntityNames() {
        return List.of(
                TestChemistryIdentifiers.Compounds.PotassiumSulfate.name,
                TestChemistryIdentifiers.Compounds.CalciumChloride.name);
    }
}
