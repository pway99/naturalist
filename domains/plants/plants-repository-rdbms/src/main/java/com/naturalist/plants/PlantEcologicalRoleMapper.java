package com.naturalist.plants;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Collection;
import java.util.List;

/**
 * MyBatis mapper for {@code plant_ecological_role} and its {@code plant_ecological_role_role} child
 * table. The uuid {@code id} travels as text and is cast in SQL ({@code ::uuid}). The child roles
 * are loaded in one batched query keyed on the id set ({@link #selectRolesByIds}) and grouped by
 * {@code role_id} in the adapter — never per-parent. Child inserts carry the parent's own id
 * directly, so no generated key has to be threaded back.
 */
@Mapper
interface PlantEcologicalRoleMapper {

    String COLUMNS = "id, plant_rank, plant_name";

    @Select("SELECT " + COLUMNS + " FROM plant_ecological_role WHERE id = #{id}::uuid")
    PlantEcologicalRoleDbo selectById(String id);

    @Select("""
        <script>
        SELECT id, plant_rank, plant_name FROM plant_ecological_role WHERE id IN
        <foreach item='x' collection='ids' open='(' separator=',' close=')'>#{x}::uuid</foreach>
        </script>
        """)
    List<PlantEcologicalRoleDbo> selectByIdSet(@Param("ids") Collection<String> ids);

    @Select("SELECT " + COLUMNS + " FROM plant_ecological_role ORDER BY id LIMIT #{limit} OFFSET #{offset}")
    List<PlantEcologicalRoleDbo> selectPage(@Param("limit") int limit, @Param("offset") int offset);

    @Select("""
        SELECT count(*) FROM (
            SELECT 1 FROM plant_ecological_role ORDER BY id OFFSET #{skip} LIMIT #{window}
        ) t
        """)
    int countInWindow(@Param("skip") int skip, @Param("window") int window);

    @Select("""
        SELECT id, plant_rank, plant_name FROM plant_ecological_role
        WHERE plant_rank = #{rank} AND plant_name = #{name}
        """)
    PlantEcologicalRoleDbo selectByPlantName(@Param("rank") String rank, @Param("name") String name);

    @Select("""
        <script>
        SELECT role_id, role FROM plant_ecological_role_role WHERE role_id IN
        <foreach item='x' collection='ids' open='(' separator=',' close=')'>#{x}::uuid</foreach>
        </script>
        """)
    List<PlantEcologicalRoleRoleDbo> selectRolesByIds(@Param("ids") Collection<String> ids);

    @Insert("""
        INSERT INTO plant_ecological_role (id, plant_rank, plant_name)
        VALUES (#{id}::uuid, #{plantRank}, #{plantName})
        """)
    int insert(PlantEcologicalRoleDbo dbo);

    @Insert("INSERT INTO plant_ecological_role_role (role_id, role) VALUES (#{roleId}::uuid, #{role})")
    int insertRole(PlantEcologicalRoleRoleDbo dbo);

    @Update("""
        UPDATE plant_ecological_role SET plant_rank = #{plantRank}, plant_name = #{plantName}
        WHERE id = #{id}::uuid
        """)
    int updateById(PlantEcologicalRoleDbo dbo);

    @Delete("DELETE FROM plant_ecological_role_role WHERE role_id = #{id}::uuid")
    void deleteRoles(String id);
}
