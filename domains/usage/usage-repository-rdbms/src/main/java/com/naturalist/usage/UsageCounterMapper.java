package com.naturalist.usage;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Collection;
import java.util.List;

/** MyBatis mapper for the flat {@code usage_counter} table. The uuid {@code id} travels as text with a {@code ::uuid} cast. */
@Mapper
interface UsageCounterMapper {

    String COLUMNS = "id, counter_name, scope, window_kind, since, limit_value, active";

    @Select("SELECT " + COLUMNS + " FROM usage_counter WHERE id = #{id}::uuid")
    UsageCounterDbo selectById(String id);

    @Select("""
        <script>
        SELECT id, counter_name, scope, window_kind, since, limit_value, active FROM usage_counter
        WHERE id IN <foreach item='x' collection='ids' open='(' separator=',' close=')'>#{x}::uuid</foreach>
        </script>
        """)
    List<UsageCounterDbo> selectByIdSet(@Param("ids") Collection<String> ids);

    @Select("SELECT " + COLUMNS + " FROM usage_counter ORDER BY id LIMIT #{limit} OFFSET #{offset}")
    List<UsageCounterDbo> selectPage(@Param("limit") int limit, @Param("offset") int offset);

    @Select("""
        SELECT count(*) FROM (SELECT 1 FROM usage_counter ORDER BY id OFFSET #{skip} LIMIT #{window}) t
        """)
    int countInWindow(@Param("skip") int skip, @Param("window") int window);

    @Select("SELECT " + COLUMNS + " FROM usage_counter WHERE counter_name = #{counterName} ORDER BY id")
    List<UsageCounterDbo> selectByCounterName(String counterName);

    @Insert("""
        INSERT INTO usage_counter (id, counter_name, scope, window_kind, since, limit_value, active)
        VALUES (#{id}::uuid, #{counterName}, #{scope}, #{windowKind}, #{since}, #{limitValue}, #{active})
        """)
    void insert(UsageCounterDbo dbo);

    @Update("""
        UPDATE usage_counter SET counter_name = #{counterName}, scope = #{scope}, window_kind = #{windowKind},
            since = #{since}, limit_value = #{limitValue}, active = #{active}
        WHERE id = #{id}::uuid
        """)
    int updateById(UsageCounterDbo dbo);
}
