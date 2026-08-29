package com.naturalist.plants;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Collection;
import java.util.List;

/**
 * MyBatis mapper for {@code plant_image}. All references are stored flat (rank+slug parent, direct
 * observation uuid), so reads need no JOIN; uuid columns travel as text with a {@code ::uuid} cast.
 */
@Mapper
interface PlantImageMapper {

    String COLUMNS = "id, parent_rank, parent_name, date_added, resource_name, observation_id";

    @Select("SELECT " + COLUMNS + " FROM plant_image WHERE id = #{id}::uuid")
    PlantImageDbo selectById(String id);

    @Select("""
        <script>
        SELECT id, parent_rank, parent_name, date_added, resource_name, observation_id
        FROM plant_image WHERE id IN
        <foreach item='x' collection='ids' open='(' separator=',' close=')'>#{x}::uuid</foreach>
        </script>
        """)
    List<PlantImageDbo> selectByIdSet(@Param("ids") Collection<String> ids);

    @Select("SELECT " + COLUMNS + " FROM plant_image ORDER BY id LIMIT #{limit} OFFSET #{offset}")
    List<PlantImageDbo> selectPage(@Param("limit") int limit, @Param("offset") int offset);

    @Select("""
        SELECT count(*) FROM (
            SELECT 1 FROM plant_image ORDER BY id OFFSET #{skip} LIMIT #{window}
        ) t
        """)
    int countInWindow(@Param("skip") int skip, @Param("window") int window);

    @Select("""
        SELECT id, parent_rank, parent_name, date_added, resource_name, observation_id
        FROM plant_image WHERE parent_rank = #{rank} AND parent_name = #{name} ORDER BY id
        """)
    List<PlantImageDbo> selectByParentName(@Param("rank") String rank, @Param("name") String name);

    @Insert("""
        INSERT INTO plant_image (id, parent_rank, parent_name, date_added, resource_name, observation_id)
        VALUES (#{id}::uuid, #{parentRank}, #{parentName}, #{dateAdded}, #{resourceName}, #{observationId}::uuid)
        """)
    void insert(PlantImageDbo dbo);

    @Update("""
        UPDATE plant_image SET
            parent_rank = #{parentRank}, parent_name = #{parentName}, date_added = #{dateAdded},
            resource_name = #{resourceName}, observation_id = #{observationId}::uuid
        WHERE id = #{id}::uuid
        """)
    int updateById(PlantImageDbo dbo);
}
