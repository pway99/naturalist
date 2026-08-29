package com.naturalist.soil.observation;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Collection;
import java.util.List;

/**
 * MyBatis mapper for the flat {@code nutrient_reading} table. Both uuid references travel as text
 * with a {@code ::uuid} cast; the {@code lab_analysis_id} soft reference is a plain column (no FK,
 * no JOIN). {@code getByLabAnalysisIds} batches every analysis's readings into one {@code IN} query.
 */
@Mapper
interface NutrientReadingMapper {

    String COLUMNS = "id, nutrient_name, lab_analysis_id, value, unit";

    @Select("SELECT " + COLUMNS + " FROM nutrient_reading WHERE id = #{id}::uuid")
    NutrientReadingDbo selectById(String id);

    @Select("""
        <script>
        SELECT id, nutrient_name, lab_analysis_id, value, unit
        FROM nutrient_reading WHERE id IN
        <foreach item='x' collection='ids' open='(' separator=',' close=')'>#{x}::uuid</foreach>
        </script>
        """)
    List<NutrientReadingDbo> selectByIdSet(@Param("ids") Collection<String> ids);

    @Select("SELECT " + COLUMNS + " FROM nutrient_reading ORDER BY id LIMIT #{limit} OFFSET #{offset}")
    List<NutrientReadingDbo> selectPage(@Param("limit") int limit, @Param("offset") int offset);

    @Select("""
        SELECT count(*) FROM (
            SELECT 1 FROM nutrient_reading ORDER BY id OFFSET #{skip} LIMIT #{window}
        ) t
        """)
    int countInWindow(@Param("skip") int skip, @Param("window") int window);

    @Select("SELECT " + COLUMNS + " FROM nutrient_reading WHERE lab_analysis_id = #{id}::uuid ORDER BY id")
    List<NutrientReadingDbo> selectByLabAnalysisId(String id);

    @Select("""
        <script>
        SELECT id, nutrient_name, lab_analysis_id, value, unit
        FROM nutrient_reading WHERE lab_analysis_id IN
        <foreach item='x' collection='ids' open='(' separator=',' close=')'>#{x}::uuid</foreach>
        ORDER BY id
        </script>
        """)
    List<NutrientReadingDbo> selectByLabAnalysisIds(@Param("ids") Collection<String> ids);

    @Select("SELECT " + COLUMNS + " FROM nutrient_reading WHERE nutrient_name = #{nutrientName} ORDER BY id")
    List<NutrientReadingDbo> selectByNutrientName(String nutrientName);

    @Insert("""
        INSERT INTO nutrient_reading (id, nutrient_name, lab_analysis_id, value, unit)
        VALUES (#{id}::uuid, #{nutrientName}, #{labAnalysisId}::uuid, #{value}, #{unit})
        """)
    void insert(NutrientReadingDbo dbo);

    @Update("""
        UPDATE nutrient_reading SET
            nutrient_name = #{nutrientName}, lab_analysis_id = #{labAnalysisId}::uuid,
            value = #{value}, unit = #{unit}
        WHERE id = #{id}::uuid
        """)
    int updateById(NutrientReadingDbo dbo);
}
