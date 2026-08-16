package com.naturalist.plants.phytochemistry;

import com.naturalist.RandomValue;
import com.naturalist.chemistry.TestChemistryIdentifiers;
import com.naturalist.chemistry.compound.CompoundName;
import com.naturalist.data.EntityRepositoryTest;
import com.naturalist.data.TestEntitySource;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.fieldnotes.Description;
import com.naturalist.plants.PlantSpeciesName;
import com.naturalist.plants.TestPlantsIdentifiers;
import com.naturalist.plants.phytochemistry.role.PhytochemicalRole;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Behavioral contract for {@link PhytochemicalConstituentRepository}.
 * <p>
 * Inherits the {@link EntityRepositoryTest} cases (ADR-002).
 * Supplies PhytochemicalConstituent-specific identity constants and entity
 * construction. The cross-domain {@code compoundName} soft FK uses
 * {@link TestChemistryIdentifiers} constants — the chemistry catalog is the
 * authoritative store, and the test fixtures must reference real compound
 * slugs so the entity passes record invariants.
 */
interface PhytochemicalConstituentRepositoryTest
        extends EntityRepositoryTest<PhytochemicalConstituentName, PhytochemicalConstituent> {

    @Override
    PhytochemicalConstituentRepository repository();

    @Override
    default TestEntitySource<PhytochemicalConstituentName, PhytochemicalConstituent> source() {
        return db.getNamed(PhytochemicalConstituentTestEntitySource.class);
    }

    @Override
    default PhytochemicalConstituentName notFoundName() {
        return TestPlantsIdentifiers.Plants.NotFound.constituentName;
    }

    @Override
    default List<PhytochemicalConstituentName> knownEntityNames() {
        return List.of(
                TestPlantsIdentifiers.Plants.CaliforniaPipevine.Constituents.AristolochicAcidI,
                TestPlantsIdentifiers.PlantGenera.Thymus.Constituents.Thymol
        );
    }

    @Override
    default PhytochemicalConstituent newEntity() {
        return new PhytochemicalConstituent(
                PhytochemicalConstituentName.of("test-" + RandomValue.string()),
                TestPlantsIdentifiers.Plants.CaliforniaPipevine.name,
                TestChemistryIdentifiers.Compounds.Thymol.name,
                description(),
                PhytochemicalCategory.ALKALOID,
                Set.of(new PhytochemicalRole.HerbivoreDeterrent()),
                Set.of(PlantTissue.LEAF),
                InductionMode.CONSTITUTIVE,
                null);
    }

    @Override
    default PhytochemicalConstituent ghostEntity() {
        return new PhytochemicalConstituent(
                PhytochemicalConstituentName.of("ghost-" + RandomValue.string()),
                TestPlantsIdentifiers.Plants.Borage.name,
                TestChemistryIdentifiers.Compounds.Azadirachtin.name,
                description(),
                PhytochemicalCategory.TERPENOID,
                Set.of(new PhytochemicalRole.AntiFungal()),
                Set.of(PlantTissue.LEAF, PlantTissue.STEM),
                InductionMode.INDUCED,
                "fictitious test fixture");
    }

    @Override
    default PhytochemicalConstituent modifiedEntity(PhytochemicalConstituent original) {
        PlantSpeciesName flippedPlant = original.plantName()
                .equals(TestPlantsIdentifiers.Plants.CaliforniaPipevine.name)
                ? TestPlantsIdentifiers.Plants.Borage.name
                : TestPlantsIdentifiers.Plants.CaliforniaPipevine.name;
        CompoundName flippedCompound = original.compoundName()
                .equals(TestChemistryIdentifiers.Compounds.Thymol.name)
                ? TestChemistryIdentifiers.Compounds.Azadirachtin.name
                : TestChemistryIdentifiers.Compounds.Thymol.name;
        PhytochemicalCategory flippedCategory = original.category() == PhytochemicalCategory.ALKALOID
                ? PhytochemicalCategory.TERPENOID
                : PhytochemicalCategory.ALKALOID;
        InductionMode flippedInduction = original.induction() == InductionMode.CONSTITUTIVE
                ? InductionMode.INDUCED
                : InductionMode.CONSTITUTIVE;
        Set<PhytochemicalRole> flippedRoles = original.roles().stream()
                .anyMatch(r -> r instanceof PhytochemicalRole.HerbivoreDeterrent)
                ? Set.of(new PhytochemicalRole.AntiFungal(), new PhytochemicalRole.Pharmaceutical())
                : Set.of(new PhytochemicalRole.HerbivoreDeterrent());
        Set<PlantTissue> flippedTissues = original.tissues().contains(PlantTissue.LEAF)
                ? Set.of(PlantTissue.ROOT, PlantTissue.STEM)
                : Set.of(PlantTissue.LEAF);
        return new PhytochemicalConstituent(
                original.name(),
                flippedPlant,
                flippedCompound,
                description(),
                flippedCategory,
                flippedRoles,
                flippedTissues,
                flippedInduction,
                RandomValue.string());
    }

    private static Description description() {
        return new Description(
                RandomValue.string(), RandomValue.string(),
                RandomValue.string(), RandomValue.string());
    }

    @Test
    default void getByPlantName_rejectsNull() {
        assertThatThrownBy(() -> repository().getByPlantName(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("plantName");
    }

    @Test
    default void getByPlantName_returnsConstituentsWithMatchingPlantName() {
        var results = repository().getByPlantName(
                TestPlantsIdentifiers.Plants.CaliforniaPipevine.name);

        assertThat(results)
                .extracting(PhytochemicalConstituent::name)
                .extracting(PhytochemicalConstituentName::value)
                .contains(
                        "aristolochia-californica-aristolochic-acid-i",
                        "aristolochia-californica-aristolochic-acid-ii");
    }

    @Test
    default void getByPlantName_returnsEmptyForUnknownPlant() {
        var results = repository().getByPlantName(TestPlantsIdentifiers.Plants.NotFound.name);

        assertThat(results).isEmpty();
    }

    @Test
    default void getByCompoundName_rejectsNull() {
        assertThatThrownBy(() -> repository().getByCompoundName(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("compoundName");
    }

    @Test
    default void getByCompoundName_returnsConstituentsAcrossPlants() {
        // The cross-domain reverse lookup that PlantsCompoundReferences depends on:
        // "which plants are known to produce this compound?"
        var results = repository().getByCompoundName(
                TestChemistryIdentifiers.Compounds.Thymol.name);

        // The thymol constituent was re-keyed to the genus thymus in the 2026-08-16
        // rank audit (M2e), when creeping-thyme demoted from species to genus rank.
        assertThat(results)
                .extracting(PhytochemicalConstituent::name)
                .extracting(PhytochemicalConstituentName::value)
                .contains("thymus-thymol");
    }

    @Test
    default void getByCompoundName_returnsEmptyForUnknownCompound() {
        var results = repository().getByCompoundName(CompoundName.of("unobtainium-oxide"));

        assertThat(results).isEmpty();
    }
}
