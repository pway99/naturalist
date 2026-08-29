package com.naturalist.usage;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Collection;
import java.util.List;

/** MyBatis mapper for the flat {@code usage_alert} table. The uuid {@code id} travels as text ({@code ::uuid}). */
@Mapper
interface UsageAlertMapper {

    String COLUMNS = "id, counter, scope, kind, period, message, at, emailed, acknowledged";

    @Select("SELECT " + COLUMNS + " FROM usage_alert WHERE id = #{id}::uuid")
    UsageAlertDbo selectById(String id);

    @Select("""
        <script>
        SELECT id, counter, scope, kind, period, message, at, emailed, acknowledged FROM usage_alert
        WHERE id IN <foreach item='x' collection='ids' open='(' separator=',' close=')'>#{x}::uuid</foreach>
        </script>
        """)
    List<UsageAlertDbo> selectByIdSet(@Param("ids") Collection<String> ids);

    @Select("SELECT " + COLUMNS + " FROM usage_alert ORDER BY id LIMIT #{limit} OFFSET #{offset}")
    List<UsageAlertDbo> selectPage(@Param("limit") int limit, @Param("offset") int offset);

    @Select("""
        SELECT count(*) FROM (SELECT 1 FROM usage_alert ORDER BY id OFFSET #{skip} LIMIT #{window}) t
        """)
    int countInWindow(@Param("skip") int skip, @Param("window") int window);

    @Select("""
        SELECT id, counter, scope, kind, period, message, at, emailed, acknowledged FROM usage_alert
        WHERE counter = #{counter} AND scope = #{scope} AND kind = #{kind} AND period = #{period}
        """)
    UsageAlertDbo selectDedupKey(@Param("counter") String counter, @Param("scope") String scope,
                                 @Param("kind") String kind, @Param("period") String period);

    @Select("SELECT " + COLUMNS + " FROM usage_alert WHERE acknowledged = false ORDER BY at DESC")
    List<UsageAlertDbo> selectUnacknowledged();

    @Select("SELECT " + COLUMNS + " FROM usage_alert WHERE emailed = false ORDER BY at DESC")
    List<UsageAlertDbo> selectUnsent();

    @Insert("""
        INSERT INTO usage_alert (id, counter, scope, kind, period, message, at, emailed, acknowledged)
        VALUES (#{id}::uuid, #{counter}, #{scope}, #{kind}, #{period}, #{message}, #{at}, #{emailed}, #{acknowledged})
        """)
    void insert(UsageAlertDbo dbo);

    @Update("""
        UPDATE usage_alert SET counter = #{counter}, scope = #{scope}, kind = #{kind}, period = #{period},
            message = #{message}, at = #{at}, emailed = #{emailed}, acknowledged = #{acknowledged}
        WHERE id = #{id}::uuid
        """)
    int updateById(UsageAlertDbo dbo);
}
