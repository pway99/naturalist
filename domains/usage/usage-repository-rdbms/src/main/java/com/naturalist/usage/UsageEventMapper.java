package com.naturalist.usage;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Collection;
import java.util.List;

/** MyBatis mapper for the flat {@code usage_event} table. The uuid {@code id} travels as text ({@code ::uuid}). */
@Mapper
interface UsageEventMapper {

    String COLUMNS = "id, counter_name, naturalist, instant";

    @Select("SELECT " + COLUMNS + " FROM usage_event WHERE id = #{id}::uuid")
    UsageEventDbo selectById(String id);

    @Select("""
        <script>
        SELECT id, counter_name, naturalist, instant FROM usage_event
        WHERE id IN <foreach item='x' collection='ids' open='(' separator=',' close=')'>#{x}::uuid</foreach>
        </script>
        """)
    List<UsageEventDbo> selectByIdSet(@Param("ids") Collection<String> ids);

    @Select("SELECT " + COLUMNS + " FROM usage_event ORDER BY id LIMIT #{limit} OFFSET #{offset}")
    List<UsageEventDbo> selectPage(@Param("limit") int limit, @Param("offset") int offset);

    @Select("""
        SELECT count(*) FROM (SELECT 1 FROM usage_event ORDER BY id OFFSET #{skip} LIMIT #{window}) t
        """)
    int countInWindow(@Param("skip") int skip, @Param("window") int window);

    @Select("""
        <script>
        SELECT id, counter_name, naturalist, instant FROM usage_event
        WHERE counter_name = #{counter} AND instant >= #{since}
        <if test='naturalist != null'>AND naturalist = #{naturalist}</if>
        ORDER BY instant
        </script>
        """)
    List<UsageEventDbo> findByCounterSince(
            @Param("counter") String counter, @Param("naturalist") String naturalist, @Param("since") java.time.Instant since);

    @Insert("""
        INSERT INTO usage_event (id, counter_name, naturalist, instant)
        VALUES (#{id}::uuid, #{counterName}, #{naturalist}, #{instant})
        """)
    void insert(UsageEventDbo dbo);

    @Update("""
        UPDATE usage_event SET counter_name = #{counterName}, naturalist = #{naturalist}, instant = #{instant}
        WHERE id = #{id}::uuid
        """)
    int updateById(UsageEventDbo dbo);
}
