package com.naturalist.insects;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Collection;
import java.util.List;

/**
 * MyBatis mapper for the {@code insect_species} aggregate and its five child tables
 * ({@code insect_species_common_name}, {@code insect_species_protected_stage},
 * {@code insect_species_habitat_zone}, {@code insect_species_habitat_layer},
 * {@code insect_species_supporting_plant}). Parent reads JOIN {@code insect_genus} to project the
 * parent genus's {@code name}; each child is loaded in one batched query keyed on the species-name
 * set (projecting {@code insect_species.name} so the adapter can group by name). Parent writes
 * nested-select {@code genus_id} from the genus name, so a missing parent inserts zero rows; child
 * writes nested-select {@code species_id} from the species name. The supporting-plant list is
 * order-significant — its read carries {@code ORDER BY ordinal} and its write persists the ordinal.
 */
@Mapper
interface InsectSpeciesMapper {

    String COLUMNS = """
        s.name, g.name AS genus_name, s.epithet, s.placed_in, s.sighting_notes,
        s.chem_mechanism, s.chem_source_compounds, s.chem_aposematic_signal,
        s.voltinism_pattern, s.voltinism_notes,
        s.habitat_moisture, s.habitat_light,
        s.req_nectar_sources, s.req_shelter, s.req_prey_availability, s.req_lighting, s.req_elevation_range,
        s.garden_relationship_to_other_beneficials, s.garden_natural_enemies,
        s.beneficial_significance, s.beneficial_pest_management_value, s.beneficial_ipm_notes,
        s.eco_indicator_value, s.eco_food_web_position, s.eco_regional_context,
        s.description_preschool, s.description_elementary, s.description_secondary, s.description_university
        """;

    String FROM = " FROM insect_species s JOIN insect_genus g ON g.id = s.genus_id ";

    // ── parent reads ─────────────────────────────────────────────────────────
    @Select("SELECT " + COLUMNS + FROM + " WHERE s.name = #{name}")
    InsectSpeciesDbo selectByName(String name);

    @Select("""
        <script>
        SELECT s.name, g.name AS genus_name, s.epithet, s.placed_in, s.sighting_notes,
               s.chem_mechanism, s.chem_source_compounds, s.chem_aposematic_signal,
               s.voltinism_pattern, s.voltinism_notes,
               s.habitat_moisture, s.habitat_light,
               s.req_nectar_sources, s.req_shelter, s.req_prey_availability, s.req_lighting, s.req_elevation_range,
               s.garden_relationship_to_other_beneficials, s.garden_natural_enemies,
               s.beneficial_significance, s.beneficial_pest_management_value, s.beneficial_ipm_notes,
               s.eco_indicator_value, s.eco_food_web_position, s.eco_regional_context,
               s.description_preschool, s.description_elementary, s.description_secondary, s.description_university
        FROM insect_species s JOIN insect_genus g ON g.id = s.genus_id
        WHERE s.name IN
        <foreach item='n' collection='names' open='(' separator=',' close=')'>#{n}</foreach>
        </script>
        """)
    List<InsectSpeciesDbo> selectByNameSet(@Param("names") Collection<String> names);

    @Select("SELECT " + COLUMNS + FROM + " WHERE g.name = #{genusName} ORDER BY s.name")
    List<InsectSpeciesDbo> selectByGenusName(String genusName);

    @Select("""
        <script>
        SELECT s.name, g.name AS genus_name, s.epithet, s.placed_in, s.sighting_notes,
               s.chem_mechanism, s.chem_source_compounds, s.chem_aposematic_signal,
               s.voltinism_pattern, s.voltinism_notes,
               s.habitat_moisture, s.habitat_light,
               s.req_nectar_sources, s.req_shelter, s.req_prey_availability, s.req_lighting, s.req_elevation_range,
               s.garden_relationship_to_other_beneficials, s.garden_natural_enemies,
               s.beneficial_significance, s.beneficial_pest_management_value, s.beneficial_ipm_notes,
               s.eco_indicator_value, s.eco_food_web_position, s.eco_regional_context,
               s.description_preschool, s.description_elementary, s.description_secondary, s.description_university
        FROM insect_species s JOIN insect_genus g ON g.id = s.genus_id
        WHERE g.name IN
        <foreach item='n' collection='names' open='(' separator=',' close=')'>#{n}</foreach>
        ORDER BY s.name
        </script>
        """)
    List<InsectSpeciesDbo> selectByGenusNames(@Param("names") Collection<String> genusNames);

