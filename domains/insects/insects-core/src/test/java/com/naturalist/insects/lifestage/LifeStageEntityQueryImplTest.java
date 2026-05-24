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
                TestInsectsIdentifiers.InsectFamily.Tachinidae.LifeStages.Egg,
                TestInsectsIdentifiers.InsectFamily.Braconidae.LifeStages.Larva);
    }

    @Test
    void forParentName_returnsAllLifeStagesForThatSpecies() {
        InsectSpeciesName species = TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name;

        LifeStageCollection collection = query.forParentName(species);

        assertThat(collection.stream())
                .allMatch(stage -> stage.name().parentSlug().equals(species.value()));
        assertThat(collection.stream().map(LifeStage::name))
                .contains(
                        TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.LifeStages.Egg,
                        TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.LifeStages.Larva,
                        TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.LifeStages.Pupa,
                        TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.LifeStages.Adult);
    }

    @Test
    void forParentName_acceptsGenusName() {
        var genus = TestInsectsIdentifiers.InsectGenus.Chrysoperla.name;

        LifeStageCollection collection = query.forParentName(genus);

        assertThat(collection.size()).isGreaterThanOrEqualTo(4);
        assertThat(collection.stream())
                .allMatch(stage -> stage.name().parentSlug().equals(genus.value()));
    }

    @Test
    void forParentName_acceptsFamilyName() {
        var family = TestInsectsIdentifiers.InsectFamily.Syrphidae.name;

        LifeStageCollection collection = query.forParentName(family);

        assertThat(collection.size()).isGreaterThanOrEqualTo(4);
        assertThat(collection.stream())
                .allMatch(stage -> stage.name().parentSlug().equals(family.value()));
    }

    @Test
    void forParentName_unknownSpecies_returnsEmpty() {
        LifeStageCollection collection =
                query.forParentName(TestInsectsIdentifiers.InsectSpecies.NotFound.name);

        assertThat(collection.isEmpty()).isTrue();
    }

    @Test
    void forParentName_rejectsNull() {
        assertThatThrownBy(() -> query.forParentName(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("parentName");
    }
}
