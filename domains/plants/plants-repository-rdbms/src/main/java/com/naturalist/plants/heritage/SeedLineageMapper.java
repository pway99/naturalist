package com.naturalist.plants.heritage;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Collection;
import java.util.List;

/**
 * MyBatis mapper for the {@code seed_lineage} table. Reads JOIN {@code cultivar} and project its
 * {@code name} as {@code cultivar_name} (so the DBO speaks the cultivar slug, never the numeric id);
 * writes name the columns and bind camelCase params, nested-selecting {@code cultivar_id} from the
 * slug. A 0-row {@link #insert} means the referenced cultivar is absent — the adapter maps that to
 * {@code EntityNotFoundException}. {@code id} is a DB-generated identity.
 */
@Mapper
interface SeedLineageMapper {

    String COLUMNS = """
        l.id, l.name, c.name AS cultivar_name, l.adaptation_start_year,
        l.selection_criteria, l.notes,
        l.provenance_originator, l.provenance_origin_location,
        l.provenance_estimated_generations, l.provenance_source_notes,
        l.description_preschool, l.description_elementary, l.description_secondary, l.description_university
        """;

    String FROM = " FROM seed_lineage l JOIN cultivar c ON c.id = l.cultivar_id ";

    @Select("SELECT " + COLUMNS + FROM + "WHERE l.name = #{name}")
    SeedLineageDbo selectByName(String name);

    @Select("""
        <script>
        SELECT l.id, l.name, c.name AS cultivar_name, l.adaptation_start_year,
               l.selection_criteria, l.notes,
               l.provenance_originator, l.provenance_origin_location,
               l.provenance_estimated_generations, l.provenance_source_notes,
               l.description_preschool, l.description_elementary, l.description_secondary, l.description_university
        FROM seed_lineage l JOIN cultivar c ON c.id = l.cultivar_id
        WHERE l.name IN
        <foreach item='n' collection='names' open='(' separator=',' close=')'>#{n}</foreach>
        </script>
        """)
    List<SeedLineageDbo> selectByNameSet(@Param("names") Collection<String> names);

    @Select("SELECT " + COLUMNS + FROM + "ORDER BY l.name LIMIT #{limit} OFFSET #{offset}")
    List<SeedLineageDbo> selectPage(@Param("limit") int limit, @Param("offset") int offset);

    @Select("""
        SELECT count(*) FROM (
            SELECT 1 FROM seed_lineage ORDER BY name OFFSET #{skip} LIMIT #{window}
        ) t
        """)
    int countInWindow(@Param("skip") int skip, @Param("window") int window);

    /** Lineages whose cultivar FK resolves to the given cultivar slug — backs getByCultivarName. */
    @Select("SELECT " + COLUMNS + FROM + "WHERE c.name = #{cultivarName} ORDER BY l.name")
    List<SeedLineageDbo> selectByCultivarName(String cultivarName);

    @Insert("""
        INSERT INTO seed_lineage (name, cultivar_id, adaptation_start_year, selection_criteria, notes,
            provenance_originator, provenance_origin_location,
            provenance_estimated_generations, provenance_source_notes,
            description_preschool, description_elementary, description_secondary, description_university)
        SELECT #{name}, c.id, #{adaptationStartYear}, #{selectionCriteria}, #{notes},
            #{provenanceOriginator}, #{provenanceOriginLocation},
            #{provenanceEstimatedGenerations}, #{provenanceSourceNotes},
            #{descriptionPreschool}, #{descriptionElementary}, #{descriptionSecondary}, #{descriptionUniversity}
        FROM cultivar c WHERE c.name = #{cultivarName}
        """)
    int insert(SeedLineageDbo dbo);

    @Update("""
        UPDATE seed_lineage SET
            cultivar_id = (SELECT id FROM cultivar WHERE name = #{cultivarName}),
            adaptation_start_year = #{adaptationStartYear},
            selection_criteria = #{selectionCriteria}, notes = #{notes},
            provenance_originator = #{provenanceOriginator},
            provenance_origin_location = #{provenanceOriginLocation},
            provenance_estimated_generations = #{provenanceEstimatedGenerations},
            provenance_source_notes = #{provenanceSourceNotes},
            description_preschool = #{descriptionPreschool},
            description_elementary = #{descriptionElementary},
            description_secondary = #{descriptionSecondary},
            description_university = #{descriptionUniversity}
        WHERE name = #{name}
        """)
    int updateByName(SeedLineageDbo dbo);
}
