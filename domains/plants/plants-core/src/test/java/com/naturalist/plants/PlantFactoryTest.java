package com.naturalist.plants;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.Page;
import com.naturalist.data.PageRequest;
import com.naturalist.plants.cultivar.Cultivar;
import com.naturalist.plants.cultivar.CultivarCollection;
import com.naturalist.plants.cultivar.CultivarName;
import com.naturalist.plants.cultivar.CultivarQuery;
import com.naturalist.plants.management.PlantProgram;
import com.naturalist.plants.management.PlantProgramCollection;
import com.naturalist.plants.management.PlantProgramName;
import com.naturalist.plants.management.PlantProgramQuery;
import com.naturalist.plants.phytochemistry.PhytochemicalConstituent;
import com.naturalist.plants.phytochemistry.PhytochemicalConstituentCollection;
import com.naturalist.plants.phytochemistry.PhytochemicalConstituentName;
import com.naturalist.plants.phytochemistry.PhytochemicalConstituentQuery;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class PlantFactoryTest {

    private PlantFactory factory() {
        NaturalistDatabase db = NaturalistDatabase.create();
        PlantQuery.GenusQuery genusQuery =
                new PlantGenusQueryImpl(new PlantGenusRepositoryMock(db));
        PlantQuery.SpeciesQuery speciesQuery =
                new PlantSpeciesQueryImpl(new PlantSpeciesRepositoryMock(db), genusQuery);
        PlantQuery.FamilyQuery familyQuery =
                new PlantFamilyQueryImpl(new PlantFamilyRepositoryMock(db));
        PlantQuery.OrderQuery orderQuery =
                new PlantOrderQueryImpl(new PlantOrderRepositoryMock(db));
        PlantAncestryResolver resolver = new PlantAncestryResolver(speciesQuery, genusQuery, familyQuery);
        PlantQuery.FeatureQuery featureQuery = new PlantFeatureQueryImpl(
                new PlantFeatureRepositoryMock(db), new PlantFeatureAssignmentRepositoryMock(db), resolver);
        PlantQuery.EcologicalRoleQuery roleQuery =
                new PlantEcologicalRoleQueryImpl(new PlantEcologicalRoleRepositoryMock(db));
        PlantQuery.ImageQuery imageQuery =
                new PlantImageQueryImpl(new PlantImageRepositoryMock(db));
        CultivarQuery cultivarStub = new CultivarQuery() {
            @Override public CultivarCollection forPlantName(PlantSpeciesName n) { return CultivarCollection.empty(); }
            @Override public Optional<Cultivar> getByName(CultivarName name) { throw new UnsupportedOperationException(); }
            @Override public CultivarCollection findByNameSet(Set<CultivarName> s) { throw new UnsupportedOperationException(); }
            @Override public Page<Cultivar> findPage(PageRequest p) { throw new UnsupportedOperationException(); }
        };
        PlantProgramQuery programStub = new PlantProgramQuery() {
            @Override public PlantProgramCollection forPlantName(PlantRankName n) { return PlantProgramCollection.empty(); }
            @Override public Optional<PlantProgram> getByName(PlantProgramName name) { throw new UnsupportedOperationException(); }
            @Override public PlantProgramCollection findByNameSet(Set<PlantProgramName> s) { throw new UnsupportedOperationException(); }
            @Override public Page<PlantProgram> findPage(PageRequest p) { throw new UnsupportedOperationException(); }
        };
        PhytochemicalConstituentQuery constituentStub = new PhytochemicalConstituentQuery() {
            @Override public PhytochemicalConstituentCollection forPlantName(PlantRankName n) { return PhytochemicalConstituentCollection.empty(); }
            @Override public PhytochemicalConstituentCollection forCompoundName(com.naturalist.chemistry.compound.CompoundName c) { throw new UnsupportedOperationException(); }
            @Override public Optional<PhytochemicalConstituent> getByName(PhytochemicalConstituentName name) { throw new UnsupportedOperationException(); }
            @Override public PhytochemicalConstituentCollection findByNameSet(Set<PhytochemicalConstituentName> s) { throw new UnsupportedOperationException(); }
            @Override public Page<PhytochemicalConstituent> findPage(PageRequest p) { throw new UnsupportedOperationException(); }
        };
        return new PlantFactory(speciesQuery, genusQuery, familyQuery, orderQuery,
                featureQuery, roleQuery, imageQuery, cultivarStub, programStub, constituentStub);
    }

    @Test
    void buildByName_speciesName_composesFullAncestrySpine() {
        Plant plant = factory().buildByName(
                com.naturalist.plants.PlantSpeciesName.of("aristolochia-californica")).orElseThrow();
        assertThat(plant.speciesName()).contains(PlantSpeciesName.of("aristolochia-californica"));
        assertThat(plant.genusName()).contains(PlantGenusName.of("aristolochia"));
        assertThat(plant.familyName()).contains(PlantFamilyName.of("aristolochiaceae"));
        assertThat(plant.orderName()).contains(PlantOrderName.of("piperales"));
    }

    @Test
    void buildByName_orderName_composesOrderOnly() {
        Plant plant = factory().buildByName(PlantOrderName.of("lamiales")).orElseThrow();
        assertThat(plant.orderName()).contains(PlantOrderName.of("lamiales"));
        assertThat(plant.familyName()).isEmpty();
        assertThat(plant.genusName()).isEmpty();
        assertThat(plant.speciesName()).isEmpty();
    }

    @Test
    void buildByName_unknownName_isEmpty() {
        assertThat(factory().buildByName(PlantOrderName.of("unobtainium-ales"))).isEmpty();
    }

    @Test
    void buildByName_nullName_throws() {
        assertThat(catchThrowable(() -> factory().buildByName(null)))
                .isInstanceOf(com.naturalist.exception.InvariantViolationException.class);
    }

    @Test
    void buildByName_composesAncestryFeatures() {
        Plant plant = factory().buildByName(PlantGenusName.of("helianthus")).orElseThrow();
        assertThat(plant.features()).isNotNull();
        assertThat(plant.features().groups().stream().map(g -> g.rank()))
                .containsExactly(PlantOrderName.of("asterales"),
                        PlantFamilyName.of("asteraceae"), PlantGenusName.of("helianthus"));
    }

    @Test
    void buildByName_orderName_composesFamilyChildren() {
        Plant plant = factory().buildByName(PlantOrderName.of("asterales")).orElseThrow();
        assertThat(plant.children()).isNotEmpty();
        assertThat(plant.children()).allSatisfy(c ->
                assertThat(c).isInstanceOf(PlantFamilyView.class));
    }

    @Test
    void buildByName_speciesName_hasNoChildren() {
        Plant plant = factory().buildByName(PlantSpeciesName.of("aristolochia-californica")).orElseThrow();
        assertThat(plant.children()).isEmpty();
    }

    @Test
    void buildByName_composesImages_forSpeciesWithSeededPhotos() {
        // aristolochia-californica has 2 seeded images (plant-images.json).
        Plant plant = factory().buildByName(PlantSpeciesName.of("aristolochia-californica")).orElseThrow();
        assertThat(plant.images().stream().toList()).hasSize(2);
    }

    @Test
    void buildByName_nonSpeciesRank_hasEmptyExtras() {
        Plant plant = factory().buildByName(PlantOrderName.of("asterales")).orElseThrow();
        assertThat(plant.cultivars().isEmpty()).isTrue();
        assertThat(plant.programs().isEmpty()).isTrue();
        assertThat(plant.constituents().isEmpty()).isTrue();
    }
}
