package com.naturalist.insects.lifestage;

import com.naturalist.data.EntityQuery;
import com.naturalist.data.EntityQueryContractTest;
import com.naturalist.data.NaturalistDatabaseExtension;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.insects.InsectSpeciesName;
import com.naturalist.insects.LifeStageName;
import com.naturalist.insects.TestInsectsIdentifiers;
import com.naturalist.insects.lifestage.InsectLifeStageEntityCollections.LifeStageCollection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Test
    void forSpeciesName_returnsAllLifeStagesForThatSpecies() {
        InsectSpeciesName species = TestInsectsIdentifiers.InsectSpecies.TachinidFly.name;

        LifeStageCollection collection = query.forSpeciesName(species);

        assertThat(collection.stream())
                .allMatch(stage -> stage.name().speciesName().equals(species));
        assertThat(collection.stream().map(LifeStage::name))
                .contains(
                        TestInsectsIdentifiers.InsectSpecies.TachinidFly.LifeStages.Egg,
                        TestInsectsIdentifiers.InsectSpecies.TachinidFly.LifeStages.Larva,
                        TestInsectsIdentifiers.InsectSpecies.TachinidFly.LifeStages.Pupa,
                        TestInsectsIdentifiers.InsectSpecies.TachinidFly.LifeStages.Adult);
    }

    @Test
    void forSpeciesName_unknownSpecies_returnsEmpty() {
        LifeStageCollection collection =
                query.forSpeciesName(TestInsectsIdentifiers.InsectSpecies.NotFound.name);

        assertThat(collection.isEmpty()).isTrue();
    }

    @Test
    void forSpeciesName_rejectsNull() {
        assertThatThrownBy(() -> query.forSpeciesName(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("speciesName");
    }
}