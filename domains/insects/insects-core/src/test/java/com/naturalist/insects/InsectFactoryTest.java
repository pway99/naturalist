package com.naturalist.insects;

import com.naturalist.RandomValue;
import com.naturalist.data.NaturalistTestExtension;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.fieldnotes.Description;
import com.naturalist.insects.lifestage.AdultStage;
import com.naturalist.insects.lifestage.EggStage;
import com.naturalist.insects.lifestage.LarvaStage;
import com.naturalist.insects.lifestage.InsectLifeStageTestEntitySource;
import com.naturalist.insects.lifestage.LifeStage;
import com.naturalist.insects.lifestage.StageHabitat;
import com.naturalist.insects.lifestage.StagePhenology;
import com.naturalist.observability.Observer;
import com.naturalist.test.context.Wired;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InsectFactoryTest {

    static final Observer observer = Observer.forClass(InsectFactoryTest.class);

    @RegisterExtension
    NaturalistTestExtension nte = NaturalistTestExtension.create();

    /** Provided by {@link InsectsTestContextInternal} over the graph's real queries, life stages included. */
    @Wired InsectFactory factory;

    @Test
    void buildByName_rejectsNull() {
        assertThatThrownBy(() -> factory.buildByName(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("name");
    }

    @Test
    void buildByName_speciesName_returnsFullyPopulatedInsect() {
        InsectSpeciesName name = TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name;

        Optional<Insect> result = factory.buildByName(name);

        assertThat(result).isPresent();
        Insect insect = result.get();
        assertThat(insect.species()).isNotNull();
        assertThat(insect.genus()).isNotNull();
        assertThat(insect.family()).isNotNull();
        assertThat(insect.order()).isNotNull();
        assertThat(insect.observations()).isNotNull();
        assertThat(insect.lifeStages()).isNotNull();
        // Verify invariants pass for the fully assembled read model
        assertThat(observer.observable(insect, "insect").violations()).isEmpty();
    }

    @Test
    void buildByName_orderName_returnsOrderOnlyInsect() {
        InsectOrderName name = InsectOrderName.of("lepidoptera");

        Optional<Insect> result = factory.buildByName(name);

        assertThat(result).isPresent();
        Insect insect = result.get();
        assertThat(insect.order()).isNotNull();
        assertThat(insect.family()).isNull();
        assertThat(insect.genus()).isNull();
        assertThat(insect.species()).isNull();
        assertThat(insect.lifeStages()).isNotNull();
    }

    @Test
    void buildByName_familyName_returnsFamilyAndOrderInsect() {
        InsectFamilyName name = TestInsectsIdentifiers.InsectFamily.Papilionidae.name;

        Optional<Insect> result = factory.buildByName(name);

        assertThat(result).isPresent();
        Insect insect = result.get();
        assertThat(insect.family()).isNotNull();
        assertThat(insect.order()).isNotNull();
        assertThat(insect.genus()).isNull();
        assertThat(insect.species()).isNull();
        assertThat(insect.lifeStages()).isNotNull();
    }

    @Test
    void buildByName_unknownName_returnsEmpty() {
        Optional<Insect> result = factory.buildByName(
                TestInsectsIdentifiers.InsectSpecies.NotFound.name);

        assertThat(result).isEmpty();
    }

    @Test
    void buildByName_subspeciesName_returnsEmpty() {
        InsectSubspeciesName subspeciesName = InsectSubspeciesName.of("battus-philenor-philenor");

        Optional<Insect> result = factory.buildByName(subspeciesName);

        assertThat(result).isEmpty();
    }

    @Test
    void buildByName_speciesWithCitations_includesResolvedCitations() {
        InsectSpeciesName name = TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name;

        Optional<Insect> result = factory.buildByName(name);

        assertThat(result).isPresent();
        Insect insect = result.get();
        // battus-philenor inherits citations from papilionidae and lepidoptera
        assertThat(insect.citations()).isNotNull();
        assertThat(insect.citations().citations()).isNotEmpty();
        assertThat(insect.citations().citations())
                .extracting(c -> c.citation().name().value())
            .contains("eol-battus-philenor-130502", "eol-lepidoptera-747");
    }

    @Test
    void buildByName_composesFeatureView() {
        InsectSpeciesName name = TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name;
        Insect insect = factory.buildByName(name).orElseThrow();
        assertThat(insect.features()).isNotNull();
        assertThat(insect.features().subject()).isEqualTo(name);
    }

    @Test
    void buildByName_orderName_composesFamilyChildren() {
        Insect insect = factory.buildByName(InsectOrderName.of("lepidoptera")).orElseThrow();
        assertThat(insect.children()).isNotEmpty();
        assertThat(insect.children()).allSatisfy(c ->
                assertThat(c).isInstanceOf(InsectFamily.class));
    }

    @Test
    void buildByName_orderName_familyChildrenAreAlphabeticallySorted() {
        Insect insect = factory.buildByName(InsectOrderName.of("lepidoptera")).orElseThrow();

        assertThat(insect.children())
                .extracting(c -> c.name().value())
                .containsExactly("hesperiidae", "nymphalidae", "papilionidae", "pieridae");
    }

    @Test
    void buildByName_speciesName_hasNoChildren() {
        Insect insect = factory.buildByName(
                TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name).orElseThrow();
        assertThat(insect.children()).isEmpty();
    }

    @Test
    void buildByName_composesRole_whenPresent() {
        // battus-philenor has a seeded SPECIES-rank InsectFunctionalRole
        // (guild KEYSTONE) in insect-functional-roles.json.
        Insect insect = factory.buildByName(
                TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name).orElseThrow();

        assertThat(insect.role()).isNotNull();
        assertThat(insect.role().parentName()).isEqualTo(
                TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name);
    }

    @Test
    void buildByName_holometabolousSpeciesHasAllFourStagesInSequence() {
        InsectSpeciesName name = TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name;

        Insect insect = factory.buildByName(name).orElseThrow();

        assertThat(insect.lifeStages().stream().map(LifeStage::kind).toList())
                .containsExactly(LifeStageKind.EGG, LifeStageKind.LARVA, LifeStageKind.PUPA, LifeStageKind.ADULT);
    }

    /**
     * A hemimetabolous sequence is egg, nymph, adult: it differs from enum order once a larva is present, so only
     * the factory's {@code orderedBy(sequence)} produces this order, not repository order and not an ordinal sort.
     * The stages are inserted larva, adult, egg, borrowing a fixture stage's phenology and habitat to stay valid.
     */
    @Test
    void buildByName_lifeStagesFollowTheSpeciesSequenceNotEnumOrder() {
        InsectSpeciesName name = TestInsectsIdentifiers.InsectSpecies.PeriplanetaAmericana.name;
        InsectLifeStageTestEntitySource stages = nte.getNamed(InsectLifeStageTestEntitySource.class);
        LifeStage valid = stages.getByName(LifeStageName.of(
                TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name, LifeStageKind.EGG)).orElseThrow();
        StagePhenology phenology = valid.phenology();
        StageHabitat habitat = valid.habitat();
        Description description = new Description(
                RandomValue.string(), RandomValue.string(), RandomValue.string(), RandomValue.string());
        stages.insert(new LarvaStage(LifeStageName.of(name, LifeStageKind.LARVA), name, phenology, habitat, null,
                description, null, List.of(), List.of(), null, null));
        stages.insert(new AdultStage(LifeStageName.of(name, LifeStageKind.ADULT), name, phenology, habitat, null,
                description, null, List.of(), null, null));
        stages.insert(new EggStage(LifeStageName.of(name, LifeStageKind.EGG), name, phenology, habitat, null,
                description, null, null, null));

        Insect insect = factory.buildByName(name).orElseThrow();

        assertThat(insect.lifeStages().stream().map(LifeStage::kind).toList())
                .containsExactly(LifeStageKind.EGG, LifeStageKind.ADULT, LifeStageKind.LARVA);
    }
}
