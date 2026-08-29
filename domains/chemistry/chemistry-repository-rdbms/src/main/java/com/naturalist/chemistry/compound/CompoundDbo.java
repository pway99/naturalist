package com.naturalist.chemistry.compound;

import com.naturalist.chemistry.TemperatureFahrenheit;
import com.naturalist.chemistry.compound.role.FunctionalRole;
import com.naturalist.chemistry.compound.structure.StructuralType;
import com.naturalist.chemistry.element.PeriodicElement;
import com.naturalist.observability.Constraints;
import com.naturalist.observability.Observer;
import com.naturalist.persistence.Dbo;
import com.naturalist.persistence.DboSchema;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Parent row of the fully-normalized {@link Compound} aggregate. The 1:1 owned value
 * objects — {@link CompoundInfo}'s scalars plus the four profiles — flatten onto columns
 * here; the multi-valued members become child tables ({@link CompoundFunctionalRoleDbo},
 * {@link CompoundConstituentElementDbo}, {@link CompoundPropertyDbo}). Because the whole
 * aggregate spans several tables, this DBO does not carry a bare {@code toEntity()}: the
 * adapter loads the child rows and passes them to {@link #toEntity(Set, Set, Map)}.
 *
 * <p>Fields are camelCase; MyBatis translates the snake_case columns across on read
 * ({@code common_name} -> {@code commonName}). The two {@code @Nullable} profiles
 * ({@code volatilization}, {@code safety}) map to nullable column groups — every column in
 * the group is null exactly when the profile is absent, so a single required field of each
 * group serves as its presence flag.
 */
@DboSchema(table = "compound", primaryKey = "id", unique = {"name", "common_name"}, entity = Compound.class)
final class CompoundDbo implements Dbo {
    Long id;              // numeric identity anchor (BIGINT IDENTITY); null before insert.
                          // Durable FK target for compound_depiction, mirroring naturalist.id.
    String name;
    String commonName;
    boolean omriListed;
    boolean cdfaRegistered;

    // ── CompoundInfo scalars ─────────────────────────────────────────────────
    String formula;
    BigDecimal molecularWeight;           // nullable
    String phCharacter;
    String chemicalNature;
    String physicalForm;
    String structuralType;                // StructuralType "kind" discriminator

    // ── SolubilityProfile ────────────────────────────────────────────────────
    BigDecimal solubilityGramsPerLiterAt20c;
    String solubilityCategory;
    BigDecimal solubilityEcContributionFactor;
    String solubilityNotes;

    // ── BioavailabilityProfile ───────────────────────────────────────────────
    String bioavailabilityPrimaryPathway;
    BigDecimal bioavailabilityRelativeAbsorptionRate;
    boolean bioavailabilityCuticular;
    boolean bioavailabilityStomatal;
    boolean bioavailabilityChelateEnhanced;
    String bioavailabilityMechanism;

    // ── VolatilizationProfile (nullable group) ───────────────────────────────
    BigDecimal volatilizationMinEffectiveTempF;
    BigDecimal volatilizationMaxSafeTempF;
    BigDecimal volatilizationOptimalTempF;
    BigDecimal volatilizationVaporPressureAt20c;
    String volatilizationEfficacyNotes;
    String volatilizationSafetyNotes;

    // ── SafetyProfile (nullable group) ───────────────────────────────────────
    String safetyHazardLevel;
    BigDecimal safetyMaxSafeConcentrationPpm;
    BigDecimal safetyMinApplicationTempF;   // nullable within the group
    BigDecimal safetyMaxApplicationTempF;   // nullable within the group
    Boolean safetyRequiresProtectiveEquipment;
    Boolean safetyHazardousToBeesWhenWet;
    Boolean safetyRequiresEveningApplication;
    String safetyApplicationConstraints;

    static CompoundDbo from(Compound c) {
        CompoundDbo d = new CompoundDbo();
        d.name = c.name().value();
        d.commonName = c.commonName();
        d.omriListed = c.omriListed();
        d.cdfaRegistered = c.cdfaRegistered();

        CompoundInfo info = c.compoundInfo();
        d.formula = info.formula();
        d.molecularWeight = info.molecularWeight() == null ? null : info.molecularWeight().value();
        d.phCharacter = info.phCharacter().name();
        d.chemicalNature = info.chemicalNature().name();
        d.physicalForm = info.physicalForm().name();
        d.structuralType = info.structuralType().kind();

        SolubilityProfile sol = c.solubility();
        d.solubilityGramsPerLiterAt20c = sol.gramsPerLiterAt20C().value();
        d.solubilityCategory = sol.category().name();
        d.solubilityEcContributionFactor = sol.ecContributionFactor();
        d.solubilityNotes = sol.notes();

        BioavailabilityProfile bio = c.bioavailability();
        d.bioavailabilityPrimaryPathway = bio.primaryPathway().name();
        d.bioavailabilityRelativeAbsorptionRate = bio.relativeAbsorptionRate();
        d.bioavailabilityCuticular = bio.cuticular();
        d.bioavailabilityStomatal = bio.stomatal();
        d.bioavailabilityChelateEnhanced = bio.isChelateEnhanced();
        d.bioavailabilityMechanism = bio.mechanism();

        VolatilizationProfile vol = c.volatilization();
        if (vol != null) {
            d.volatilizationMinEffectiveTempF = vol.minEffectiveTempF().value();
            d.volatilizationMaxSafeTempF = vol.maxSafeTempF().value();
            d.volatilizationOptimalTempF = vol.optimalTempF().value();
            d.volatilizationVaporPressureAt20c = vol.vaporPressureAt20C();
            d.volatilizationEfficacyNotes = vol.efficacyNotes();
            d.volatilizationSafetyNotes = vol.safetyNotes();
        }

        SafetyProfile safety = c.safety();
        if (safety != null) {
            d.safetyHazardLevel = safety.hazardLevel().name();
            d.safetyMaxSafeConcentrationPpm = safety.maxSafeConcentrationPpm();
            d.safetyMinApplicationTempF =
                    safety.minApplicationTempF() == null ? null : safety.minApplicationTempF().value();
            d.safetyMaxApplicationTempF =
                    safety.maxApplicationTempF() == null ? null : safety.maxApplicationTempF().value();
            d.safetyRequiresProtectiveEquipment = safety.requiresProtectiveEquipment();
            d.safetyHazardousToBeesWhenWet = safety.hazardousToBeesWhenWet();
            d.safetyRequiresEveningApplication = safety.requiresEveningApplication();
            d.safetyApplicationConstraints = safety.applicationConstraints();
        }

        Observer.forClass(CompoundDbo.class)
                .arguments("from", i -> i.observable(d, "dbo"))
                .throwWhenInvalid();
        return d;
    }

    /**
     * Reassembles the aggregate from this parent row plus its child-table members.
     * The adapter supplies the {@code functionalRoles}, {@code constituentElements}, and
     * {@code properties} it loaded from the child tables.
     */
    Compound toEntity(Set<FunctionalRole> functionalRoles,
                      Set<PeriodicElement> constituentElements,
                      Map<String, String> properties) {
        CompoundInfo info = new CompoundInfo(
                formula,
                molecularWeight == null ? null : MolecularWeight.of(molecularWeight),
                PhCharacter.valueOf(phCharacter),
                ChemicalNature.valueOf(chemicalNature),
                PhysicalForm.valueOf(physicalForm),
                StructuralType.ofKind(structuralType),
                functionalRoles,
                constituentElements);

        SolubilityProfile solubility = new SolubilityProfile(
                Solubility.of(solubilityGramsPerLiterAt20c),
                SolubilityProfile.SolubilityCategory.valueOf(solubilityCategory),
                solubilityEcContributionFactor,
                solubilityNotes);

        BioavailabilityProfile bioavailability = new BioavailabilityProfile(
                BioavailabilityProfile.AbsorptionPathway.valueOf(bioavailabilityPrimaryPathway),
                bioavailabilityRelativeAbsorptionRate,
                bioavailabilityCuticular,
                bioavailabilityStomatal,
                bioavailabilityChelateEnhanced,
                bioavailabilityMechanism);

        VolatilizationProfile volatilization = volatilizationMinEffectiveTempF == null ? null
                : new VolatilizationProfile(
                        TemperatureFahrenheit.of(volatilizationMinEffectiveTempF),
                        TemperatureFahrenheit.of(volatilizationMaxSafeTempF),
                        TemperatureFahrenheit.of(volatilizationOptimalTempF),
                        volatilizationVaporPressureAt20c,
                        volatilizationEfficacyNotes,
                        volatilizationSafetyNotes);

        SafetyProfile safety = safetyHazardLevel == null ? null
                : new SafetyProfile(
                        SafetyProfile.HazardLevel.valueOf(safetyHazardLevel),
                        safetyMaxSafeConcentrationPpm,
                        safetyMinApplicationTempF == null ? null : TemperatureFahrenheit.of(safetyMinApplicationTempF),
                        safetyMaxApplicationTempF == null ? null : TemperatureFahrenheit.of(safetyMaxApplicationTempF),
                        safetyRequiresProtectiveEquipment,
                        safetyHazardousToBeesWhenWet,
                        safetyRequiresEveningApplication,
                        safetyApplicationConstraints);

        return new Compound(
                CompoundName.of(name),
                commonName,
                info,
                solubility,
                bioavailability,
                volatilization,
                safety,
                omriListed,
                cdfaRegistered,
                properties);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return c -> c
                .notNull(name, "name").kebabFormat(name, "name").maxLength(name, 96, "name")
                .notBlank(commonName, "commonName").maxLength(commonName, 128, "commonName")
                .notBlank(formula, "formula").maxLength(formula, 64, "formula")
                .notBlank(phCharacter, "phCharacter")
                .notBlank(chemicalNature, "chemicalNature")
                .notBlank(physicalForm, "physicalForm")
                .notBlank(structuralType, "structuralType")
                .notNull(solubilityGramsPerLiterAt20c, "solubilityGramsPerLiterAt20c")
                .notBlank(solubilityCategory, "solubilityCategory")
                .notNull(solubilityEcContributionFactor, "solubilityEcContributionFactor")
                .notBlank(bioavailabilityPrimaryPathway, "bioavailabilityPrimaryPathway")
                .notNull(bioavailabilityRelativeAbsorptionRate, "bioavailabilityRelativeAbsorptionRate")
                .notBlank(bioavailabilityMechanism, "bioavailabilityMechanism");
    }
}
