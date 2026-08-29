package com.naturalist.insects;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Collection;
import java.util.List;

/**
 * MyBatis mapper for {@code insect_functional_role} and its {@code insect_functional_role_guild}
 * child table. The uuid {@code id} travels as text and is cast in SQL ({@code ::uuid}). The child
 * guilds are loaded in one batched query keyed on the id set ({@link #selectGuildsByIds}) and grouped
 * by {@code role_id} in the adapter — never per-parent. Child inserts carry the parent's own id
 * directly, so no generated key has to be threaded back. {@code getByGuild} joins the child table.
 */
@Mapper
interface InsectFunctionalRoleMapper {

    String COLUMNS = "id, parent_rank, parent_name, beneficial";

    @Select("SELECT " + COLUMNS + " FROM insect_functional_role WHERE id = #{id}::uuid")
    InsectFunctionalRoleDbo selectById(String id);

    @Select("""
        <script>
        SELECT id, parent_rank, parent_name, beneficial FROM insect_functional_role WHERE id IN
        <foreach item='x' collection='ids' open='(' separator=',' close=')'>#{x}::uuid</foreach>
        </script>
        """)
    List<InsectFunctionalRoleDbo> selectByIdSet(@Param("ids") Collection<String> ids);

    @Select("SELECT " + COLUMNS + " FROM insect_functional_role ORDER BY id LIMIT #{limit} OFFSET #{offset}")
    List<InsectFunctionalRoleDbo> selectPage(@Param("limit") int limit, @Param("offset") int offset);

    @Select("""
        SELECT count(*) FROM (
            SELECT 1 FROM insect_functional_role ORDER BY id OFFSET #{skip} LIMIT #{window}
        ) t
        """)
    int countInWindow(@Param("skip") int skip, @Param("window") int window);

    @Select("""
        SELECT id, parent_rank, parent_name, beneficial FROM insect_functional_role
        WHERE parent_rank = #{rank} AND parent_name = #{name}
        """)
    InsectFunctionalRoleDbo selectByParentName(@Param("rank") String rank, @Param("name") String name);

    @Select("""
        <script>
        SELECT id, parent_rank, parent_name, beneficial FROM insect_functional_role
        WHERE (parent_rank, parent_name) IN
        <foreach item='k' collection='keys' open='(' separator=',' close=')'>
            (#{k.rank}, #{k.name})
        </foreach>
        </script>
        """)
    List<InsectFunctionalRoleDbo> selectByParentNames(@Param("keys") Collection<InsectRankKey> keys);

    @Select("""
        SELECT r.id, r.parent_rank, r.parent_name, r.beneficial FROM insect_functional_role r
        JOIN insect_functional_role_guild g ON g.role_id = r.id
        WHERE g.guild = #{guild}
        ORDER BY r.id
        """)
    List<InsectFunctionalRoleDbo> selectByGuild(String guild);

    @Select("""
        <script>
        SELECT role_id, guild FROM insect_functional_role_guild WHERE role_id IN
        <foreach item='x' collection='ids' open='(' separator=',' close=')'>#{x}::uuid</foreach>
        </script>
        """)
    List<InsectFunctionalRoleGuildDbo> selectGuildsByIds(@Param("ids") Collection<String> ids);

    @Insert("""
        INSERT INTO insect_functional_role (id, parent_rank, parent_name, beneficial)
        VALUES (#{id}::uuid, #{parentRank}, #{parentName}, #{beneficial})
        """)
    int insert(InsectFunctionalRoleDbo dbo);

    @Insert("INSERT INTO insect_functional_role_guild (role_id, guild) VALUES (#{roleId}::uuid, #{guild})")
    int insertGuild(InsectFunctionalRoleGuildDbo dbo);

    @Update("""
        UPDATE insect_functional_role SET parent_rank = #{parentRank}, parent_name = #{parentName},
            beneficial = #{beneficial}
        WHERE id = #{id}::uuid
        """)
    int updateById(InsectFunctionalRoleDbo dbo);

    @Delete("DELETE FROM insect_functional_role_guild WHERE role_id = #{id}::uuid")
    void deleteGuilds(String id);
}
