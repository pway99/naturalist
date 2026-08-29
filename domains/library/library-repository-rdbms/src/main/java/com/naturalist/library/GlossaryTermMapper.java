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
 * MyBatis mapper for the flat {@code glossary_term} table. Snake_case columns auto-map to the DBO's
 * camelCase fields; writes name the columns and bind camelCase params. {@code id} is a DB-generated
 * identity, populated on insert.
 */
@Mapper
interface GlossaryTermMapper {

    String COLUMNS = "id, name, term, definition, example";

    @Select("SELECT " + COLUMNS + " FROM glossary_term WHERE name = #{name}")
    GlossaryTermDbo selectByName(String name);

    @Select("""
        <script>
        SELECT id, name, term, definition, example FROM glossary_term
        WHERE name IN
        <foreach item='n' collection='names' open='(' separator=',' close=')'>#{n}</foreach>
        </script>
        """)
    List<GlossaryTermDbo> selectByNameSet(@Param("names") Collection<String> names);

    @Select("SELECT " + COLUMNS + " FROM glossary_term ORDER BY name LIMIT #{limit} OFFSET #{offset}")
    List<GlossaryTermDbo> selectPage(@Param("limit") int limit, @Param("offset") int offset);

    @Select("""
        SELECT count(*) FROM (
            SELECT 1 FROM glossary_term ORDER BY name OFFSET #{skip} LIMIT #{window}
        ) t
        """)
    int countInWindow(@Param("skip") int skip, @Param("window") int window);

    @Insert("""
        INSERT INTO glossary_term (name, term, definition, example)
        VALUES (#{name}, #{term}, #{definition}, #{example})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    void insert(GlossaryTermDbo dbo);

    @Update("""
        UPDATE glossary_term SET term = #{term}, definition = #{definition}, example = #{example}
        WHERE name = #{name}
        """)
    int updateByName(GlossaryTermDbo dbo);
}
