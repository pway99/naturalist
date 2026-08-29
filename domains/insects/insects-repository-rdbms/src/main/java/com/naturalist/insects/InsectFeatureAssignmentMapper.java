package com.naturalist.insects;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Collection;
import java.util.List;

/**
 * MyBatis mapper for {@code insect_feature_assignment} — a surrogate-UUID entity. Both the
 * {@code id} and the within-insects {@code feature_id} FK travel as text and are cast in SQL
 * ({@code ::uuid}); no JOIN is needed because the feature reference is a direct id. A set of ranks
 * is resolved in one query by {@link #selectByRankKeys}, a row-value {@code IN} over the
 * {@code (rank, rank_name)} pair, parameterised with the module's shared {@link InsectRankKey}.
 */
@Mapper
interface InsectFeatureAssignmentMapper {

    String COLUMNS = "id, feature_id, rank, rank_name, ordinal";

    @Select("SELECT " + COLUMNS + " FROM insect_feature_assignment WHERE id = #{id}::uuid")
    InsectFeatureAssignmentDbo selectById(String id);

    @Select("""
        <script>
        SELECT id, feature_id, rank, rank_name, ordinal FROM insect_feature_assignment WHERE id IN
        <foreach item='x' collection='ids' open='(' separator=',' close=')'>#{x}::uuid</foreach>
        </script>
        """)
    List<InsectFeatureAssignmentDbo> selectByIdSet(@Param("ids") Collection<String> ids);

    @Select("SELECT " + COLUMNS + " FROM insect_feature_assignment ORDER BY id LIMIT #{limit} OFFSET #{offset}")
    List<InsectFeatureAssignmentDbo> selectPage(@Param("limit") int limit, @Param("offset") int offset);

    @Select("""
        SELECT count(*) FROM (
            SELECT 1 FROM insect_feature_assignment ORDER BY id OFFSET #{skip} LIMIT #{window}
        ) t
        """)
    int countInWindow(@Param("skip") int skip, @Param("window") int window);

    @Select("""
        SELECT id, feature_id, rank, rank_name, ordinal FROM insect_feature_assignment
        WHERE rank = #{rank} AND rank_name = #{rankName} ORDER BY ordinal
        """)
    List<InsectFeatureAssignmentDbo> selectByRank(@Param("rank") String rank, @Param("rankName") String rankName);

    @Select("""
        <script>
        SELECT id, feature_id, rank, rank_name, ordinal FROM insect_feature_assignment
        WHERE (rank, rank_name) IN
        <foreach item='k' collection='keys' open='(' separator=',' close=')'>
            (#{k.rank}, #{k.name})
        </foreach>
        ORDER BY ordinal
        </script>
        """)
    List<InsectFeatureAssignmentDbo> selectByRankKeys(@Param("keys") Collection<InsectRankKey> keys);

    @Select("""
        SELECT id, feature_id, rank, rank_name, ordinal FROM insect_feature_assignment
        WHERE feature_id = #{featureId}::uuid ORDER BY ordinal
        """)
    List<InsectFeatureAssignmentDbo> selectByFeatureId(String featureId);

    @Insert("""
        INSERT INTO insect_feature_assignment (id, feature_id, rank, rank_name, ordinal)
        VALUES (#{id}::uuid, #{featureId}::uuid, #{rank}, #{rankName}, #{ordinal})
        """)
    int insert(InsectFeatureAssignmentDbo dbo);

    @Update("""
        UPDATE insect_feature_assignment SET
            feature_id = #{featureId}::uuid, rank = #{rank}, rank_name = #{rankName}, ordinal = #{ordinal}
        WHERE id = #{id}::uuid
        """)
    int updateById(InsectFeatureAssignmentDbo dbo);
}
