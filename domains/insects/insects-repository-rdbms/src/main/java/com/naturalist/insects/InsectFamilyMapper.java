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
 * MyBatis mapper for the {@code insect_family} table and its {@code insect_family_common_name} child.
 * Reads JOIN {@code insect_order} to project the parent order's {@code name} (so {@code order_id}
 * never surfaces); the common-name child is loaded in one batched query keyed on the family-name set.
 * Writes nested-select {@code order_id} from the order name, so a missing parent inserts zero rows
 * (the adapter turns that into {@code EntityNotFoundException}) and no generated id is threaded.
 */
@Mapper
interface InsectFamilyMapper {

    String COLUMNS = """
        f.name, o.name AS order_name, f.taxonomic_family, f.placed_in,
        f.description_preschool, f.description_elementary, f.description_secondary, f.description_university
        """;

    String FROM = " FROM insect_family f JOIN insect_order o ON o.id = f.order_id ";

    // ── parent reads ─────────────────────────────────────────────────────────
    @Select("SELECT " + COLUMNS + FROM + " WHERE f.name = #{name}")
    InsectFamilyDbo selectByName(String name);

    @Select("""
        <script>
        SELECT f.name, o.name AS order_name, f.taxonomic_family, f.placed_in,
               f.description_preschool, f.description_elementary, f.description_secondary, f.description_university
        FROM insect_family f JOIN insect_order o ON o.id = f.order_id
        WHERE f.name IN
        <foreach item='n' collection='names' open='(' separator=',' close=')'>#{n}</foreach>
        </script>
        """)
    List<InsectFamilyDbo> selectByNameSet(@Param("names") Collection<String> names);

    @Select("SELECT " + COLUMNS + FROM + " WHERE o.name = #{orderName} ORDER BY f.name")
    List<InsectFamilyDbo> selectByOrderName(String orderName);

    @Select("""
        <script>
        SELECT f.name, o.name AS order_name, f.taxonomic_family, f.placed_in,
               f.description_preschool, f.description_elementary, f.description_secondary, f.description_university
        FROM insect_family f JOIN insect_order o ON o.id = f.order_id
        WHERE o.name IN
        <foreach item='n' collection='names' open='(' separator=',' close=')'>#{n}</foreach>
        ORDER BY f.name
        </script>
        """)
    List<InsectFamilyDbo> selectByOrderNameSet(@Param("names") Collection<String> names);

    @Select("SELECT " + COLUMNS + FROM + " ORDER BY f.name LIMIT #{limit} OFFSET #{offset}")
    List<InsectFamilyDbo> selectPage(@Param("limit") int limit, @Param("offset") int offset);

    @Select("""
        SELECT count(*) FROM (
            SELECT 1 FROM insect_family ORDER BY name OFFSET #{skip} LIMIT #{window}
        ) t
        """)
    int countInWindow(@Param("skip") int skip, @Param("window") int window);

    // ── child reads (one batched query; project insect_family.name for grouping) ──
    @Select("""
        <script>
        SELECT f.name AS family_name, cn.label, cn.locale
        FROM insect_family_common_name cn JOIN insect_family f ON f.id = cn.family_id
        WHERE f.name IN
        <foreach item='n' collection='names' open='(' separator=',' close=')'>#{n}</foreach>
        </script>
        """)
    List<InsectFamilyCommonNameDbo> selectCommonNames(@Param("names") Collection<String> names);

    // ── parent writes (nested-select the order_id from the order name) ───────
    @Insert("""
        INSERT INTO insect_family (name, order_id, taxonomic_family, placed_in,
            description_preschool, description_elementary, description_secondary, description_university)
        SELECT #{name}, id, #{taxonomicFamily}, #{placedIn},
            #{descriptionPreschool}, #{descriptionElementary}, #{descriptionSecondary}, #{descriptionUniversity}
        FROM insect_order WHERE name = #{orderName}
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(InsectFamilyDbo dbo);

    @Update("""
        UPDATE insect_family SET
            order_id = (SELECT id FROM insect_order WHERE name = #{orderName}),
            taxonomic_family = #{taxonomicFamily},
            placed_in = #{placedIn},
            description_preschool = #{descriptionPreschool},
            description_elementary = #{descriptionElementary},
            description_secondary = #{descriptionSecondary},
            description_university = #{descriptionUniversity}
        WHERE name = #{name}
        """)
    int updateByName(InsectFamilyDbo dbo);

    // ── child writes (nested-select the family_id from the name) ─────────────
    @Insert("""
        INSERT INTO insect_family_common_name (family_id, label, locale)
        SELECT id, #{label}, #{locale} FROM insect_family WHERE name = #{familyName}
        """)
    int insertCommonName(InsectFamilyCommonNameDbo dbo);

    @Delete("DELETE FROM insect_family_common_name WHERE family_id = (SELECT id FROM insect_family WHERE name = #{name})")
    void deleteCommonNames(String name);
}
