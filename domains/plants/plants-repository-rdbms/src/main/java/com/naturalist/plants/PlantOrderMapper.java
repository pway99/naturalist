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
 * MyBatis mapper for the {@code plant_order} table (top of the rank chain — no upward FK) and its
 * {@code plant_order_common_name} child. Snake_case columns auto-map to camelCase fields; the
 * common-name child is loaded in one batched query keyed on the order-name set. Child inserts
 * nested-select the {@code order_id} from the order name, so no generated id is threaded back.
 */
@Mapper
interface PlantOrderMapper {

    String COLUMNS = """
        name, taxonomic_order, placed_in,
        description_preschool, description_elementary, description_secondary, description_university
        """;

    // ── parent reads ─────────────────────────────────────────────────────────
    @Select("SELECT " + COLUMNS + " FROM plant_order WHERE name = #{name}")
    PlantOrderDbo selectByName(String name);

    @Select("""
        <script>
        SELECT name, taxonomic_order, placed_in,
               description_preschool, description_elementary, description_secondary, description_university
        FROM plant_order WHERE name IN
        <foreach item='n' collection='names' open='(' separator=',' close=')'>#{n}</foreach>
        </script>
        """)
    List<PlantOrderDbo> selectByNameSet(@Param("names") Collection<String> names);

    @Select("SELECT " + COLUMNS + " FROM plant_order ORDER BY name LIMIT #{limit} OFFSET #{offset}")
    List<PlantOrderDbo> selectPage(@Param("limit") int limit, @Param("offset") int offset);

    @Select("""
        SELECT count(*) FROM (
            SELECT 1 FROM plant_order ORDER BY name OFFSET #{skip} LIMIT #{window}
        ) t
        """)
    int countInWindow(@Param("skip") int skip, @Param("window") int window);

    // ── child reads (one batched query; project plant_order.name for grouping) ──
    @Select("""
        <script>
        SELECT o.name AS order_name, cn.label, cn.locale
        FROM plant_order_common_name cn JOIN plant_order o ON o.id = cn.order_id
        WHERE o.name IN
        <foreach item='n' collection='names' open='(' separator=',' close=')'>#{n}</foreach>
        </script>
        """)
    List<PlantOrderCommonNameDbo> selectCommonNames(@Param("names") Collection<String> names);

    // ── parent writes ────────────────────────────────────────────────────────
    @Insert("""
        INSERT INTO plant_order (name, taxonomic_order, placed_in,
            description_preschool, description_elementary, description_secondary, description_university)
        VALUES (#{name}, #{taxonomicOrder}, #{placedIn},
            #{descriptionPreschool}, #{descriptionElementary}, #{descriptionSecondary}, #{descriptionUniversity})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    void insert(PlantOrderDbo dbo);

    @Update("""
        UPDATE plant_order SET taxonomic_order = #{taxonomicOrder}, placed_in = #{placedIn},
            description_preschool = #{descriptionPreschool},
            description_elementary = #{descriptionElementary},
            description_secondary = #{descriptionSecondary},
            description_university = #{descriptionUniversity}
        WHERE name = #{name}
        """)
    int updateByName(PlantOrderDbo dbo);

    // ── child writes (nested-select the order_id from the name) ──────────────
    @Insert("""
        INSERT INTO plant_order_common_name (order_id, label, locale)
        SELECT id, #{label}, #{locale} FROM plant_order WHERE name = #{orderName}
        """)
    int insertCommonName(PlantOrderCommonNameDbo dbo);

    @Delete("DELETE FROM plant_order_common_name WHERE order_id = (SELECT id FROM plant_order WHERE name = #{name})")
    void deleteCommonNames(String name);
}
