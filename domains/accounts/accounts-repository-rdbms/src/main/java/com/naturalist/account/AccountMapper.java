package com.naturalist.account;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Options.FlushCachePolicy;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.Instant;
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

    /** A lapsed registration: unverified, BROWSE_ONLY, ACTIVE, with verify tokens and none live at {@code now}. */
    String LAPSED = """
            FROM account a
            WHERE a.email_verified = false AND a.access = 'BROWSE_ONLY' AND a.status = 'ACTIVE'
              AND EXISTS (SELECT 1 FROM email_verification_token t
                          WHERE t.account_id = a.id AND t.purpose = 'VERIFY_EMAIL')
              AND NOT EXISTS (SELECT 1 FROM email_verification_token t
                              WHERE t.account_id = a.id AND t.purpose = 'VERIFY_EMAIL'
                                AND t.expires_at >= #{now})
            """;

    @Select("SELECT a.id, a.name, a.email, a.password_hash, a.email_verified, a.access, a.status "
            + LAPSED + " ORDER BY a.name LIMIT #{limit} OFFSET #{offset}")
    List<AccountDbo> selectLapsedPage(@Param("now") Instant now, @Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT count(*) FROM (SELECT 1 " + LAPSED + " ORDER BY a.name OFFSET #{skip} LIMIT #{window}) w")
    int countLapsedInWindow(@Param("now") Instant now, @Param("skip") int skip, @Param("window") int window);

    /** Deletes the still-unverified accounts among {@code names}; {@code RETURNING} names the ones deleted. */
    @Select("""
            <script>
            DELETE FROM account
            WHERE email_verified = false AND name IN
            <foreach item='n' collection='names' open='(' separator=',' close=')'>#{n}</foreach>
            RETURNING name
            </script>
            """)
    @Options(flushCache = FlushCachePolicy.TRUE, useCache = false)
    List<String> deleteUnverified(@Param("names") Collection<String> names);
}
