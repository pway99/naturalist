package com.naturalist.account;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Collection;
import java.util.List;

@Mapper
interface AccountMapper {

    String COLUMNS = "id, name, email, password_hash, email_verified, access, status";

    @Select("SELECT " + COLUMNS + " FROM account WHERE name = #{name}")
    AccountDbo selectByName(String name);

    @Select("""
        <script>
        SELECT id, name, email, password_hash, email_verified, access, status
        FROM account
        WHERE name IN
        <foreach item='n' collection='names' open='(' separator=',' close=')'>#{n}</foreach>
        </script>
        """)
    List<AccountDbo> selectByNameSet(@Param("names") Collection<String> names);

    @Select("""
        SELECT id, name, email, password_hash, email_verified, access, status
        FROM account ORDER BY name LIMIT #{limit} OFFSET #{offset}
        """)
    List<AccountDbo> selectPage(@Param("limit") int limit, @Param("offset") int offset);

    @Select("""
        SELECT count(*) FROM (
            SELECT 1 FROM account ORDER BY name OFFSET #{skip} LIMIT #{window}
        ) t
        """)
    int countInWindow(@Param("skip") int skip, @Param("window") int window);

    @Select("SELECT " + COLUMNS + " FROM account WHERE email = #{email}")
    AccountDbo selectByEmail(String email);

    @Insert("""
        INSERT INTO account (name, email, password_hash, email_verified, access, status)
        VALUES (#{name}, #{email}, #{passwordHash}, #{emailVerified}, #{access}, #{status})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    void insert(AccountDbo dbo);

    @Update("""
        UPDATE account SET email = #{email}, password_hash = #{passwordHash},
               email_verified = #{emailVerified}, access = #{access}, status = #{status}
        WHERE name = #{name}
        """)
    int updateByName(AccountDbo dbo);
}
