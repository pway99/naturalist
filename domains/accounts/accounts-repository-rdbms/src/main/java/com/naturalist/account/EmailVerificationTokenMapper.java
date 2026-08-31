package com.naturalist.account;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Collection;
import java.util.List;

/**
 * MyBatis mapper for {@code email_verification_token} — a surrogate-UUID entity. Reads JOIN
 * {@code account} to project the account {@code name} for reconstruction; writes nested-select
 * {@code account.id} from that name. The uuid {@code id} travels as text and is cast to uuid in
 * SQL ({@code ::uuid}) — no MyBatis type handler.
 */
@Mapper
interface EmailVerificationTokenMapper {

    String SELECT_JOIN = """
        SELECT t.id, a.name AS account, t.token_hash, t.purpose, t.expires_at, t.consumed_at
        FROM email_verification_token t JOIN account a ON a.id = t.account_id
        """;

    @Select(SELECT_JOIN + " WHERE t.id = #{id}::uuid")
    EmailVerificationTokenDbo selectById(String id);

    @Select("""
        <script>
        SELECT t.id, a.name AS account, t.token_hash, t.purpose, t.expires_at, t.consumed_at
        FROM email_verification_token t JOIN account a ON a.id = t.account_id
        WHERE t.id IN
        <foreach item='x' collection='ids' open='(' separator=',' close=')'>#{x}::uuid</foreach>
        </script>
        """)
    List<EmailVerificationTokenDbo> selectByIdSet(@Param("ids") Collection<String> ids);

    @Select(SELECT_JOIN + " ORDER BY t.id LIMIT #{limit} OFFSET #{offset}")
    List<EmailVerificationTokenDbo> selectPage(@Param("limit") int limit, @Param("offset") int offset);

    @Select("""
        SELECT count(*) FROM (
            SELECT 1 FROM email_verification_token ORDER BY id OFFSET #{skip} LIMIT #{window}
        ) t
        """)
    int countInWindow(@Param("skip") int skip, @Param("window") int window);

    @Select(SELECT_JOIN + " WHERE t.token_hash = #{tokenHash}")
    EmailVerificationTokenDbo selectByTokenHash(String tokenHash);

    @Insert("""
        INSERT INTO email_verification_token (id, account_id, token_hash, purpose, expires_at, consumed_at)
        SELECT #{id}::uuid, id, #{tokenHash}, #{purpose}, #{expiresAt}, #{consumedAt} FROM account WHERE name = #{account}
        """)
    int insert(EmailVerificationTokenDbo dbo);

    @Update("""
        UPDATE email_verification_token SET
            token_hash = #{tokenHash}, purpose = #{purpose},
            expires_at = #{expiresAt}, consumed_at = #{consumedAt},
            account_id = (SELECT id FROM account WHERE name = #{account})
        WHERE id = #{id}::uuid
        """)
    int updateById(EmailVerificationTokenDbo dbo);
}
