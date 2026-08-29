package com.naturalist.garden;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Collection;
import java.util.List;

/**
 * MyBatis mapper for the flat {@code planting} table. All references are stored flat (no FK, no JOIN);
 * the uuid {@code id} travels as text with a {@code ::uuid} cast. {@code getByPlantName} matches on BOTH
 * {@code plant_rank} and {@code plant_name} so a genus and a species holding the same slug never collide.
 */
@Mapper
interface PlantingMapper {

    String COLUMNS = """
        id, plant_rank, plant_name, cultivar_name, zone_name, sub_zone_name,
        plant_count, planted_date, removed_date, notes
        """;

    @Select("SELECT " + COLUMNS + " FROM planting WHERE id = #{id}::uuid")
    PlantingDbo selectById(String id);

    @Select("""
        <script>
        SELECT id, plant_rank, plant_name, cultivar_name, zone_name, sub_zone_name,
               plant_count, planted_date, removed_date, notes
        FROM planting WHERE id IN
        <foreach item='x' collection='ids' open='(' separator=',' close=')'>#{x}::uuid</foreach>
        </script>
        """)
    List<PlantingDbo> selectByIdSet(@Param("ids") Collection<String> ids);

    @Select("SELECT " + COLUMNS + " FROM planting ORDER BY id LIMIT #{limit} OFFSET #{offset}")
    List<PlantingDbo> selectPage(@Param("limit") int limit, @Param("offset") int offset);

    @Select("""
        SELECT count(*) FROM (
            SELECT 1 FROM planting ORDER BY id OFFSET #{skip} LIMIT #{window}
        ) t
        """)
    int countInWindow(@Param("skip") int skip, @Param("window") int window);

    @Select("SELECT " + COLUMNS + " FROM planting WHERE zone_name = #{zoneName} ORDER BY id")
    List<PlantingDbo> selectByZoneName(String zoneName);

    @Select("SELECT " + COLUMNS + " FROM planting WHERE sub_zone_name = #{subZoneName} ORDER BY id")
    List<PlantingDbo> selectBySubZoneName(String subZoneName);

    @Select("""
        SELECT id, plant_rank, plant_name, cultivar_name, zone_name, sub_zone_name,
               plant_count, planted_date, removed_date, notes
        FROM planting WHERE plant_rank = #{rank} AND plant_name = #{name} ORDER BY id
        """)
    List<PlantingDbo> selectByPlantName(@Param("rank") String rank, @Param("name") String name);

    @Insert("""
        INSERT INTO planting (id, plant_rank, plant_name, cultivar_name, zone_name, sub_zone_name,
            plant_count, planted_date, removed_date, notes)
        VALUES (#{id}::uuid, #{plantRank}, #{plantName}, #{cultivarName}, #{zoneName}, #{subZoneName},
            #{plantCount}, #{plantedDate}, #{removedDate}, #{notes})
        """)
    void insert(PlantingDbo dbo);

    @Update("""
        UPDATE planting SET
            plant_rank = #{plantRank}, plant_name = #{plantName}, cultivar_name = #{cultivarName},
            zone_name = #{zoneName}, sub_zone_name = #{subZoneName}, plant_count = #{plantCount},
            planted_date = #{plantedDate}, removed_date = #{removedDate}, notes = #{notes}
        WHERE id = #{id}::uuid
        """)
    int updateById(PlantingDbo dbo);
}
