package com.naturalist.chemistry.compound;

import com.naturalist.RandomValue;
import com.naturalist.chemistry.TemperatureFahrenheit;
import com.naturalist.chemistry.TestChemistryIdentifiers;
import com.naturalist.chemistry.compound.role.FunctionalRole;
import com.naturalist.chemistry.compound.structure.StructuralType;
import com.naturalist.chemistry.element.PeriodicElement;
import com.naturalist.data.EntityRepositoryTest;
import com.naturalist.data.TestEntitySource;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Behavioral contract for {@link CompoundRepository.CompoundEntityRepository}.
 * <p>
 * Inherits the {@link EntityRepositoryTest} cases (ADR-002).
 * Supplies Compound-specific identity constants and entity construction.
 */
interface CompoundEntityRepositoryTest
        extends EntityRepositoryTest<CompoundName, Compound> {

    @Override
    CompoundRepository.CompoundEntityRepository repository();

    @Override
    default TestEntitySource<CompoundName, Compound> source() {
        return db.getNamed(CompoundTestEntitySource.class);
    }

    @Override
    default CompoundName notFoundName() {
        return TestChemistryIdentifiers.Compounds.NotFound.name;
    }

    @Override
    default List<CompoundName> knownEntityNames() {
        return List.of(
                TestChemistryIdentifiers.Compounds.PotassiumSulfate.name,
                TestChemistryIdentifiers.Compounds.CalciumChloride.name);
    }

    @Override
    default Compound newEntity() {
        return new Compound(
                CompoundName.of(RandomValue.string()),
                RandomValue.string(),
                compoundInfo(),
                solubility(),
                bioavailability(),
                null,
                null,
                false,
                false,
                Map.of());
    }

    @Override
    default Compound ghostEntity() {
        return new Compound(
                CompoundName.of(RandomValue.string()),
                RandomValue.string(),
                compoundInfo(),
                solubility(),
                bioavailability(),
                null,
                null,
                false,
                false,
                Map.of());
    }

    @Override
    default Compound modifiedEntity(Compound original) {
        return new Compound(
                original.name(),
                RandomValue.string(),
                compoundInfo(),
                solubility(),
                bioavailability(),
                volatilization(),
                safety(),
                !original.omriListed(),
                !original.cdfaRegistered(),
                Map.of(RandomValue.string(), RandomValue.string()));
    }

    private CompoundInfo compoundInfo() {
        return new CompoundInfo(
                RandomValue.string(),
                null,
                PhCharacter.STRONGLY_ACIDIC,
                ChemicalNature.ORGANIC,
                PhysicalForm.COMPLEX,
                new StructuralType.OtherOrganic(),
                Set.of(new FunctionalRole.Chelator()),
                Set.of(PeriodicElement.P, PeriodicElement.Be));
    }

    private SolubilityProfile solubility() {
        return new SolubilityProfile(
                Solubility.of(RandomValue.bigDecimal()),
                SolubilityProfile.SolubilityCategory.INSOLUBLE,
                RandomValue.bigDecimal(),
                RandomValue.string());
    }

    private BioavailabilityProfile bioavailability() {
        return new BioavailabilityProfile(
                BioavailabilityProfile.AbsorptionPathway.FOLIAR_BOTH,
                RandomValue.bigDecimal(),
                true, true, true,
                RandomValue.string());
    }

    private VolatilizationProfile volatilization() {
        return new VolatilizationProfile(
                TemperatureFahrenheit.of(BigDecimal.valueOf(50)),
                TemperatureFahrenheit.of(BigDecimal.valueOf(85)),
                TemperatureFahrenheit.of(BigDecimal.valueOf(65)),
                RandomValue.bigDecimal(),
                RandomValue.string(),
                RandomValue.string());
    }

    private SafetyProfile safety() {
        return new SafetyProfile(
                SafetyProfile.HazardLevel.MODERATE,
                RandomValue.bigDecimal(),
                null,
                null,
                false,
                false,
                false,
                RandomValue.string());
    }
}
