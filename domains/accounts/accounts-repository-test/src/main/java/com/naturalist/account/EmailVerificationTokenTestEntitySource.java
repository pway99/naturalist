package com.naturalist.account;

import com.naturalist.data.ForeignKeyConstraint;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.TestEntitySource;

import java.util.List;

/**
 * In-memory catalog of {@link EmailVerificationToken} fixtures. Keyed automatically on the
 * surrogate {@link EmailVerificationTokenId}. Declares the intra-domain foreign key
 * {@code account → Account} so a token whose owning account is absent fails at load rather
 * than at a later query — and so the mock exercises the same write-ordering Postgres
 * enforces (mock/rdbms FK parity).
 */
public class EmailVerificationTokenTestEntitySource
        extends TestEntitySource<EmailVerificationTokenId, EmailVerificationToken> {

    public EmailVerificationTokenTestEntitySource(NaturalistDatabase database) {
        super(database);
        loadFile("account/email-verification-tokens.json");
    }

    @Override
    protected List<ForeignKeyConstraint<EmailVerificationToken, ?>> foreignKeyConstraints() {
        return List.of(ForeignKeyConstraint.of(
                "account",
                EmailVerificationToken::account,
                AccountTestEntitySource.class));
    }
}
