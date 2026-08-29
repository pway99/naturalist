package com.naturalist.chemistry.compound;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Collection;
import java.util.List;

/**
 * MyBatis mapper for {@code compound_depiction} — a surrogate-UUID entity. Reads JOIN
 * {@code compound} to project the compound {@code name} for reconstruction; writes nested-select
 * {@code compound.id} from that name. The uuid {@code id} travels as text and is cast to uuid in
 * SQL ({@code ::uuid}) — no MyBatis type handler.
 */
@Mapper
interface DepictionMapper {

    String SELECT_JOIN = """
        SELECT d.id, c.name AS compound_name, d.smiles, d.note
        FROM compound_depiction d JOIN compound c ON c.id = d.compound_id
        """;

    @Select(SELECT_JOIN + " WHERE d.id = #{id}::uuid")
    CompoundDepictionDbo selectById(String id);

    @Select("""
        <script>
        SELECT d.id, c.name AS compound_name, d.smiles, d.note
        FROM compound_depiction d JOIN compound c ON c.id = d.compound_id
        WHERE d.id IN
        <foreach item='x' collection='ids' open='(' separator=',' close=')'>#{x}::uuid</foreach>
        </script>
        """)
    List<CompoundDepictionDbo> selectByIdSet(@Param("ids") Collection<String> ids);

    @Select(SELECT_JOIN + " ORDER BY d.id LIMIT #{limit} OFFSET #{offset}")
    List<CompoundDepictionDbo> selectPage(@Param("limit") int limit, @Param("offset") int offset);

    @Select("""
        SELECT count(*) FROM (
            SELECT 1 FROM compound_depiction ORDER BY id OFFSET #{skip} LIMIT #{window}
        ) t
        """)
    int countInWindow(@Param("skip") int skip, @Param("window") int window);

    @Select(SELECT_JOIN + " WHERE c.name = #{compoundName}")
    CompoundDepictionDbo selectByCompoundName(String compoundName);

    @Select("""
        SELECT c.name FROM compound_depiction d JOIN compound c ON c.id = d.compound_id
        ORDER BY c.name
        """)
    List<String> selectAllDepictedCompoundNames();

    @Insert("""
        INSERT INTO compound_depiction (id, compound_id, smiles, note)
        SELECT #{id}::uuid, id, #{smiles}, #{note} FROM compound WHERE name = #{compoundName}
        """)
    int insert(CompoundDepictionDbo dbo);

    @Update("""
        UPDATE compound_depiction SET
            smiles = #{smiles}, note = #{note},
            compound_id = (SELECT id FROM compound WHERE name = #{compoundName})
        WHERE id = #{id}::uuid
        """)
    int updateById(CompoundDepictionDbo dbo);
}
