package com.naturalist.soil;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Collection;
import java.util.List;

/**
 * MyBatis mapper for the flat {@code soil_profile_info} table. Snake_case columns auto-map to the
 * DBO's camelCase fields; writes name the columns and bind camelCase params. {@code id} is a
 * DB-generated identity, populated on insert. The zone/sub-zone references are stored flat (no FK,
 * no JOIN); identity at the port is the slug.
 */
@Mapper
interface SoilProfileInfoMapper {

    String COLUMNS = "id, name, zone_name, sub_zone_name";

    @Select("SELECT " + COLUMNS + " FROM soil_profile_info WHERE name = #{name}")
    SoilProfileInfoDbo selectByName(String name);

    @Select("""
        <script>
        SELECT id, name, zone_name, sub_zone_name
        FROM soil_profile_info WHERE name IN
        <foreach item='n' collection='names' open='(' separator=',' close=')'>#{n}</foreach>
        </script>
        """)
    List<SoilProfileInfoDbo> selectByNameSet(@Param("names") Collection<String> names);

    @Select("SELECT " + COLUMNS + " FROM soil_profile_info ORDER BY name LIMIT #{limit} OFFSET #{offset}")
    List<SoilProfileInfoDbo> selectPage(@Param("limit") int limit, @Param("offset") int offset);

    @Select("""
        SELECT count(*) FROM (
            SELECT 1 FROM soil_profile_info ORDER BY name OFFSET #{skip} LIMIT #{window}
        ) t
        """)
    int countInWindow(@Param("skip") int skip, @Param("window") int window);

    @Insert("""
        INSERT INTO soil_profile_info (name, zone_name, sub_zone_name)
        VALUES (#{name}, #{zoneName}, #{subZoneName})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    void insert(SoilProfileInfoDbo dbo);

    @Update("""
        UPDATE soil_profile_info SET zone_name = #{zoneName}, sub_zone_name = #{subZoneName}
        WHERE name = #{name}
        """)
    int updateByName(SoilProfileInfoDbo dbo);
}
