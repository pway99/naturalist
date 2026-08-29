package com.naturalist.chemistry.element;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Collection;
import java.util.List;

/**
 * MyBatis mapper for the flat {@code element} table. Snake_case columns auto-map to the DBO's
 * camelCase fields ({@code atomic_weight} → {@code atomicWeight}); writes name the columns and
 * bind camelCase params. {@code id} is a DB-generated identity, populated on insert.
 */
@Mapper
interface ElementMapper {

    String COLUMNS = "id, name, symbol, atomic_weight, ionic_form, ionic_charge";

    @Select("SELECT " + COLUMNS + " FROM element WHERE name = #{name}")
    ElementDbo selectByName(String name);

    @Select("""
        <script>
        SELECT id, name, symbol, atomic_weight, ionic_form, ionic_charge FROM element
        WHERE name IN
        <foreach item='n' collection='names' open='(' separator=',' close=')'>#{n}</foreach>
        </script>
        """)
    List<ElementDbo> selectByNameSet(@Param("names") Collection<String> names);

    @Select("SELECT " + COLUMNS + " FROM element ORDER BY name LIMIT #{limit} OFFSET #{offset}")
    List<ElementDbo> selectPage(@Param("limit") int limit, @Param("offset") int offset);

    @Select("""
        SELECT count(*) FROM (
            SELECT 1 FROM element ORDER BY name OFFSET #{skip} LIMIT #{window}
        ) t
        """)
    int countInWindow(@Param("skip") int skip, @Param("window") int window);

    @Insert("""
        INSERT INTO element (name, symbol, atomic_weight, ionic_form, ionic_charge)
        VALUES (#{name}, #{symbol}, #{atomicWeight}, #{ionicForm}, #{ionicCharge})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    void insert(ElementDbo dbo);

    @Update("""
        UPDATE element SET symbol = #{symbol}, atomic_weight = #{atomicWeight},
               ionic_form = #{ionicForm}, ionic_charge = #{ionicCharge}
        WHERE name = #{name}
        """)
    int updateByName(ElementDbo dbo);
}