    @Select("SELECT " + COLUMNS + FROM + " ORDER BY s.name LIMIT #{limit} OFFSET #{offset}")
    List<InsectSpeciesDbo> selectPage(@Param("limit") int limit, @Param("offset") int offset);

    @Select("""
        SELECT count(*) FROM (
            SELECT 1 FROM insect_species ORDER BY name OFFSET #{skip} LIMIT #{window}
        ) t
        """)
    int countInWindow(@Param("skip") int skip, @Param("window") int window);

    // ── child reads (one batched query each; project insect_species.name for grouping) ──
    @Select("""
        <script>
        SELECT s.name AS species_name, cn.label, cn.locale
        FROM insect_species_common_name cn JOIN insect_species s ON s.id = cn.species_id
        WHERE s.name IN
        <foreach item='n' collection='names' open='(' separator=',' close=')'>#{n}</foreach>
        </script>
        """)
    List<InsectSpeciesCommonNameDbo> selectCommonNames(@Param("names") Collection<String> names);

    @Select("""
        <script>
        SELECT s.name AS species_name, ps.stage_kind
        FROM insect_species_protected_stage ps JOIN insect_species s ON s.id = ps.species_id
        WHERE s.name IN
        <foreach item='n' collection='names' open='(' separator=',' close=')'>#{n}</foreach>
        </script>
        """)
    List<InsectSpeciesProtectedStageDbo> selectProtectedStages(@Param("names") Collection<String> names);

    @Select("""
        <script>
        SELECT s.name AS species_name, hz.zone
        FROM insect_species_habitat_zone hz JOIN insect_species s ON s.id = hz.species_id
        WHERE s.name IN
        <foreach item='n' collection='names' open='(' separator=',' close=')'>#{n}</foreach>
        </script>
        """)
    List<InsectSpeciesHabitatZoneDbo> selectHabitatZones(@Param("names") Collection<String> names);

    @Select("""
        <script>
        SELECT s.name AS species_name, hl.layer
        FROM insect_species_habitat_layer hl JOIN insect_species s ON s.id = hl.species_id
        WHERE s.name IN
        <foreach item='n' collection='names' open='(' separator=',' close=')'>#{n}</foreach>
        </script>
        """)
    List<InsectSpeciesHabitatLayerDbo> selectHabitatLayers(@Param("names") Collection<String> names);

    @Select("""
        <script>
        SELECT s.name AS species_name, sp.ordinal, sp.plant
        FROM insect_species_supporting_plant sp JOIN insect_species s ON s.id = sp.species_id
        WHERE s.name IN
        <foreach item='n' collection='names' open='(' separator=',' close=')'>#{n}</foreach>
        ORDER BY s.name, sp.ordinal
        </script>
        """)
    List<InsectSpeciesSupportingPlantDbo> selectSupportingPlants(@Param("names") Collection<String> names);

