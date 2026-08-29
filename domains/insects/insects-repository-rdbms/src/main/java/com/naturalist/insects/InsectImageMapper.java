package com.naturalist.insects;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Collection;
import java.util.List;

/**
 * MyBatis mapper for {@code insect_image}. References stored flat (rank+slug parent, direct observation
 * uuid); uuid columns travel as text with a {@code ::uuid} cast. {@code getByParentNames} batches over a
 * row-value {@code IN} on (parent_rank, parent_name).
 */
@Mapper
interface InsectImageMapper {

    String COLUMNS = "id, parent_rank, parent_name, date_added, resource_name, observation_id";

    @Select("SELECT " + COLUMNS + " FROM insect_image WHERE id = #{id}::uuid")
    InsectImageDbo selectById(String id);

    @Select("""
        <script>
        SELECT id, parent_rank, parent_name, date_added, resource_name, observation_id
        FROM insect_image WHERE id IN
        <foreach item='x' collection='ids' open='(' separator=',' close=')'>#{x}::uuid</foreach>
        </script>
        """)
    List<InsectImageDbo> selectByIdSet(@Param("ids") Collection<String> ids);

    @Select("SELECT " + COLUMNS + " FROM insect_image ORDER BY id LIMIT #{limit} OFFSET #{offset}")
    List<InsectImageDbo> selectPage(@Param("limit") int limit, @Param("offset") int offset);

    @Select("""
        SELECT count(*) FROM (
            SELECT 1 FROM insect_image ORDER BY id OFFSET #{skip} LIMIT #{window}
        ) t
        """)
    int countInWindow(@Param("skip") int skip, @Param("window") int window);

    @Select("""
        SELECT id, parent_rank, parent_name, date_added, resource_name, observation_id
        FROM insect_image WHERE parent_rank = #{rank} AND parent_name = #{name} ORDER BY id
        """)
    List<InsectImageDbo> selectByParentName(@Param("rank") String rank, @Param("name") String name);

    @Select("""
        <script>
        SELECT id, parent_rank, parent_name, date_added, resource_name, observation_id
        FROM insect_image WHERE (parent_rank, parent_name) IN
        <foreach item='k' collection='keys' open='(' separator=',' close=')'>(#{k.rank}, #{k.name})</foreach>
        ORDER BY id
        </script>
        """)
    List<InsectImageDbo> selectByParentNames(@Param("keys") Collection<InsectRankKey> keys);

    @Insert("""
        INSERT INTO insect_image (id, parent_rank, parent_name, date_added, resource_name, observation_id)
        VALUES (#{id}::uuid, #{parentRank}, #{parentName}, #{dateAdded}, #{resourceName}, #{observationId}::uuid)
        """)
    void insert(InsectImageDbo dbo);

    @Update("""
        UPDATE insect_image SET
            parent_rank = #{parentRank}, parent_name = #{parentName}, date_added = #{dateAdded},
            resource_name = #{resourceName}, observation_id = #{observationId}::uuid
        WHERE id = #{id}::uuid
        """)
    int updateById(InsectImageDbo dbo);
}
