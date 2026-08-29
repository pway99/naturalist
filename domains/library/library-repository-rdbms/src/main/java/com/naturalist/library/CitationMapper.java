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
 * MyBatis mapper for the flat {@code citation} table. Snake_case columns auto-map to the DBO's
 * camelCase fields ({@code authority_source_id} → {@code authoritySourceId},
 * {@code last_modified} → {@code lastModified}); writes name the columns and bind camelCase params.
 * {@code id} is a DB-generated identity, populated on insert.
 */
@Mapper
interface CitationMapper {

    String COLUMNS = """
        id, name, kind, title, author, year, last_modified,
        authority_source_id, authority_source_display_name, authority_url
        """;

    @Select("SELECT " + COLUMNS + " FROM citation WHERE name = #{name}")
    CitationDbo selectByName(String name);

    @Select("""
        <script>
        SELECT id, name, kind, title, author, year, last_modified,
               authority_source_id, authority_source_display_name, authority_url
        FROM citation WHERE name IN
        <foreach item='n' collection='names' open='(' separator=',' close=')'>#{n}</foreach>
        </script>
        """)
    List<CitationDbo> selectByNameSet(@Param("names") Collection<String> names);

    @Select("SELECT " + COLUMNS + " FROM citation ORDER BY name LIMIT #{limit} OFFSET #{offset}")
    List<CitationDbo> selectPage(@Param("limit") int limit, @Param("offset") int offset);

    @Select("""
        SELECT count(*) FROM (
            SELECT 1 FROM citation ORDER BY name OFFSET #{skip} LIMIT #{window}
        ) t
        """)
    int countInWindow(@Param("skip") int skip, @Param("window") int window);

    @Insert("""
        INSERT INTO citation (name, kind, title, author, year, last_modified,
            authority_source_id, authority_source_display_name, authority_url)
        VALUES (#{name}, #{kind}, #{title}, #{author}, #{year}, #{lastModified},
            #{authoritySourceId}, #{authoritySourceDisplayName}, #{authorityUrl})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    void insert(CitationDbo dbo);

    @Update("""
        UPDATE citation SET kind = #{kind}, title = #{title}, author = #{author}, year = #{year},
            last_modified = #{lastModified}, authority_source_id = #{authoritySourceId},
            authority_source_display_name = #{authoritySourceDisplayName}, authority_url = #{authorityUrl}
        WHERE name = #{name}
        """)
    int updateByName(CitationDbo dbo);
}
