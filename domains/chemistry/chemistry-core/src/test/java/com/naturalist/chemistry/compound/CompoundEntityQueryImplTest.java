package com.naturalist.chemistry.compound;

import com.naturalist.chemistry.TestChemistryIdentifiers;
import com.naturalist.chemistry.compound.CompoundEntityCollections.CompoundCollection;
import com.naturalist.data.EntityQuery;
import com.naturalist.data.EntityQueryContractTest;
import com.naturalist.data.NaturalistTestExtension;
import com.naturalist.test.context.Wired;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;

class CompoundEntityQueryImplTest
        implements EntityQueryContractTest<CompoundName, Compound, CompoundCollection> {

    @RegisterExtension
    NaturalistTestExtension nte = NaturalistTestExtension.create();

    @Wired CompoundQuery.CompoundEntityQuery query;

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
