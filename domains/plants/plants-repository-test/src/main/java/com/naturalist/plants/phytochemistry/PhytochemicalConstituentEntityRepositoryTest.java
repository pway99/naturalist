package com.naturalist.plants.phytochemistry;

import com.naturalist.RandomValue;
import com.naturalist.chemistry.TestChemistryIdentifiers;
import com.naturalist.chemistry.compound.CompoundName;
import com.naturalist.data.EntityRepositoryTest;
import com.naturalist.data.TestEntitySource;
import com.naturalist.fieldnotes.Description;
import com.naturalist.plants.PlantName;
import com.naturalist.plants.TestPlantsIdentifiers;
import com.naturalist.plants.phytochemistry.role.PhytochemicalRole;

import java.util.List;
import java.util.Set;

/**
 * Behavioral contract for {@link PhytochemicalConstituentRepository.PhytochemicalConstituentEntityRepository}.
 * <p>
 * Inherits the {@link EntityRepositoryTest} cases (ADR-002).
 * Supplies PhytochemicalConstituent-specific identity constants and entity
 * construction. The cross-domain {@code compoundName} soft FK uses
 * {@link TestChemistryIdentifiers} constants — the chemistry catalog is the
 * authoritative store, and the test fixtures must reference real compound
 * slugs so the entity passes record invariants.
 */
interface PhytochemicalConstituentEntityRepositoryTest
        extends EntityRepositoryTest<PhytochemicalConstituentName, PhytochemicalConstituent> {

    @Override
    PhytochemicalConstituentRepository.PhytochemicalConstituentEntityRepository repository();

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
                TestPlantsIdentifiers.Plants.CreepingThyme.Constituents.Thymol
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
        PlantName flippedPlant = original.plantName()
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
}
