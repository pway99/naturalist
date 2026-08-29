package com.naturalist.soil.observation;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Collection;
import java.util.List;

/**
 * MyBatis mapper for the flat {@code reported_recommendation} table. Both uuid references travel as
 * text with a {@code ::uuid} cast; the {@code lab_analysis_id} soft reference is a plain column (no
 * FK). The flattened {@code amount_kind}/{@code amount_value} columns and the nullable {@code route}
 * are plain scalars.
 */
@Mapper
interface ReportedRecommendationMapper {

    String COLUMNS = "id, input_name, lab_analysis_id, amount_kind, amount_value, unit, route";

    @Select("SELECT " + COLUMNS + " FROM reported_recommendation WHERE id = #{id}::uuid")
    ReportedRecommendationDbo selectById(String id);

    @Select("""
        <script>
        SELECT id, input_name, lab_analysis_id, amount_kind, amount_value, unit, route
        FROM reported_recommendation WHERE id IN
        <foreach item='x' collection='ids' open='(' separator=',' close=')'>#{x}::uuid</foreach>
        </script>
        """)
    List<ReportedRecommendationDbo> selectByIdSet(@Param("ids") Collection<String> ids);

    @Select("SELECT " + COLUMNS + " FROM reported_recommendation ORDER BY id LIMIT #{limit} OFFSET #{offset}")
    List<ReportedRecommendationDbo> selectPage(@Param("limit") int limit, @Param("offset") int offset);

    @Select("""
        SELECT count(*) FROM (
            SELECT 1 FROM reported_recommendation ORDER BY id OFFSET #{skip} LIMIT #{window}
        ) t
        """)
    int countInWindow(@Param("skip") int skip, @Param("window") int window);

    @Select("SELECT " + COLUMNS + " FROM reported_recommendation WHERE lab_analysis_id = #{id}::uuid ORDER BY id")
    List<ReportedRecommendationDbo> selectByLabAnalysisId(String id);

    @Select("""
        <script>
        SELECT id, input_name, lab_analysis_id, amount_kind, amount_value, unit, route
        FROM reported_recommendation WHERE lab_analysis_id IN
        <foreach item='x' collection='ids' open='(' separator=',' close=')'>#{x}::uuid</foreach>
        ORDER BY id
        </script>
        """)
    List<ReportedRecommendationDbo> selectByLabAnalysisIds(@Param("ids") Collection<String> ids);

    @Select("SELECT " + COLUMNS + " FROM reported_recommendation WHERE input_name = #{inputName} ORDER BY id")
    List<ReportedRecommendationDbo> selectByInputName(String inputName);

    @Insert("""
        INSERT INTO reported_recommendation (id, input_name, lab_analysis_id, amount_kind, amount_value, unit, route)
        VALUES (#{id}::uuid, #{inputName}, #{labAnalysisId}::uuid, #{amountKind}, #{amountValue}, #{unit}, #{route})
        """)
    void insert(ReportedRecommendationDbo dbo);

    @Update("""
        UPDATE reported_recommendation SET
            input_name = #{inputName}, lab_analysis_id = #{labAnalysisId}::uuid,
            amount_kind = #{amountKind}, amount_value = #{amountValue}, unit = #{unit}, route = #{route}
        WHERE id = #{id}::uuid
        """)
    int updateById(ReportedRecommendationDbo dbo);
}
