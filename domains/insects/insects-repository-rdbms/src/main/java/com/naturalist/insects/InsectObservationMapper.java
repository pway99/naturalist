package com.naturalist.insects;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Collection;
import java.util.List;

/**
 * MyBatis mapper for {@code insect_observation} + its {@code insect_observation_candidate} child. All
 * references stored flat; uuid {@code id} travels as text with a {@code ::uuid} cast. Candidates load in
 * one batched query keyed on the observation-id set.
 */
@Mapper
interface InsectObservationMapper {

    String COLUMNS = """
        id, observed_by, subject_rank, subject, observed_on, notes, location,
        identification_confidence, identification_evidence
        """;

    @Select("SELECT " + COLUMNS + " FROM insect_observation WHERE id = #{id}::uuid")
    InsectObservationDbo selectById(String id);

    @Select("""
        <script>
        SELECT id, observed_by, subject_rank, subject, observed_on, notes, location,
               identification_confidence, identification_evidence
        FROM insect_observation WHERE id IN
        <foreach item='x' collection='ids' open='(' separator=',' close=')'>#{x}::uuid</foreach>
        </script>
        """)
    List<InsectObservationDbo> selectByIdSet(@Param("ids") Collection<String> ids);

    @Select("SELECT " + COLUMNS + " FROM insect_observation ORDER BY id LIMIT #{limit} OFFSET #{offset}")
    List<InsectObservationDbo> selectPage(@Param("limit") int limit, @Param("offset") int offset);

    @Select("""
        SELECT count(*) FROM (
            SELECT 1 FROM insect_observation ORDER BY id OFFSET #{skip} LIMIT #{window}
        ) t
        """)
    int countInWindow(@Param("skip") int skip, @Param("window") int window);

    @Select("SELECT " + COLUMNS + " FROM insect_observation WHERE observed_by = #{observedBy} ORDER BY id")
    List<InsectObservationDbo> selectByNaturalist(String observedBy);

    @Select("""
        <script>
        SELECT id, observed_by, subject_rank, subject, observed_on, notes, location,
               identification_confidence, identification_evidence
        FROM insect_observation
        WHERE observed_by = #{observedBy} AND (subject_rank, subject) IN
        <foreach item='k' collection='keys' open='(' separator=',' close=')'>(#{k.rank}, #{k.name})</foreach>
        ORDER BY id
        </script>
        """)
    List<InsectObservationDbo> selectByNaturalistAndSubjects(
            @Param("observedBy") String observedBy, @Param("keys") Collection<InsectRankKey> keys);

    @Select("""
        <script>
        SELECT observation_id, ordinal, scientific_name, common_name, confidence
        FROM insect_observation_candidate WHERE observation_id IN
        <foreach item='x' collection='ids' open='(' separator=',' close=')'>#{x}::uuid</foreach>
        ORDER BY observation_id, ordinal
        </script>
        """)
    List<InsectObservationCandidateDbo> selectCandidates(@Param("ids") Collection<String> ids);

    @Insert("""
        INSERT INTO insect_observation (id, observed_by, subject_rank, subject, observed_on, notes, location,
            identification_confidence, identification_evidence)
        VALUES (#{id}::uuid, #{observedBy}, #{subjectRank}, #{subject}, #{observedOn}, #{notes}, #{location},
            #{identificationConfidence}, #{identificationEvidence})
        """)
    void insert(InsectObservationDbo dbo);

    @Update("""
        UPDATE insect_observation SET
            observed_by = #{observedBy}, subject_rank = #{subjectRank}, subject = #{subject},
            observed_on = #{observedOn}, notes = #{notes}, location = #{location},
            identification_confidence = #{identificationConfidence}, identification_evidence = #{identificationEvidence}
        WHERE id = #{id}::uuid
        """)
    int updateById(InsectObservationDbo dbo);

    @Insert("""
        INSERT INTO insect_observation_candidate (observation_id, ordinal, scientific_name, common_name, confidence)
        VALUES (#{observationId}::uuid, #{ordinal}, #{scientificName}, #{commonName}, #{confidence})
        """)
    void insertCandidate(InsectObservationCandidateDbo dbo);

    @Delete("DELETE FROM insect_observation_candidate WHERE observation_id = #{id}::uuid")
    void deleteCandidates(String id);
}
