package com.naturalist.plants.management;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Collection;
import java.util.List;

/**
 * MyBatis mapper for the flat {@code plant_program} table. Snake_case columns auto-map to the DBO's
 * camelCase fields ({@code program_constraint} → {@code programConstraint}); the polymorphic
 * cross-rank reference is the plain {@code (plant_rank, plant_name)} pair, so getByPlantName filters
 * on both. {@code id} is a DB-generated identity, populated on insert.
 */
@Mapper
interface PlantProgramMapper {

    String COLUMNS = """
        id, name, plant_rank, plant_name, program_constraint, notes,
        description_preschool, description_elementary, description_secondary, description_university
        """;

    @Select("SELECT " + COLUMNS + " FROM plant_program WHERE name = #{name}")
    PlantProgramDbo selectByName(String name);

    @Select("""
        <script>
        SELECT id, name, plant_rank, plant_name, program_constraint, notes,
               description_preschool, description_elementary, description_secondary, description_university
        FROM plant_program WHERE name IN
        <foreach item='n' collection='names' open='(' separator=',' close=')'>#{n}</foreach>
        </script>
        """)
    List<PlantProgramDbo> selectByNameSet(@Param("names") Collection<String> names);

    @Select("SELECT " + COLUMNS + " FROM plant_program ORDER BY name LIMIT #{limit} OFFSET #{offset}")
    List<PlantProgramDbo> selectPage(@Param("limit") int limit, @Param("offset") int offset);

    @Select("""
        SELECT count(*) FROM (
            SELECT 1 FROM plant_program ORDER BY name OFFSET #{skip} LIMIT #{window}
        ) t
        """)
    int countInWindow(@Param("skip") int skip, @Param("window") int window);

    /** Programs targeting the given taxon (rank + slug) — backs getByPlantName. */
    @Select("""
        SELECT id, name, plant_rank, plant_name, program_constraint, notes,
               description_preschool, description_elementary, description_secondary, description_university
        FROM plant_program WHERE plant_rank = #{rank} AND plant_name = #{name} ORDER BY name
        """)
    List<PlantProgramDbo> selectByPlantName(@Param("rank") String rank, @Param("name") String name);

    @Insert("""
        INSERT INTO plant_program (name, plant_rank, plant_name, program_constraint, notes,
            description_preschool, description_elementary, description_secondary, description_university)
        VALUES (#{name}, #{plantRank}, #{plantName}, #{programConstraint}, #{notes},
            #{descriptionPreschool}, #{descriptionElementary}, #{descriptionSecondary}, #{descriptionUniversity})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    void insert(PlantProgramDbo dbo);

    @Update("""
        UPDATE plant_program SET
            plant_rank = #{plantRank}, plant_name = #{plantName},
            program_constraint = #{programConstraint}, notes = #{notes},
            description_preschool = #{descriptionPreschool},
            description_elementary = #{descriptionElementary},
            description_secondary = #{descriptionSecondary},
            description_university = #{descriptionUniversity}
        WHERE name = #{name}
        """)
    int updateByName(PlantProgramDbo dbo);
}
