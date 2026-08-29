package com.naturalist.insects.lifestage;

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
 * MyBatis mapper for the single-table {@code insect_life_stage} + its six child tables (windows, habitat
 * zones/layers, larva host plants, larva parasitoid hosts, adult nectar sources). Parent {@code id} is
 * DB-generated; child rows carry the life-stage name (JOIN-project on read, nested-select the id on write);
 * each child type loads in one batched query keyed on the name set (no N+1).
 */
@Mapper
interface InsectLifeStageMapper {

    String COLUMNS = """
        id, name, stage_kind, parent_rank, parent_name, phenology_notes,
        habitat_moisture, habitat_light, habitat_substrate, habitat_microclimate, habitat_spatial_notes,
        chemistry_role, chemistry_notes,
        egg_color_progression, egg_laying_pattern, egg_adaptive_significance,
        larva_feeding_strategy, larva_remarkable_behavior, larva_instar_progression,
        pupa_appearance, pupa_adaptive_significance, diapause_kind, diapause_field_a, diapause_field_b,
        adult_feeding_habit, adult_ecological_role, adult_lifespan,
        description_preschool, description_elementary, description_secondary, description_university
        """;

    @Select("SELECT " + COLUMNS + " FROM insect_life_stage WHERE name = #{name}")
    InsectLifeStageDbo selectByName(String name);

    @Select("""
        <script>
        SELECT id, name, stage_kind, parent_rank, parent_name, phenology_notes,
               habitat_moisture, habitat_light, habitat_substrate, habitat_microclimate, habitat_spatial_notes,
               chemistry_role, chemistry_notes,
               egg_color_progression, egg_laying_pattern, egg_adaptive_significance,
               larva_feeding_strategy, larva_remarkable_behavior, larva_instar_progression,
               pupa_appearance, pupa_adaptive_significance, diapause_kind, diapause_field_a, diapause_field_b,
               adult_feeding_habit, adult_ecological_role, adult_lifespan,
               description_preschool, description_elementary, description_secondary, description_university
        FROM insect_life_stage WHERE name IN
        <foreach item='n' collection='names' open='(' separator=',' close=')'>#{n}</foreach>
        </script>
        """)
    List<InsectLifeStageDbo> selectByNameSet(@Param("names") Collection<String> names);

    @Select("SELECT " + COLUMNS + " FROM insect_life_stage ORDER BY name LIMIT #{limit} OFFSET #{offset}")
    List<InsectLifeStageDbo> selectPage(@Param("limit") int limit, @Param("offset") int offset);

    @Select("""
        SELECT count(*) FROM (
            SELECT 1 FROM insect_life_stage ORDER BY name OFFSET #{skip} LIMIT #{window}
        ) t
        """)
    int countInWindow(@Param("skip") int skip, @Param("window") int window);

    @Select("SELECT " + COLUMNS + " FROM insect_life_stage WHERE parent_rank = #{rank} AND parent_name = #{name} ORDER BY name")
    List<InsectLifeStageDbo> selectByParentName(@Param("rank") String rank, @Param("name") String name);

    // ── child reads (batched; project life_stage name for grouping) ─────────────
    @Select("""
        <script>
        SELECT ls.name AS life_stage_name, w.ordinal, w.onset, w.peak, w.tail, w.cohort_label
        FROM insect_life_stage_window w JOIN insect_life_stage ls ON ls.id = w.life_stage_id
        WHERE ls.name IN <foreach item='n' collection='names' open='(' separator=',' close=')'>#{n}</foreach>
        ORDER BY ls.name, w.ordinal
        </script>
        """)
    List<InsectLifeStageWindowDbo> selectWindows(@Param("names") Collection<String> names);

    @Select("""
        <script>
        SELECT ls.name AS life_stage_name, z.zone
        FROM insect_life_stage_habitat_zone z JOIN insect_life_stage ls ON ls.id = z.life_stage_id
        WHERE ls.name IN <foreach item='n' collection='names' open='(' separator=',' close=')'>#{n}</foreach>
        </script>
        """)
    List<InsectLifeStageHabitatZoneDbo> selectHabitatZones(@Param("names") Collection<String> names);

