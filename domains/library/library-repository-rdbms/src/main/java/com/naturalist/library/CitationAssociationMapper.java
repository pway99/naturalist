package com.naturalist.library;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Collection;
import java.util.List;

/**
 * MyBatis mapper for {@code citation_association} — a surrogate-UUID entity. Reads JOIN {@code citation}
 * to project the citation {@code name} for reconstruction; writes nested-select {@code citation.id} from
 * that name. The uuid {@code id} travels as text and is cast to uuid in SQL ({@code ::uuid}). The
 * {@code subject} slugs are plain columns (the target is cross-domain, no FK); the batched
 * {@link #selectBySubjects} matches them with a row-value {@code IN} so a set of subjects costs one query.
 */
@Mapper
interface CitationAssociationMapper {

    String SELECT_JOIN = """
        SELECT ca.id, c.name AS citation_name,
               ca.subject_domain, ca.subject_rank, ca.subject_name, ca.note
        FROM citation_association ca JOIN citation c ON c.id = ca.citation_id
        """;

    @Select(SELECT_JOIN + " WHERE ca.id = #{id}::uuid")
    CitationAssociationDbo selectById(String id);

    @Select("""
        <script>
        SELECT ca.id, c.name AS citation_name,
               ca.subject_domain, ca.subject_rank, ca.subject_name, ca.note
        FROM citation_association ca JOIN citation c ON c.id = ca.citation_id
        WHERE ca.id IN
        <foreach item='x' collection='ids' open='(' separator=',' close=')'>#{x}::uuid</foreach>
        </script>
        """)
    List<CitationAssociationDbo> selectByIdSet(@Param("ids") Collection<String> ids);

    @Select(SELECT_JOIN + " ORDER BY ca.id LIMIT #{limit} OFFSET #{offset}")
    List<CitationAssociationDbo> selectPage(@Param("limit") int limit, @Param("offset") int offset);

    @Select("""
        SELECT count(*) FROM (
            SELECT 1 FROM citation_association ORDER BY id OFFSET #{skip} LIMIT #{window}
        ) t
        """)
    int countInWindow(@Param("skip") int skip, @Param("window") int window);

    @Select(SELECT_JOIN + " WHERE c.name = #{citationName} ORDER BY ca.id")
    List<CitationAssociationDbo> selectByCitationName(String citationName);

    @Select("""
        SELECT ca.id, c.name AS citation_name,
               ca.subject_domain, ca.subject_rank, ca.subject_name, ca.note
        FROM citation_association ca JOIN citation c ON c.id = ca.citation_id
        WHERE ca.subject_domain = #{domain} AND ca.subject_rank = #{rank} AND ca.subject_name = #{name}
        ORDER BY ca.id
        """)
    List<CitationAssociationDbo> selectBySubject(
            @Param("domain") String domain, @Param("rank") String rank, @Param("name") String name);

    @Select("""
        <script>
        SELECT ca.id, c.name AS citation_name,
               ca.subject_domain, ca.subject_rank, ca.subject_name, ca.note
        FROM citation_association ca JOIN citation c ON c.id = ca.citation_id
        WHERE (ca.subject_domain, ca.subject_rank, ca.subject_name) IN
        <foreach item='k' collection='keys' open='(' separator=',' close=')'>
            (#{k.subjectDomain}, #{k.subjectRank}, #{k.subjectName})
        </foreach>
        ORDER BY ca.id
        </script>
        """)
    List<CitationAssociationDbo> selectBySubjects(@Param("keys") Collection<CitationSubjectKey> keys);

    @Insert("""
        INSERT INTO citation_association (id, citation_id, subject_domain, subject_rank, subject_name, note)
        SELECT #{id}::uuid, id, #{subjectDomain}, #{subjectRank}, #{subjectName}, #{note}
        FROM citation WHERE name = #{citationName}
        """)
    int insert(CitationAssociationDbo dbo);

    @Update("""
        UPDATE citation_association SET
            subject_domain = #{subjectDomain}, subject_rank = #{subjectRank},
            subject_name = #{subjectName}, note = #{note},
            citation_id = (SELECT id FROM citation WHERE name = #{citationName})
        WHERE id = #{id}::uuid
        """)
    int updateById(CitationAssociationDbo dbo);
}
