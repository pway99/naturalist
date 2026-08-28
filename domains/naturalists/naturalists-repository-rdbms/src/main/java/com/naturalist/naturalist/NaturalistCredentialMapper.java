package com.naturalist.naturalist;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Collection;
import java.util.List;

@Mapper
interface NaturalistCredentialMapper {

    @Select("""
        SELECT c.password_hash, n.name AS name
        FROM naturalist_credential c JOIN naturalist n ON n.id = c.naturalist_id
        WHERE n.name = #{name}
        """)
    NaturalistCredentialDbo selectByName(String name);

    @Select("""
        <script>
        SELECT c.password_hash, n.name AS name
        FROM naturalist_credential c JOIN naturalist n ON n.id = c.naturalist_id
        WHERE n.name IN
        <foreach item='x' collection='names' open='(' separator=',' close=')'>#{x}</foreach>
        </script>
        """)
    List<NaturalistCredentialDbo> selectByNameSet(@Param("names") Collection<String> names);

    @Select("""
        SELECT c.password_hash, n.name AS name
        FROM naturalist_credential c JOIN naturalist n ON n.id = c.naturalist_id
        ORDER BY n.name LIMIT #{limit} OFFSET #{offset}
        """)
    List<NaturalistCredentialDbo> selectPage(@Param("limit") int limit, @Param("offset") int offset);

    @Select("""
        SELECT count(*) FROM (
            SELECT 1 FROM naturalist_credential c JOIN naturalist n ON n.id = c.naturalist_id
            ORDER BY n.name OFFSET #{skip} LIMIT #{window}
        ) t
        """)
    int countInWindow(@Param("skip") int skip, @Param("window") int window);

    @Insert("""
        INSERT INTO naturalist_credential (naturalist_id, password_hash)
        SELECT id, #{password_hash} FROM naturalist WHERE name = #{name}
        """)
    int insert(NaturalistCredentialDbo dbo);

    @Update("""
        UPDATE naturalist_credential SET password_hash = #{password_hash}
        WHERE naturalist_id = (SELECT id FROM naturalist WHERE name = #{name})
        """)
    int updateByName(NaturalistCredentialDbo dbo);
}
