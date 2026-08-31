package com.naturalist.account;

import com.naturalist.RandomValue;
import com.naturalist.data.EntityRepositoryTest;
import com.naturalist.data.TestEntitySource;
import com.naturalist.exception.InvariantViolationException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Behavioral contract for {@link AccountRepository.VerificationTokenRepository}. Inherits
 * the {@link EntityRepositoryTest} cases (ADR-002) and supplies the identity constants and
 * entity construction specific to {@link EmailVerificationToken}. Known ids match the
 * fixtures in {@code account/email-verification-tokens.json}; constructed entities reference
 * existing account fixtures so the {@code account} foreign key resolves.
 */
interface EmailVerificationTokenRepositoryTest
        extends EntityRepositoryTest<EmailVerificationTokenId, EmailVerificationToken> {

    EmailVerificationTokenId TOKEN_ONE =
            EmailVerificationTokenId.of(UUID.fromString("018f3a2e-9b71-7a01-8b01-000000000001"));
    EmailVerificationTokenId TOKEN_TWO =
            EmailVerificationTokenId.of(UUID.fromString("018f3a2e-9b71-7a01-8b01-000000000002"));
    EmailVerificationTokenId NOT_FOUND =
            EmailVerificationTokenId.of(UUID.fromString("018f3a2e-9b71-7a01-8b01-0000000000ff"));

    @Override
    AccountRepository.VerificationTokenRepository repository();

    @Override
    default TestEntitySource<EmailVerificationTokenId, EmailVerificationToken> source() {
        return db.getNamed(EmailVerificationTokenTestEntitySource.class);
    }

    @Override
    default EmailVerificationTokenId notFoundName() {
        return NOT_FOUND;
    }

    @Override
    default List<EmailVerificationTokenId> knownEntityNames() {
        return List.of(TOKEN_ONE, TOKEN_TWO);
    }

    @Override
    default EmailVerificationToken newEntity() {
        return new EmailVerificationToken(
                EmailVerificationTokenId.create(),
                AccountName.of("acct-018f3a2e9b71"),
                "sha256:" + RandomValue.string(),
                TokenPurpose.VERIFY_EMAIL,
                Instant.parse("2026-12-01T00:00:00Z"),
                null);
    }

    @Override
    default EmailVerificationToken ghostEntity() {
        return new EmailVerificationToken(
                EmailVerificationTokenId.create(),
                AccountName.of("acct-018f3a2e9b71"),
                "sha256:" + RandomValue.string(),
                TokenPurpose.VERIFY_EMAIL,
                Instant.parse("2026-12-01T00:00:00Z"),
                null);
    }

    @Override
    default EmailVerificationToken modifiedEntity(EmailVerificationToken original) {
        return new EmailVerificationToken(
                original.id(),
                AccountName.of("acct-018f3a2ec4d2"),
                "sha256:changed-" + RandomValue.string(),
                TokenPurpose.RESET_PASSWORD,
                Instant.parse("2026-12-31T00:00:00Z"),
                Instant.parse("2026-12-15T00:00:00Z"));
    }

    @Test
    default void getByTokenHash_rejectsNull() {
        assertThatThrownBy(() -> repository().getByTokenHash(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("tokenHash");
    }

    @Test
    default void getByTokenHash_returnsMatchingToken() {
        var result = repository().getByTokenHash("sha256:1a2b3c4d5e6f70819a2b3c4d5e6f7081");

        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(TOKEN_ONE);
    }

    @Test
    default void getByTokenHash_returnsEmptyForUnknownHash() {
        assertThat(repository().getByTokenHash("sha256:does-not-exist")).isEmpty();
    }
}