    @Select("""
        <script>
        SELECT ls.name AS life_stage_name, l.layer
        FROM insect_life_stage_habitat_layer l JOIN insect_life_stage ls ON ls.id = l.life_stage_id
        WHERE ls.name IN <foreach item='n' collection='names' open='(' separator=',' close=')'>#{n}</foreach>
        </script>
        """)
    List<InsectLifeStageHabitatLayerDbo> selectHabitatLayers(@Param("names") Collection<String> names);

    @Select("""
        <script>
        SELECT ls.name AS life_stage_name, h.ordinal, h.plant_name
        FROM insect_life_stage_host_plant h JOIN insect_life_stage ls ON ls.id = h.life_stage_id
        WHERE ls.name IN <foreach item='n' collection='names' open='(' separator=',' close=')'>#{n}</foreach>
        ORDER BY ls.name, h.ordinal
        </script>
        """)
    List<InsectLifeStageHostPlantDbo> selectHostPlants(@Param("names") Collection<String> names);

    @Select("""
        <script>
        SELECT ls.name AS life_stage_name, p.ordinal, p.species_name
        FROM insect_life_stage_parasitoid_host p JOIN insect_life_stage ls ON ls.id = p.life_stage_id
        WHERE ls.name IN <foreach item='n' collection='names' open='(' separator=',' close=')'>#{n}</foreach>
        ORDER BY ls.name, p.ordinal
        </script>
        """)
    List<InsectLifeStageParasitoidHostDbo> selectParasitoidHosts(@Param("names") Collection<String> names);

    @Select("""
        <script>
        SELECT ls.name AS life_stage_name, n.ordinal, n.plant_name
        FROM insect_life_stage_nectar_source n JOIN insect_life_stage ls ON ls.id = n.life_stage_id
        WHERE ls.name IN <foreach item='x' collection='names' open='(' separator=',' close=')'>#{x}</foreach>
        ORDER BY ls.name, n.ordinal
        </script>
        """)
    List<InsectLifeStageNectarSourceDbo> selectNectarSources(@Param("names") Collection<String> names);

