package com.naturalist.insects.lifestage;

import com.naturalist.data.EntityQuery;
import com.naturalist.data.EntityQueryContractTest;
import com.naturalist.data.NaturalistDatabaseExtension;
import com.naturalist.insects.LifeStageName;
import com.naturalist.insects.TestInsectsIdentifiers;
import com.naturalist.insects.lifestage.InsectLifeStageEntityCollections.LifeStageCollection;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;

class LifeStageEntityQueryImplTest
        implements EntityQueryContractTest<LifeStageName, LifeStage, LifeStageCollection> {

    @RegisterExtension
    NaturalistDatabaseExtension db = NaturalistDatabaseExtension.create();

    LifeStageEntityRepositoryMock repository = new LifeStageEntityRepositoryMock(db);
    InsectLifeStageQuery.LifeStageEntityQuery query = new LifeStageEntityQueryImpl(repository);

    @Override
    public EntityQuery<LifeStageName, LifeStage, LifeStageCollection> query() {
        return query;
    }

    @Override
    public LifeStageName notFoundName() {
        return TestInsectsIdentifiers.InsectSpecies.NotFound.lifeStageName;
    }

    @Override
    public List<LifeStageName> knownEntityNames() {
        return List.of(
                TestInsectsIdentifiers.InsectSpecies.TachinidFly.LifeStages.Egg,
                TestInsectsIdentifiers.InsectSpecies.BraconidWasp.LifeStages.Larva);
    }
}