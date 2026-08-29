package com.naturalist.plants.phytochemistry;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Collection;
import java.util.List;

/**
 * MyBatis mapper for the {@code phytochemical_constituent} link entity. The parent row's 1:1
 * {@link com.naturalist.fieldnotes.Description} is four flat columns (auto-mapped snake→camel);
 * the two multi-valued members live in child tables and are loaded one batched query each (keyed
 * on {@code constituent_id}, projecting {@code phytochemical_constituent.name} so the adapter can
 * group by name). Child inserts nested-select the {@code constituent_id} from the constituent name,
 * so no generated id has to be threaded back. Two reverse lookups back the repository's
 * {@code getByPlantName} / {@code getByCompoundName} projections.
 */
@Mapper
interface PhytochemicalConstituentMapper {

    String COLUMNS = """
        name, plant_rank, plant_name, compound_name, category, induction, notes,
        description_preschool, description_elementary, description_secondary, description_university
        """;

    // ── parent reads ─────────────────────────────────────────────────────────
    @Select("SELECT " + COLUMNS + " FROM phytochemical_constituent WHERE name = #{name}")
    PhytochemicalConstituentDbo selectByName(String name);

    @Select("""
        <script>
        SELECT name, plant_rank, plant_name, compound_name, category, induction, notes,
               description_preschool, description_elementary, description_secondary, description_university
        FROM phytochemical_constituent WHERE name IN
        <foreach item='n' collection='names' open='(' separator=',' close=')'>#{n}</foreach>
        </script>
        """)
    List<PhytochemicalConstituentDbo> selectByNameSet(@Param("names") Collection<String> names);

    @Select("SELECT " + COLUMNS + " FROM phytochemical_constituent ORDER BY name LIMIT #{limit} OFFSET #{offset}")
    List<PhytochemicalConstituentDbo> selectPage(@Param("limit") int limit, @Param("offset") int offset);

    @Select("""
        SELECT count(*) FROM (
            SELECT 1 FROM phytochemical_constituent ORDER BY name OFFSET #{skip} LIMIT #{window}
        ) t
        """)
    int countInWindow(@Param("skip") int skip, @Param("window") int window);

    // ── reverse lookups (constituent names for a plant / a compound) ──────────
    @Select("""
        SELECT name FROM phytochemical_constituent
        WHERE plant_rank = #{plantRank} AND plant_name = #{plantName} ORDER BY name
        """)
    List<String> selectNamesByPlant(@Param("plantRank") String plantRank, @Param("plantName") String plantName);

    @Select("""
        SELECT name FROM phytochemical_constituent WHERE compound_name = #{compoundName} ORDER BY name
        """)
    List<String> selectNamesByCompound(String compoundName);

    // ── child reads (one batched query each; project constituent.name for grouping) ──
    @Select("""
        <script>
        SELECT c.name AS constituent_name, r.role_kind
        FROM phytochemical_constituent_role r JOIN phytochemical_constituent c ON c.id = r.constituent_id
        WHERE c.name IN
        <foreach item='n' collection='names' open='(' separator=',' close=')'>#{n}</foreach>
        </script>
        """)
    List<PhytochemicalConstituentRoleDbo> selectRoles(@Param("names") Collection<String> names);

    @Select("""
        <script>
        SELECT c.name AS constituent_name, t.tissue
        FROM phytochemical_constituent_tissue t JOIN phytochemical_constituent c ON c.id = t.constituent_id
        WHERE c.name IN
        <foreach item='n' collection='names' open='(' separator=',' close=')'>#{n}</foreach>
        </script>
        """)
    List<PhytochemicalConstituentTissueDbo> selectTissues(@Param("names") Collection<String> names);

    // ── parent writes ────────────────────────────────────────────────────────
    @Insert("""
        INSERT INTO phytochemical_constituent (
            name, plant_rank, plant_name, compound_name, category, induction, notes,
            description_preschool, description_elementary, description_secondary, description_university)
        VALUES (
            #{name}, #{plantRank}, #{plantName}, #{compoundName}, #{category}, #{induction}, #{notes},
            #{descriptionPreschool}, #{descriptionElementary}, #{descriptionSecondary}, #{descriptionUniversity})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    void insertConstituent(PhytochemicalConstituentDbo dbo);

    @Update("""
        UPDATE phytochemical_constituent SET
            plant_rank = #{plantRank}, plant_name = #{plantName}, compound_name = #{compoundName},
            category = #{category}, induction = #{induction}, notes = #{notes},
            description_preschool = #{descriptionPreschool}, description_elementary = #{descriptionElementary},
            description_secondary = #{descriptionSecondary}, description_university = #{descriptionUniversity}
        WHERE name = #{name}
        """)
    int updateConstituent(PhytochemicalConstituentDbo dbo);

    // ── child writes (nested-select the constituent_id from the name) ─────────
    @Insert("""
        INSERT INTO phytochemical_constituent_role (constituent_id, role_kind)
        SELECT id, #{roleKind} FROM phytochemical_constituent WHERE name = #{constituentName}
        """)
    int insertRole(PhytochemicalConstituentRoleDbo dbo);

    @Insert("""
        INSERT INTO phytochemical_constituent_tissue (constituent_id, tissue)
        SELECT id, #{tissue} FROM phytochemical_constituent WHERE name = #{constituentName}
        """)
    int insertTissue(PhytochemicalConstituentTissueDbo dbo);

    @Delete("""
        DELETE FROM phytochemical_constituent_role
        WHERE constituent_id = (SELECT id FROM phytochemical_constituent WHERE name = #{name})
        """)
    void deleteRoles(String name);

    @Delete("""
        DELETE FROM phytochemical_constituent_tissue
        WHERE constituent_id = (SELECT id FROM phytochemical_constituent WHERE name = #{name})
        """)
    void deleteTissues(String name);
}
