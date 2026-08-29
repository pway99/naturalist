package com.naturalist.insects;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Collection;
import java.util.List;

/**
 * MyBatis mapper for {@code insect_feature} — a surrogate-UUID entity. The uuid {@code id} travels
 * as text and is cast in SQL ({@code ::uuid}); there is no type handler. {@code value} is a plain
 * column carrying a single-column {@code UNIQUE} constraint.
 */
@Mapper
interface InsectFeatureMapper {

    @Select("SELECT id, value FROM insect_feature WHERE id = #{id}::uuid")
    InsectFeatureDbo selectById(String id);

    @Select("""
        <script>
        SELECT id, value FROM insect_feature WHERE id IN
        <foreach item='x' collection='ids' open='(' separator=',' close=')'>#{x}::uuid</foreach>
        </script>
        """)
    List<InsectFeatureDbo> selectByIdSet(@Param("ids") Collection<String> ids);

    @Select("SELECT id, value FROM insect_feature ORDER BY id LIMIT #{limit} OFFSET #{offset}")
    List<InsectFeatureDbo> selectPage(@Param("limit") int limit, @Param("offset") int offset);

    @Select("""
        SELECT count(*) FROM (
            SELECT 1 FROM insect_feature ORDER BY id OFFSET #{skip} LIMIT #{window}
        ) t
        """)
    int countInWindow(@Param("skip") int skip, @Param("window") int window);

    @Insert("INSERT INTO insect_feature (id, value) VALUES (#{id}::uuid, #{value})")
    int insert(InsectFeatureDbo dbo);

    @Update("UPDATE insect_feature SET value = #{value} WHERE id = #{id}::uuid")
    int updateById(InsectFeatureDbo dbo);
}
