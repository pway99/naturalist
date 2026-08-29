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
 * MyBatis mapper for the {@code plant_genus} table and its {@code plant_genus_common_name} child.
 * Reads JOIN {@code plant_family} to project the parent family's {@code name} as {@code family_name}
 * (distinct from the locally-carried {@code taxonomic_family} epithet); the common-name child is
 * loaded in one batched query keyed on the genus-name set. Writes nested-select {@code family_id}
 * from the family name, so a missing parent inserts zero rows.
 */
@Mapper
interface PlantGenusMapper {

    String COLUMNS = """
        g.name, f.name AS family_name, g.taxonomic_family, g.taxonomic_genus,
        g.description_preschool, g.description_elementary, g.description_secondary, g.description_university
        """;

    String FROM = " FROM plant_genus g JOIN plant_family f ON f.id = g.family_id ";

    // ── parent reads ─────────────────────────────────────────────────────────
    @Select("SELECT " + COLUMNS + FROM + " WHERE g.name = #{name}")
    PlantGenusDbo selectByName(String name);

    @Select("""
        <script>
        SELECT g.name, f.name AS family_name, g.taxonomic_family, g.taxonomic_genus,
               g.description_preschool, g.description_elementary, g.description_secondary, g.description_university
        FROM plant_genus g JOIN plant_family f ON f.id = g.family_id
        WHERE g.name IN
        <foreach item='n' collection='names' open='(' separator=',' close=')'>#{n}</foreach>
        </script>
        """)
    List<PlantGenusDbo> selectByNameSet(@Param("names") Collection<String> names);

    @Select("SELECT " + COLUMNS + FROM + " WHERE f.name = #{familyName} ORDER BY g.name")
    List<PlantGenusDbo> selectByFamilyName(String familyName);

    @Select("SELECT " + COLUMNS + FROM + " ORDER BY g.name LIMIT #{limit} OFFSET #{offset}")
    List<PlantGenusDbo> selectPage(@Param("limit") int limit, @Param("offset") int offset);

    @Select("""
        SELECT count(*) FROM (
            SELECT 1 FROM plant_genus ORDER BY name OFFSET #{skip} LIMIT #{window}
        ) t
        """)
    int countInWindow(@Param("skip") int skip, @Param("window") int window);

    // ── child reads (one batched query; project plant_genus.name for grouping) ──
    @Select("""
        <script>
        SELECT g.name AS genus_name, cn.label, cn.locale
        FROM plant_genus_common_name cn JOIN plant_genus g ON g.id = cn.genus_id
        WHERE g.name IN
        <foreach item='n' collection='names' open='(' separator=',' close=')'>#{n}</foreach>
        </script>
        """)
    List<PlantGenusCommonNameDbo> selectCommonNames(@Param("names") Collection<String> names);

    // ── parent writes (nested-select the family_id from the family name) ─────
    @Insert("""
        INSERT INTO plant_genus (name, family_id, taxonomic_family, taxonomic_genus,
            description_preschool, description_elementary, description_secondary, description_university)
        SELECT #{name}, id, #{taxonomicFamily}, #{taxonomicGenus},
            #{descriptionPreschool}, #{descriptionElementary}, #{descriptionSecondary}, #{descriptionUniversity}
        FROM plant_family WHERE name = #{familyName}
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(PlantGenusDbo dbo);

    @Update("""
        UPDATE plant_genus SET
            family_id = (SELECT id FROM plant_family WHERE name = #{familyName}),
            taxonomic_family = #{taxonomicFamily},
            taxonomic_genus = #{taxonomicGenus},
            description_preschool = #{descriptionPreschool},
            description_elementary = #{descriptionElementary},
            description_secondary = #{descriptionSecondary},
            description_university = #{descriptionUniversity}
        WHERE name = #{name}
        """)
    int updateByName(PlantGenusDbo dbo);

    // ── child writes (nested-select the genus_id from the name) ──────────────
    @Insert("""
        INSERT INTO plant_genus_common_name (genus_id, label, locale)
        SELECT id, #{label}, #{locale} FROM plant_genus WHERE name = #{genusName}
        """)
    int insertCommonName(PlantGenusCommonNameDbo dbo);

    @Delete("DELETE FROM plant_genus_common_name WHERE genus_id = (SELECT id FROM plant_genus WHERE name = #{name})")
    void deleteCommonNames(String name);
}
