package com.naturalist.naturalist;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Collection;
import java.util.List;

@Mapper
interface NaturalistMapper {

    @Select("""
        SELECT id, name, given_name, family_name, role, stage, notes
        FROM naturalist WHERE name = #{name}
        """)
    NaturalistDbo selectByName(String name);

    @Select("""
        <script>
        SELECT id, name, given_name, family_name, role, stage, notes
        FROM naturalist
        WHERE name IN
        <foreach item='n' collection='names' open='(' separator=',' close=')'>#{n}</foreach>
        </script>
        """)
    List<NaturalistDbo> selectByNameSet(@Param("names") Collection<String> names);

    @Select("""
        SELECT id, name, given_name, family_name, role, stage, notes
        FROM naturalist ORDER BY name LIMIT #{limit} OFFSET #{offset}
        """)
    List<NaturalistDbo> selectPage(@Param("limit") int limit, @Param("offset") int offset);

    @Select("""
        SELECT count(*) FROM (
            SELECT 1 FROM naturalist ORDER BY name OFFSET #{skip} LIMIT #{window}
        ) t
        """)
    int countInWindow(@Param("skip") int skip, @Param("window") int window);

    @Insert("""
        INSERT INTO naturalist (name, given_name, family_name, role, stage, notes)
        VALUES (#{name}, #{given_name}, #{family_name}, #{role}, #{stage}, #{notes})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    void insert(NaturalistDbo dbo);

    @Update("""
        UPDATE naturalist SET given_name = #{given_name}, family_name = #{family_name},
               role = #{role}, stage = #{stage}, notes = #{notes}
        WHERE name = #{name}
        """)
    int updateByName(NaturalistDbo dbo);
}