    // ── parent writes (nested-select the genus_id from the genus name) ───────
    @Insert("""
        INSERT INTO insect_species (name, genus_id, epithet, placed_in, sighting_notes,
            chem_mechanism, chem_source_compounds, chem_aposematic_signal,
            voltinism_pattern, voltinism_notes,
            habitat_moisture, habitat_light,
            req_nectar_sources, req_shelter, req_prey_availability, req_lighting, req_elevation_range,
            garden_relationship_to_other_beneficials, garden_natural_enemies,
            beneficial_significance, beneficial_pest_management_value, beneficial_ipm_notes,
            eco_indicator_value, eco_food_web_position, eco_regional_context,
            description_preschool, description_elementary, description_secondary, description_university)
        SELECT #{name}, id, #{epithet}, #{placedIn}, #{sightingNotes},
            #{chemMechanism}, #{chemSourceCompounds}, #{chemAposematicSignal},
            #{voltinismPattern}, #{voltinismNotes},
            #{habitatMoisture}, #{habitatLight},
            #{reqNectarSources}, #{reqShelter}, #{reqPreyAvailability}, #{reqLighting}, #{reqElevationRange},
            #{gardenRelationshipToOtherBeneficials}, #{gardenNaturalEnemies},
            #{beneficialSignificance}, #{beneficialPestManagementValue}, #{beneficialIpmNotes},
            #{ecoIndicatorValue}, #{ecoFoodWebPosition}, #{ecoRegionalContext},
            #{descriptionPreschool}, #{descriptionElementary}, #{descriptionSecondary}, #{descriptionUniversity}
        FROM insect_genus WHERE name = #{genusName}
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(InsectSpeciesDbo dbo);

    @Update("""
        UPDATE insect_species SET
            genus_id = (SELECT id FROM insect_genus WHERE name = #{genusName}),
            epithet = #{epithet},
            placed_in = #{placedIn},
            sighting_notes = #{sightingNotes},
            chem_mechanism = #{chemMechanism},
            chem_source_compounds = #{chemSourceCompounds},
            chem_aposematic_signal = #{chemAposematicSignal},
            voltinism_pattern = #{voltinismPattern},
            voltinism_notes = #{voltinismNotes},
            habitat_moisture = #{habitatMoisture},
            habitat_light = #{habitatLight},
            req_nectar_sources = #{reqNectarSources},
            req_shelter = #{reqShelter},
            req_prey_availability = #{reqPreyAvailability},
            req_lighting = #{reqLighting},
            req_elevation_range = #{reqElevationRange},
            garden_relationship_to_other_beneficials = #{gardenRelationshipToOtherBeneficials},
            garden_natural_enemies = #{gardenNaturalEnemies},
            beneficial_significance = #{beneficialSignificance},
            beneficial_pest_management_value = #{beneficialPestManagementValue},
            beneficial_ipm_notes = #{beneficialIpmNotes},
            eco_indicator_value = #{ecoIndicatorValue},
            eco_food_web_position = #{ecoFoodWebPosition},
            eco_regional_context = #{ecoRegionalContext},
            description_preschool = #{descriptionPreschool},
            description_elementary = #{descriptionElementary},
            description_secondary = #{descriptionSecondary},
            description_university = #{descriptionUniversity}
        WHERE name = #{name}
        """)
    int updateByName(InsectSpeciesDbo dbo);

    // ── child writes (nested-select the species_id from the name) ────────────
    @Insert("""
        INSERT INTO insect_species_common_name (species_id, label, locale)
        SELECT id, #{label}, #{locale} FROM insect_species WHERE name = #{speciesName}
        """)
    int insertCommonName(InsectSpeciesCommonNameDbo dbo);

    @Insert("""
        INSERT INTO insect_species_protected_stage (species_id, stage_kind)
        SELECT id, #{stageKind} FROM insect_species WHERE name = #{speciesName}
        """)
    int insertProtectedStage(InsectSpeciesProtectedStageDbo dbo);

    @Insert("""
        INSERT INTO insect_species_habitat_zone (species_id, zone)
        SELECT id, #{zone} FROM insect_species WHERE name = #{speciesName}
        """)
    int insertHabitatZone(InsectSpeciesHabitatZoneDbo dbo);

    @Insert("""
        INSERT INTO insect_species_habitat_layer (species_id, layer)
        SELECT id, #{layer} FROM insect_species WHERE name = #{speciesName}
        """)
    int insertHabitatLayer(InsectSpeciesHabitatLayerDbo dbo);

    @Insert("""
        INSERT INTO insect_species_supporting_plant (species_id, ordinal, plant)
        SELECT id, #{ordinal}, #{plant} FROM insect_species WHERE name = #{speciesName}
        """)
    int insertSupportingPlant(InsectSpeciesSupportingPlantDbo dbo);

    @Delete("DELETE FROM insect_species_common_name WHERE species_id = (SELECT id FROM insect_species WHERE name = #{name})")
    void deleteCommonNames(String name);

    @Delete("DELETE FROM insect_species_protected_stage WHERE species_id = (SELECT id FROM insect_species WHERE name = #{name})")
    void deleteProtectedStages(String name);

    @Delete("DELETE FROM insect_species_habitat_zone WHERE species_id = (SELECT id FROM insect_species WHERE name = #{name})")
    void deleteHabitatZones(String name);

    @Delete("DELETE FROM insect_species_habitat_layer WHERE species_id = (SELECT id FROM insect_species WHERE name = #{name})")
    void deleteHabitatLayers(String name);

    @Delete("DELETE FROM insect_species_supporting_plant WHERE species_id = (SELECT id FROM insect_species WHERE name = #{name})")
    void deleteSupportingPlants(String name);
}
