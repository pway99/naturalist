package com.naturalist.chemistry.compound;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Collection;
import java.util.List;

/**
 * MyBatis mapper for the {@code compound} aggregate. The parent row's 1:1 owned value objects
 * are flat columns (auto-mapped snake→camel); the multi-valued members live in child tables and
 * are loaded in a single batched query per child type (keyed on {@code compound_id}, projecting
 * {@code compound.name} so the adapter can group by name). Child inserts nested-select the
 * {@code compound_id} from the compound name, so no generated id has to be threaded back.
 */
@Mapper
interface CompoundMapper {

    String COLUMNS = """
        name, common_name, omri_listed, cdfa_registered,
        formula, molecular_weight, ph_character, chemical_nature, physical_form, structural_type,
        solubility_grams_per_liter_at_20c, solubility_category, solubility_ec_contribution_factor, solubility_notes,
        bioavailability_primary_pathway, bioavailability_relative_absorption_rate, bioavailability_cuticular,
        bioavailability_stomatal, bioavailability_chelate_enhanced, bioavailability_mechanism,
        volatilization_min_effective_temp_f, volatilization_max_safe_temp_f, volatilization_optimal_temp_f,
        volatilization_vapor_pressure_at_20c, volatilization_efficacy_notes, volatilization_safety_notes,
        safety_hazard_level, safety_max_safe_concentration_ppm, safety_min_application_temp_f,
        safety_max_application_temp_f, safety_requires_protective_equipment, safety_hazardous_to_bees_when_wet,
        safety_requires_evening_application, safety_application_constraints
        """;

    // ── parent reads ─────────────────────────────────────────────────────────
    @Select("SELECT " + COLUMNS + " FROM compound WHERE name = #{name}")
    CompoundDbo selectByName(String name);

    @Select("""
        <script>
        SELECT name, common_name, omri_listed, cdfa_registered,
               formula, molecular_weight, ph_character, chemical_nature, physical_form, structural_type,
               solubility_grams_per_liter_at_20c, solubility_category, solubility_ec_contribution_factor, solubility_notes,
               bioavailability_primary_pathway, bioavailability_relative_absorption_rate, bioavailability_cuticular,
               bioavailability_stomatal, bioavailability_chelate_enhanced, bioavailability_mechanism,
               volatilization_min_effective_temp_f, volatilization_max_safe_temp_f, volatilization_optimal_temp_f,
               volatilization_vapor_pressure_at_20c, volatilization_efficacy_notes, volatilization_safety_notes,
               safety_hazard_level, safety_max_safe_concentration_ppm, safety_min_application_temp_f,
               safety_max_application_temp_f, safety_requires_protective_equipment, safety_hazardous_to_bees_when_wet,
               safety_requires_evening_application, safety_application_constraints
        FROM compound WHERE name IN
        <foreach item='n' collection='names' open='(' separator=',' close=')'>#{n}</foreach>
        </script>
        """)
    List<CompoundDbo> selectByNameSet(@Param("names") Collection<String> names);

    @Select("SELECT " + COLUMNS + " FROM compound ORDER BY name LIMIT #{limit} OFFSET #{offset}")
    List<CompoundDbo> selectPage(@Param("limit") int limit, @Param("offset") int offset);

    @Select("""
        SELECT count(*) FROM (
            SELECT 1 FROM compound ORDER BY name OFFSET #{skip} LIMIT #{window}
        ) t
        """)
    int countInWindow(@Param("skip") int skip, @Param("window") int window);

    // ── child reads (one batched query each; project compound.name for grouping) ──
    @Select("""
        <script>
        SELECT c.name AS compound_name, r.functional_role
        FROM compound_functional_role r JOIN compound c ON c.id = r.compound_id
        WHERE c.name IN
        <foreach item='n' collection='names' open='(' separator=',' close=')'>#{n}</foreach>
        </script>
        """)
    List<CompoundFunctionalRoleDbo> selectFunctionalRoles(@Param("names") Collection<String> names);

    @Select("""
        <script>
        SELECT c.name AS compound_name, e.element_symbol
        FROM compound_constituent_element e JOIN compound c ON c.id = e.compound_id
        WHERE c.name IN
        <foreach item='n' collection='names' open='(' separator=',' close=')'>#{n}</foreach>
        </script>
        """)
    List<CompoundConstituentElementDbo> selectConstituentElements(@Param("names") Collection<String> names);

    @Select("""
        <script>
        SELECT c.name AS compound_name, p.property_key, p.property_value
        FROM compound_property p JOIN compound c ON c.id = p.compound_id
        WHERE c.name IN
        <foreach item='n' collection='names' open='(' separator=',' close=')'>#{n}</foreach>
        </script>
        """)
    List<CompoundPropertyDbo> selectProperties(@Param("names") Collection<String> names);

