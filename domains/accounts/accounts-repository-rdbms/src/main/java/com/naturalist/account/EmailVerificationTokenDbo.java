package com.naturalist.account;

import com.naturalist.observability.Constraints;
import com.naturalist.observability.Observer;
import com.naturalist.persistence.Dbo;
import com.naturalist.persistence.DboSchema;
import com.naturalist.persistence.Fk;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Consumer;
// id is unwrapped to its String form (like EntityName -> String): MyBatis has no UUID type
// handler, and the ACL forbids adding one, so the mapper casts the text to uuid in SQL (::uuid).

/**
 * Persistence view of {@link EmailVerificationToken} — a surrogate-UUID {@code Entity} whose
 * reference to the owning account is the DB-enforced numeric {@code account_id}; the DBO
 * carries the account's {@code name}, which the mapper JOINs to project on read and
 * nested-selects {@code account.id} from on write — so no name is persisted on this row and
 * nothing can drift.
 */
@DboSchema(table = "email_verification_token", primaryKey = "id",
           foreignKeys = @Fk(columns = "account_id", references = "account(id)"),
           entity = EmailVerificationToken.class)
final class EmailVerificationTokenDbo implements Dbo {
    String id;             // the EmailVerificationTokenId's UUID as text; the mapper casts it to uuid (::uuid)
    String account;        // JOIN projection (read) / nested-select key (write); not a stored column
    String tokenHash;
    String purpose;
    Instant expiresAt;
    @Nullable Instant consumedAt;

    static EmailVerificationTokenDbo from(EmailVerificationToken t) {
        EmailVerificationTokenDbo d = new EmailVerificationTokenDbo();
        d.id = t.id().value().toString();
        d.account = t.account().value();
        d.tokenHash = t.tokenHash();
        d.purpose = t.purpose().name();
        d.expiresAt = t.expiresAt();
        d.consumedAt = t.consumedAt();
        Observer.forClass(EmailVerificationTokenDbo.class)
                .arguments("from", i -> i.observable(d, "dbo"))
                .throwWhenInvalid();
        return d;
    }

    EmailVerificationToken toEntity() {
        return new EmailVerificationToken(
                EmailVerificationTokenId.of(UUID.fromString(id)),
                AccountName.of(account),
                tokenHash,
                TokenPurpose.valueOf(purpose),
                expiresAt,
                consumedAt);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return c -> c
                .notBlank(id, "id")
                .notNull(account, "account").kebabFormat(account, "account")
                .notBlank(tokenHash, "tokenHash").maxLength(tokenHash, 128, "tokenHash")
                .notNull(purpose, "purpose").maxLength(purpose, 32, "purpose")
                .notNull(expiresAt, "expiresAt");
    }
}
