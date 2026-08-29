package com.naturalist.soil.observation;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Collection;
import java.util.List;

/**
 * MyBatis mapper for the flat {@code soil_physical_characteristics} table. Snake_case columns
 * auto-map to the DBO's camelCase fields. Both uuid references travel as text with a {@code ::uuid}
 * cast; the {@code lab_analysis_id} soft reference is a plain column (no FK), UNIQUE per analysis,
 * so {@code selectByLabAnalysisId} returns at most one row.
 */
@Mapper
interface SoilPhysicalCharacteristicsMapper {

    String COLUMNS = """
        id, lab_analysis_id, cec_meq_per_100g, ph, ec_ds_per_meter, sar, limestone_pct, saturation_pct,
        cbs_calcium_pct, cbs_magnesium_pct, cbs_potassium_pct, cbs_sodium_pct, cbs_hydrogen_pct,
        cbs_hydrogen_below_detection_limit
        """;

    @Select("SELECT " + COLUMNS + " FROM soil_physical_characteristics WHERE id = #{id}::uuid")
    SoilPhysicalCharacteristicsDbo selectById(String id);

    @Select("""
        <script>
        SELECT id, lab_analysis_id, cec_meq_per_100g, ph, ec_ds_per_meter, sar, limestone_pct, saturation_pct,
               cbs_calcium_pct, cbs_magnesium_pct, cbs_potassium_pct, cbs_sodium_pct, cbs_hydrogen_pct,
               cbs_hydrogen_below_detection_limit
        FROM soil_physical_characteristics WHERE id IN
        <foreach item='x' collection='ids' open='(' separator=',' close=')'>#{x}::uuid</foreach>
        </script>
        """)
    List<SoilPhysicalCharacteristicsDbo> selectByIdSet(@Param("ids") Collection<String> ids);

    @Select("SELECT " + COLUMNS + " FROM soil_physical_characteristics ORDER BY id LIMIT #{limit} OFFSET #{offset}")
    List<SoilPhysicalCharacteristicsDbo> selectPage(@Param("limit") int limit, @Param("offset") int offset);

    @Select("""
        SELECT count(*) FROM (
            SELECT 1 FROM soil_physical_characteristics ORDER BY id OFFSET #{skip} LIMIT #{window}
        ) t
        """)
    int countInWindow(@Param("skip") int skip, @Param("window") int window);

    @Select("SELECT " + COLUMNS + " FROM soil_physical_characteristics WHERE lab_analysis_id = #{id}::uuid")
    SoilPhysicalCharacteristicsDbo selectByLabAnalysisId(String id);

    @Select("""
        <script>
        SELECT id, lab_analysis_id, cec_meq_per_100g, ph, ec_ds_per_meter, sar, limestone_pct, saturation_pct,
               cbs_calcium_pct, cbs_magnesium_pct, cbs_potassium_pct, cbs_sodium_pct, cbs_hydrogen_pct,
               cbs_hydrogen_below_detection_limit
        FROM soil_physical_characteristics WHERE lab_analysis_id IN
        <foreach item='x' collection='ids' open='(' separator=',' close=')'>#{x}::uuid</foreach>
        ORDER BY id
        </script>
        """)
    List<SoilPhysicalCharacteristicsDbo> selectByLabAnalysisIds(@Param("ids") Collection<String> ids);

    @Insert("""
        INSERT INTO soil_physical_characteristics (id, lab_analysis_id, cec_meq_per_100g, ph, ec_ds_per_meter, sar,
            limestone_pct, saturation_pct,
            cbs_calcium_pct, cbs_magnesium_pct, cbs_potassium_pct, cbs_sodium_pct, cbs_hydrogen_pct,
            cbs_hydrogen_below_detection_limit)
        VALUES (#{id}::uuid, #{labAnalysisId}::uuid, #{cecMeqPer100g}, #{ph}, #{ecDsPerMeter}, #{sar},
            #{limestonePct}, #{saturationPct},
            #{cbsCalciumPct}, #{cbsMagnesiumPct}, #{cbsPotassiumPct}, #{cbsSodiumPct}, #{cbsHydrogenPct},
            #{cbsHydrogenBelowDetectionLimit})
        """)
    void insert(SoilPhysicalCharacteristicsDbo dbo);

    @Update("""
        UPDATE soil_physical_characteristics SET
            lab_analysis_id = #{labAnalysisId}::uuid, cec_meq_per_100g = #{cecMeqPer100g}, ph = #{ph},
            ec_ds_per_meter = #{ecDsPerMeter}, sar = #{sar}, limestone_pct = #{limestonePct},
            saturation_pct = #{saturationPct},
            cbs_calcium_pct = #{cbsCalciumPct}, cbs_magnesium_pct = #{cbsMagnesiumPct},
            cbs_potassium_pct = #{cbsPotassiumPct}, cbs_sodium_pct = #{cbsSodiumPct},
            cbs_hydrogen_pct = #{cbsHydrogenPct},
            cbs_hydrogen_below_detection_limit = #{cbsHydrogenBelowDetectionLimit}
        WHERE id = #{id}::uuid
        """)
    int updateById(SoilPhysicalCharacteristicsDbo dbo);
}