    // ── parent writes ───────────────────────────────────────────────────────────
    @Insert("""
        INSERT INTO insect_life_stage (name, stage_kind, parent_rank, parent_name, phenology_notes,
            habitat_moisture, habitat_light, habitat_substrate, habitat_microclimate, habitat_spatial_notes,
            chemistry_role, chemistry_notes,
            egg_color_progression, egg_laying_pattern, egg_adaptive_significance,
            larva_feeding_strategy, larva_remarkable_behavior, larva_instar_progression,
            pupa_appearance, pupa_adaptive_significance, diapause_kind, diapause_field_a, diapause_field_b,
            adult_feeding_habit, adult_ecological_role, adult_lifespan,
            description_preschool, description_elementary, description_secondary, description_university)
        VALUES (#{name}, #{stageKind}, #{parentRank}, #{parentName}, #{phenologyNotes},
            #{habitatMoisture}, #{habitatLight}, #{habitatSubstrate}, #{habitatMicroclimate}, #{habitatSpatialNotes},
            #{chemistryRole}, #{chemistryNotes},
            #{eggColorProgression}, #{eggLayingPattern}, #{eggAdaptiveSignificance},
            #{larvaFeedingStrategy}, #{larvaRemarkableBehavior}, #{larvaInstarProgression},
            #{pupaAppearance}, #{pupaAdaptiveSignificance}, #{diapauseKind}, #{diapauseFieldA}, #{diapauseFieldB},
            #{adultFeedingHabit}, #{adultEcologicalRole}, #{adultLifespan},
            #{descriptionPreschool}, #{descriptionElementary}, #{descriptionSecondary}, #{descriptionUniversity})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    void insert(InsectLifeStageDbo dbo);

    @Update("""
        UPDATE insect_life_stage SET
            stage_kind = #{stageKind}, parent_rank = #{parentRank}, parent_name = #{parentName},
            phenology_notes = #{phenologyNotes},
            habitat_moisture = #{habitatMoisture}, habitat_light = #{habitatLight},
            habitat_substrate = #{habitatSubstrate}, habitat_microclimate = #{habitatMicroclimate},
            habitat_spatial_notes = #{habitatSpatialNotes},
            chemistry_role = #{chemistryRole}, chemistry_notes = #{chemistryNotes},
            egg_color_progression = #{eggColorProgression}, egg_laying_pattern = #{eggLayingPattern},
            egg_adaptive_significance = #{eggAdaptiveSignificance},
            larva_feeding_strategy = #{larvaFeedingStrategy}, larva_remarkable_behavior = #{larvaRemarkableBehavior},
            larva_instar_progression = #{larvaInstarProgression},
            pupa_appearance = #{pupaAppearance}, pupa_adaptive_significance = #{pupaAdaptiveSignificance},
            diapause_kind = #{diapauseKind}, diapause_field_a = #{diapauseFieldA}, diapause_field_b = #{diapauseFieldB},
            adult_feeding_habit = #{adultFeedingHabit}, adult_ecological_role = #{adultEcologicalRole},
            adult_lifespan = #{adultLifespan},
            description_preschool = #{descriptionPreschool}, description_elementary = #{descriptionElementary},
            description_secondary = #{descriptionSecondary}, description_university = #{descriptionUniversity}
        WHERE name = #{name}
        """)
    int updateByName(InsectLifeStageDbo dbo);

    // ── child writes (nested-select the life_stage_id from the name) ─────────────
    @Insert("""
        INSERT INTO insect_life_stage_window (life_stage_id, ordinal, onset, peak, tail, cohort_label)
        SELECT id, #{ordinal}, #{onset}, #{peak}, #{tail}, #{cohortLabel}
        FROM insect_life_stage WHERE name = #{lifeStageName}
        """)
    void insertWindow(InsectLifeStageWindowDbo dbo);

    @Insert("""
        INSERT INTO insect_life_stage_habitat_zone (life_stage_id, zone)
        SELECT id, #{zone} FROM insect_life_stage WHERE name = #{lifeStageName}
        """)
    void insertHabitatZone(InsectLifeStageHabitatZoneDbo dbo);

    @Insert("""
        INSERT INTO insect_life_stage_habitat_layer (life_stage_id, layer)
        SELECT id, #{layer} FROM insect_life_stage WHERE name = #{lifeStageName}
        """)
    void insertHabitatLayer(InsectLifeStageHabitatLayerDbo dbo);

    @Insert("""
        INSERT INTO insect_life_stage_host_plant (life_stage_id, ordinal, plant_name)
        SELECT id, #{ordinal}, #{plantName} FROM insect_life_stage WHERE name = #{lifeStageName}
        """)
    void insertHostPlant(InsectLifeStageHostPlantDbo dbo);

    @Insert("""
        INSERT INTO insect_life_stage_parasitoid_host (life_stage_id, ordinal, species_name)
        SELECT id, #{ordinal}, #{speciesName} FROM insect_life_stage WHERE name = #{lifeStageName}
        """)
    void insertParasitoidHost(InsectLifeStageParasitoidHostDbo dbo);

    @Insert("""
        INSERT INTO insect_life_stage_nectar_source (life_stage_id, ordinal, plant_name)
        SELECT id, #{ordinal}, #{plantName} FROM insect_life_stage WHERE name = #{lifeStageName}
        """)
    void insertNectarSource(InsectLifeStageNectarSourceDbo dbo);

    @Delete("DELETE FROM insect_life_stage_window WHERE life_stage_id = (SELECT id FROM insect_life_stage WHERE name = #{name})")
    void deleteWindows(String name);

    @Delete("DELETE FROM insect_life_stage_habitat_zone WHERE life_stage_id = (SELECT id FROM insect_life_stage WHERE name = #{name})")
    void deleteHabitatZones(String name);

    @Delete("DELETE FROM insect_life_stage_habitat_layer WHERE life_stage_id = (SELECT id FROM insect_life_stage WHERE name = #{name})")
    void deleteHabitatLayers(String name);

    @Delete("DELETE FROM insect_life_stage_host_plant WHERE life_stage_id = (SELECT id FROM insect_life_stage WHERE name = #{name})")
    void deleteHostPlants(String name);

    @Delete("DELETE FROM insect_life_stage_parasitoid_host WHERE life_stage_id = (SELECT id FROM insect_life_stage WHERE name = #{name})")
    void deleteParasitoidHosts(String name);

    @Delete("DELETE FROM insect_life_stage_nectar_source WHERE life_stage_id = (SELECT id FROM insect_life_stage WHERE name = #{name})")
    void deleteNectarSources(String name);
}
