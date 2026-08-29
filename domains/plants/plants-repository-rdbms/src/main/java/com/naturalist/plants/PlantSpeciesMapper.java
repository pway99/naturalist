package com.naturalist.plants;

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
 * MyBatis mapper for the {@code plant_species} table and its two child tables
 * ({@code plant_species_common_name}, {@code plant_species_native_bioregion}). Reads JOIN
 * {@code plant_genus} to project the parent genus's {@code name}; the children are each loaded in
 * one batched query keyed on the species-name set. Writes nested-select {@code genus_id} from the
 * genus name, so a missing parent inserts zero rows.
 */
@Mapper
interface PlantSpeciesMapper {

    String COLUMNS = """
        s.name, g.name AS genus_name, s.epithet, s.growth_habit, s.life_cycle,
        s.description_preschool, s.description_elementary, s.description_secondary, s.description_university
        """;

    String FROM = " FROM plant_species s JOIN plant_genus g ON g.id = s.genus_id ";

    // ── parent reads ─────────────────────────────────────────────────────────
    @Select("SELECT " + COLUMNS + FROM + " WHERE s.name = #{name}")
    PlantSpeciesDbo selectByName(String name);

    @Select("""
        <script>
        SELECT s.name, g.name AS genus_name, s.epithet, s.growth_habit, s.life_cycle,
               s.description_preschool, s.description_elementary, s.description_secondary, s.description_university
        FROM plant_species s JOIN plant_genus g ON g.id = s.genus_id
        WHERE s.name IN
        <foreach item='n' collection='names' open='(' separator=',' close=')'>#{n}</foreach>
        </script>
        """)
    List<PlantSpeciesDbo> selectByNameSet(@Param("names") Collection<String> names);

    @Select("SELECT " + COLUMNS + FROM + " WHERE g.name = #{genusName} ORDER BY s.name")
    List<PlantSpeciesDbo> selectByGenusName(String genusName);

    @Select("""
        <script>
        SELECT s.name, g.name AS genus_name, s.epithet, s.growth_habit, s.life_cycle,
               s.description_preschool, s.description_elementary, s.description_secondary, s.description_university
        FROM plant_species s JOIN plant_genus g ON g.id = s.genus_id
        WHERE g.name IN
        <foreach item='n' collection='names' open='(' separator=',' close=')'>#{n}</foreach>
        ORDER BY s.name
        </script>
        """)
    List<PlantSpeciesDbo> selectByGenusNames(@Param("names") Collection<String> genusNames);

    @Select("SELECT " + COLUMNS + FROM + " ORDER BY s.name LIMIT #{limit} OFFSET #{offset}")
    List<PlantSpeciesDbo> selectPage(@Param("limit") int limit, @Param("offset") int offset);

    @Select("""
        SELECT count(*) FROM (
            SELECT 1 FROM plant_species ORDER BY name OFFSET #{skip} LIMIT #{window}
        ) t
        """)
    int countInWindow(@Param("skip") int skip, @Param("window") int window);

    // ── child reads (one batched query each; project plant_species.name for grouping) ──
    @Select("""
        <script>
        SELECT s.name AS species_name, cn.label, cn.locale
        FROM plant_species_common_name cn JOIN plant_species s ON s.id = cn.species_id
        WHERE s.name IN
        <foreach item='n' collection='names' open='(' separator=',' close=')'>#{n}</foreach>
        </script>
        """)
    List<PlantSpeciesCommonNameDbo> selectCommonNames(@Param("names") Collection<String> names);

    @Select("""
        <script>
        SELECT s.name AS species_name, nb.bioregion
        FROM plant_species_native_bioregion nb JOIN plant_species s ON s.id = nb.species_id
        WHERE s.name IN
        <foreach item='n' collection='names' open='(' separator=',' close=')'>#{n}</foreach>
        </script>
        """)
    List<PlantSpeciesNativeBioregionDbo> selectNativeBioregions(@Param("names") Collection<String> names);

    // ── parent writes (nested-select the genus_id from the genus name) ───────
    @Insert("""
        INSERT INTO plant_species (name, genus_id, epithet, growth_habit, life_cycle,
            description_preschool, description_elementary, description_secondary, description_university)
        SELECT #{name}, id, #{epithet}, #{growthHabit}, #{lifeCycle},
            #{descriptionPreschool}, #{descriptionElementary}, #{descriptionSecondary}, #{descriptionUniversity}
        FROM plant_genus WHERE name = #{genusName}
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(PlantSpeciesDbo dbo);

    @Update("""
        UPDATE plant_species SET
            genus_id = (SELECT id FROM plant_genus WHERE name = #{genusName}),
            epithet = #{epithet},
            growth_habit = #{growthHabit},
            life_cycle = #{lifeCycle},
            description_preschool = #{descriptionPreschool},
            description_elementary = #{descriptionElementary},
            description_secondary = #{descriptionSecondary},
            description_university = #{descriptionUniversity}
        WHERE name = #{name}
        """)
    int updateByName(PlantSpeciesDbo dbo);

    // ── child writes (nested-select the species_id from the name) ────────────
    @Insert("""
        INSERT INTO plant_species_common_name (species_id, label, locale)
        SELECT id, #{label}, #{locale} FROM plant_species WHERE name = #{speciesName}
        """)
    int insertCommonName(PlantSpeciesCommonNameDbo dbo);

    @Insert("""
        INSERT INTO plant_species_native_bioregion (species_id, bioregion)
        SELECT id, #{bioregion} FROM plant_species WHERE name = #{speciesName}
        """)
    int insertNativeBioregion(PlantSpeciesNativeBioregionDbo dbo);

    @Delete("DELETE FROM plant_species_common_name WHERE species_id = (SELECT id FROM plant_species WHERE name = #{name})")
    void deleteCommonNames(String name);

    @Delete("DELETE FROM plant_species_native_bioregion WHERE species_id = (SELECT id FROM plant_species WHERE name = #{name})")
    void deleteNativeBioregions(String name);
}
