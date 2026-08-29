package com.naturalist.soil.observation;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Collection;
import java.util.List;

/**
 * MyBatis mapper for the flat {@code lab_analysis_info} table. Snake_case columns auto-map to the
 * DBO's camelCase fields. The uuid {@code id} travels as text with a {@code ::uuid} cast; the
 * {@code soil_profile_name} soft reference is a plain slug column (no FK, no JOIN).
 */
@Mapper
interface LabAnalysisInfoMapper {

    String COLUMNS = """
        id, soil_profile_name, crop_type, sample_date, lab_id, lab_sample_id,
        sample_depth_inches, notes,
        sampling_subsample_count, sampling_tool, sampling_compositing_method
        """;

    @Select("SELECT " + COLUMNS + " FROM lab_analysis_info WHERE id = #{id}::uuid")
    LabAnalysisInfoDbo selectById(String id);

    @Select("""
        <script>
        SELECT id, soil_profile_name, crop_type, sample_date, lab_id, lab_sample_id,
               sample_depth_inches, notes,
               sampling_subsample_count, sampling_tool, sampling_compositing_method
        FROM lab_analysis_info WHERE id IN
        <foreach item='x' collection='ids' open='(' separator=',' close=')'>#{x}::uuid</foreach>
        </script>
        """)
    List<LabAnalysisInfoDbo> selectByIdSet(@Param("ids") Collection<String> ids);

    @Select("SELECT " + COLUMNS + " FROM lab_analysis_info ORDER BY id LIMIT #{limit} OFFSET #{offset}")
    List<LabAnalysisInfoDbo> selectPage(@Param("limit") int limit, @Param("offset") int offset);

    @Select("""
        SELECT count(*) FROM (
            SELECT 1 FROM lab_analysis_info ORDER BY id OFFSET #{skip} LIMIT #{window}
        ) t
        """)
    int countInWindow(@Param("skip") int skip, @Param("window") int window);

    @Select("SELECT " + COLUMNS + " FROM lab_analysis_info WHERE soil_profile_name = #{soilProfileName} ORDER BY id")
    List<LabAnalysisInfoDbo> selectBySoilProfileName(String soilProfileName);

    @Insert("""
        INSERT INTO lab_analysis_info (id, soil_profile_name, crop_type, sample_date, lab_id, lab_sample_id,
            sample_depth_inches, notes,
            sampling_subsample_count, sampling_tool, sampling_compositing_method)
        VALUES (#{id}::uuid, #{soilProfileName}, #{cropType}, #{sampleDate}, #{labId}, #{labSampleId},
            #{sampleDepthInches}, #{notes},
            #{samplingSubsampleCount}, #{samplingTool}, #{samplingCompositingMethod})
        """)
    void insert(LabAnalysisInfoDbo dbo);

    @Update("""
        UPDATE lab_analysis_info SET
            soil_profile_name = #{soilProfileName}, crop_type = #{cropType}, sample_date = #{sampleDate},
            lab_id = #{labId}, lab_sample_id = #{labSampleId},
            sample_depth_inches = #{sampleDepthInches}, notes = #{notes},
            sampling_subsample_count = #{samplingSubsampleCount}, sampling_tool = #{samplingTool},
            sampling_compositing_method = #{samplingCompositingMethod}
        WHERE id = #{id}::uuid
        """)
    int updateById(LabAnalysisInfoDbo dbo);
}
