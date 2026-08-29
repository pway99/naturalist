package com.naturalist.soil.observation;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Collection;
import java.util.List;

/**
 * MyBatis mapper for the flat {@code reported_optimum} table. Both uuid references travel as text
 * with a {@code ::uuid} cast; the {@code lab_analysis_id} soft reference is a plain column (no FK).
 * The flattened {@code range_shape}/{@code range_min}/{@code range_max} columns are plain scalars.
 */
@Mapper
interface ReportedOptimumMapper {

    String COLUMNS = "id, nutrient_name, lab_analysis_id, range_shape, range_min, range_max, unit";

    @Select("SELECT " + COLUMNS + " FROM reported_optimum WHERE id = #{id}::uuid")
    ReportedOptimumDbo selectById(String id);

    @Select("""
        <script>
        SELECT id, nutrient_name, lab_analysis_id, range_shape, range_min, range_max, unit
        FROM reported_optimum WHERE id IN
        <foreach item='x' collection='ids' open='(' separator=',' close=')'>#{x}::uuid</foreach>
        </script>
        """)
    List<ReportedOptimumDbo> selectByIdSet(@Param("ids") Collection<String> ids);

    @Select("SELECT " + COLUMNS + " FROM reported_optimum ORDER BY id LIMIT #{limit} OFFSET #{offset}")
    List<ReportedOptimumDbo> selectPage(@Param("limit") int limit, @Param("offset") int offset);

    @Select("""
        SELECT count(*) FROM (
            SELECT 1 FROM reported_optimum ORDER BY id OFFSET #{skip} LIMIT #{window}
        ) t
        """)
    int countInWindow(@Param("skip") int skip, @Param("window") int window);

    @Select("SELECT " + COLUMNS + " FROM reported_optimum WHERE lab_analysis_id = #{id}::uuid ORDER BY id")
    List<ReportedOptimumDbo> selectByLabAnalysisId(String id);

    @Select("""
        <script>
        SELECT id, nutrient_name, lab_analysis_id, range_shape, range_min, range_max, unit
        FROM reported_optimum WHERE lab_analysis_id IN
        <foreach item='x' collection='ids' open='(' separator=',' close=')'>#{x}::uuid</foreach>
        ORDER BY id
        </script>
        """)
    List<ReportedOptimumDbo> selectByLabAnalysisIds(@Param("ids") Collection<String> ids);

    @Select("SELECT " + COLUMNS + " FROM reported_optimum WHERE nutrient_name = #{nutrientName} ORDER BY id")
    List<ReportedOptimumDbo> selectByNutrientName(String nutrientName);

    @Insert("""
        INSERT INTO reported_optimum (id, nutrient_name, lab_analysis_id, range_shape, range_min, range_max, unit)
        VALUES (#{id}::uuid, #{nutrientName}, #{labAnalysisId}::uuid, #{rangeShape}, #{rangeMin}, #{rangeMax}, #{unit})
        """)
    void insert(ReportedOptimumDbo dbo);

    @Update("""
        UPDATE reported_optimum SET
            nutrient_name = #{nutrientName}, lab_analysis_id = #{labAnalysisId}::uuid,
            range_shape = #{rangeShape}, range_min = #{rangeMin}, range_max = #{rangeMax}, unit = #{unit}
        WHERE id = #{id}::uuid
        """)
    int updateById(ReportedOptimumDbo dbo);
}
