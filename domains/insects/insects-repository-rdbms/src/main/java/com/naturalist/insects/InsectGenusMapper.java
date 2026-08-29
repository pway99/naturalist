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
 * MyBatis mapper for the {@code insect_genus} table and its {@code insect_genus_common_name} child.
 * Reads JOIN {@code insect_family} to project the parent family's {@code name} as {@code family_name}
 * (so {@code family_id} never surfaces); the genus carries only its own {@code taxonomic_genus}
 * epithet. The common-name child is loaded in one batched query keyed on the genus-name set. Writes
 * nested-select {@code family_id} from the family name, so a missing parent inserts zero rows.
 */
@Mapper
interface InsectGenusMapper {

    String COLUMNS = """
        g.name, f.name AS family_name, g.taxonomic_genus, g.placed_in,
        g.description_preschool, g.description_elementary, g.description_secondary, g.description_university
        """;

    String FROM = " FROM insect_genus g JOIN insect_family f ON f.id = g.family_id ";

    // ── parent reads ─────────────────────────────────────────────────────────
    @Select("SELECT " + COLUMNS + FROM + " WHERE g.name = #{name}")
    InsectGenusDbo selectByName(String name);

    @Select("""
        <script>
        SELECT g.name, f.name AS family_name, g.taxonomic_genus, g.placed_in,
               g.description_preschool, g.description_elementary, g.description_secondary, g.description_university
        FROM insect_genus g JOIN insect_family f ON f.id = g.family_id
        WHERE g.name IN
        <foreach item='n' collection='names' open='(' separator=',' close=')'>#{n}</foreach>
        </script>
        """)
    List<InsectGenusDbo> selectByNameSet(@Param("names") Collection<String> names);

    @Select("SELECT " + COLUMNS + FROM + " WHERE f.name = #{familyName} ORDER BY g.name")
    List<InsectGenusDbo> selectByFamilyName(String familyName);

    @Select("""
        <script>
        SELECT g.name, f.name AS family_name, g.taxonomic_genus, g.placed_in,
               g.description_preschool, g.description_elementary, g.description_secondary, g.description_university
        FROM insect_genus g JOIN insect_family f ON f.id = g.family_id
        WHERE f.name IN
        <foreach item='n' collection='names' open='(' separator=',' close=')'>#{n}</foreach>
        ORDER BY g.name
        </script>
        """)
    List<InsectGenusDbo> selectByFamilyNameSet(@Param("names") Collection<String> names);

    @Select("SELECT " + COLUMNS + FROM + " ORDER BY g.name LIMIT #{limit} OFFSET #{offset}")
    List<InsectGenusDbo> selectPage(@Param("limit") int limit, @Param("offset") int offset);

    @Select("""
        SELECT count(*) FROM (
            SELECT 1 FROM insect_genus ORDER BY name OFFSET #{skip} LIMIT #{window}
        ) t
        """)
    int countInWindow(@Param("skip") int skip, @Param("window") int window);

    // ── child reads (one batched query; project insect_genus.name for grouping) ──
    @Select("""
        <script>
        SELECT g.name AS genus_name, cn.label, cn.locale
        FROM insect_genus_common_name cn JOIN insect_genus g ON g.id = cn.genus_id
        WHERE g.name IN
        <foreach item='n' collection='names' open='(' separator=',' close=')'>#{n}</foreach>
        </script>
        """)
    List<InsectGenusCommonNameDbo> selectCommonNames(@Param("names") Collection<String> names);

    // ── parent writes (nested-select the family_id from the family name) ─────
    @Insert("""
        INSERT INTO insect_genus (name, family_id, taxonomic_genus, placed_in,
            description_preschool, description_elementary, description_secondary, description_university)
        SELECT #{name}, id, #{taxonomicGenus}, #{placedIn},
            #{descriptionPreschool}, #{descriptionElementary}, #{descriptionSecondary}, #{descriptionUniversity}
        FROM insect_family WHERE name = #{familyName}
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(InsectGenusDbo dbo);

    @Update("""
        UPDATE insect_genus SET
            family_id = (SELECT id FROM insect_family WHERE name = #{familyName}),
            taxonomic_genus = #{taxonomicGenus},
            placed_in = #{placedIn},
            description_preschool = #{descriptionPreschool},
            description_elementary = #{descriptionElementary},
            description_secondary = #{descriptionSecondary},
            description_university = #{descriptionUniversity}
        WHERE name = #{name}
        """)
    int updateByName(InsectGenusDbo dbo);

    // ── child writes (nested-select the genus_id from the name) ──────────────
    @Insert("""
        INSERT INTO insect_genus_common_name (genus_id, label, locale)
        SELECT id, #{label}, #{locale} FROM insect_genus WHERE name = #{genusName}
        """)
    int insertCommonName(InsectGenusCommonNameDbo dbo);

    @Delete("DELETE FROM insect_genus_common_name WHERE genus_id = (SELECT id FROM insect_genus WHERE name = #{name})")
    void deleteCommonNames(String name);
}