    // ── parent writes ────────────────────────────────────────────────────────
    @Insert("""
        INSERT INTO compound (
            name, common_name, omri_listed, cdfa_registered,
            formula, molecular_weight, ph_character, chemical_nature, physical_form, structural_type,
            solubility_grams_per_liter_at_20c, solubility_category, solubility_ec_contribution_factor, solubility_notes,
            bioavailability_primary_pathway, bioavailability_relative_absorption_rate, bioavailability_cuticular,
            bioavailability_stomatal, bioavailability_chelate_enhanced, bioavailability_mechanism,
            volatilization_min_effective_temp_f, volatilization_max_safe_temp_f, volatilization_optimal_temp_f,
            volatilization_vapor_pressure_at_20c, volatilization_efficacy_notes, volatilization_safety_notes,
            safety_hazard_level, safety_max_safe_concentration_ppm, safety_min_application_temp_f,
            safety_max_application_temp_f, safety_requires_protective_equipment, safety_hazardous_to_bees_when_wet,
            safety_requires_evening_application, safety_application_constraints)
        VALUES (
            #{name}, #{commonName}, #{omriListed}, #{cdfaRegistered},
            #{formula}, #{molecularWeight}, #{phCharacter}, #{chemicalNature}, #{physicalForm}, #{structuralType},
            #{solubilityGramsPerLiterAt20c}, #{solubilityCategory}, #{solubilityEcContributionFactor}, #{solubilityNotes},
            #{bioavailabilityPrimaryPathway}, #{bioavailabilityRelativeAbsorptionRate}, #{bioavailabilityCuticular},
            #{bioavailabilityStomatal}, #{bioavailabilityChelateEnhanced}, #{bioavailabilityMechanism},
            #{volatilizationMinEffectiveTempF}, #{volatilizationMaxSafeTempF}, #{volatilizationOptimalTempF},
            #{volatilizationVaporPressureAt20c}, #{volatilizationEfficacyNotes}, #{volatilizationSafetyNotes},
            #{safetyHazardLevel}, #{safetyMaxSafeConcentrationPpm}, #{safetyMinApplicationTempF},
            #{safetyMaxApplicationTempF}, #{safetyRequiresProtectiveEquipment}, #{safetyHazardousToBeesWhenWet},
            #{safetyRequiresEveningApplication}, #{safetyApplicationConstraints})
        """)
    void insertCompound(CompoundDbo dbo);

    @Update("""
        UPDATE compound SET
            common_name = #{commonName}, omri_listed = #{omriListed}, cdfa_registered = #{cdfaRegistered},
            formula = #{formula}, molecular_weight = #{molecularWeight}, ph_character = #{phCharacter},
            chemical_nature = #{chemicalNature}, physical_form = #{physicalForm}, structural_type = #{structuralType},
            solubility_grams_per_liter_at_20c = #{solubilityGramsPerLiterAt20c},
            solubility_category = #{solubilityCategory},
            solubility_ec_contribution_factor = #{solubilityEcContributionFactor}, solubility_notes = #{solubilityNotes},
            bioavailability_primary_pathway = #{bioavailabilityPrimaryPathway},
            bioavailability_relative_absorption_rate = #{bioavailabilityRelativeAbsorptionRate},
            bioavailability_cuticular = #{bioavailabilityCuticular}, bioavailability_stomatal = #{bioavailabilityStomatal},
            bioavailability_chelate_enhanced = #{bioavailabilityChelateEnhanced},
            bioavailability_mechanism = #{bioavailabilityMechanism},
            volatilization_min_effective_temp_f = #{volatilizationMinEffectiveTempF},
            volatilization_max_safe_temp_f = #{volatilizationMaxSafeTempF},
            volatilization_optimal_temp_f = #{volatilizationOptimalTempF},
            volatilization_vapor_pressure_at_20c = #{volatilizationVaporPressureAt20c},
            volatilization_efficacy_notes = #{volatilizationEfficacyNotes},
            volatilization_safety_notes = #{volatilizationSafetyNotes},
            safety_hazard_level = #{safetyHazardLevel},
            safety_max_safe_concentration_ppm = #{safetyMaxSafeConcentrationPpm},
            safety_min_application_temp_f = #{safetyMinApplicationTempF},
            safety_max_application_temp_f = #{safetyMaxApplicationTempF},
            safety_requires_protective_equipment = #{safetyRequiresProtectiveEquipment},
            safety_hazardous_to_bees_when_wet = #{safetyHazardousToBeesWhenWet},
            safety_requires_evening_application = #{safetyRequiresEveningApplication},
            safety_application_constraints = #{safetyApplicationConstraints}
        WHERE name = #{name}
        """)
    int updateCompound(CompoundDbo dbo);

    // ── child writes (nested-select the compound_id from the name) ───────────
    @Insert("""
        INSERT INTO compound_functional_role (compound_id, functional_role)
        SELECT id, #{functionalRole} FROM compound WHERE name = #{compoundName}
        """)
    int insertFunctionalRole(CompoundFunctionalRoleDbo dbo);

    @Insert("""
        INSERT INTO compound_constituent_element (compound_id, element_symbol)
        SELECT id, #{elementSymbol} FROM compound WHERE name = #{compoundName}
        """)
    int insertConstituentElement(CompoundConstituentElementDbo dbo);

    @Insert("""
        INSERT INTO compound_property (compound_id, property_key, property_value)
        SELECT id, #{propertyKey}, #{propertyValue} FROM compound WHERE name = #{compoundName}
        """)
    int insertProperty(CompoundPropertyDbo dbo);

    @Delete("DELETE FROM compound_functional_role WHERE compound_id = (SELECT id FROM compound WHERE name = #{name})")
    void deleteFunctionalRoles(String name);

    @Delete("DELETE FROM compound_constituent_element WHERE compound_id = (SELECT id FROM compound WHERE name = #{name})")
    void deleteConstituentElements(String name);

    @Delete("DELETE FROM compound_property WHERE compound_id = (SELECT id FROM compound WHERE name = #{name})")
    void deleteProperties(String name);
}
