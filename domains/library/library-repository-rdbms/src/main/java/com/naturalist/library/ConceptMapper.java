package com.naturalist.library;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Collection;
import java.util.List;

/**
 * MyBatis mapper for the flat {@code concept} table. Snake_case columns auto-map to the DBO's
 * camelCase fields ({@code description_preschool} → {@code descriptionPreschool}); writes name the
 * columns and bind camelCase params. {@code id} is a DB-generated identity, populated on insert.
 */
@Mapper
interface ConceptMapper {

    String COLUMNS = """
        id, name, title,
        description_preschool, description_elementary, description_secondary, description_university
        """;

    @Select("SELECT " + COLUMNS + " FROM concept WHERE name = #{name}")
    ConceptDbo selectByName(String name);

    @Select("""
        <script>
        SELECT id, name, title,
               description_preschool, description_elementary, description_secondary, description_university
        FROM concept WHERE name IN
        <foreach item='n' collection='names' open='(' separator=',' close=')'>#{n}</foreach>
        </script>
        """)
    List<ConceptDbo> selectByNameSet(@Param("names") Collection<String> names);

    @Select("SELECT " + COLUMNS + " FROM concept ORDER BY name LIMIT #{limit} OFFSET #{offset}")
    List<ConceptDbo> selectPage(@Param("limit") int limit, @Param("offset") int offset);

    @Select("""
        SELECT count(*) FROM (
            SELECT 1 FROM concept ORDER BY name OFFSET #{skip} LIMIT #{window}
        ) t
        """)
    int countInWindow(@Param("skip") int skip, @Param("window") int window);

    @Insert("""
        INSERT INTO concept (name, title,
            description_preschool, description_elementary, description_secondary, description_university)
        VALUES (#{name}, #{title},
            #{descriptionPreschool}, #{descriptionElementary}, #{descriptionSecondary}, #{descriptionUniversity})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    void insert(ConceptDbo dbo);

    @Update("""
        UPDATE concept SET title = #{title},
            description_preschool = #{descriptionPreschool},
            description_elementary = #{descriptionElementary},
            description_secondary = #{descriptionSecondary},
            description_university = #{descriptionUniversity}
        WHERE name = #{name}
        """)
    int updateByName(ConceptDbo dbo);
}
