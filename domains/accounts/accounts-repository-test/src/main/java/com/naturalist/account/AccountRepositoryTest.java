package com.naturalist.account;

import com.naturalist.RandomValue;
import com.naturalist.data.EntityRepositoryTest;
import com.naturalist.data.Page;
import com.naturalist.data.PageRequest;
import com.naturalist.data.TestEntitySource;
import com.naturalist.exception.InvariantViolationException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Behavioral contract for {@link AccountRepository}. Inherits the
 * {@link EntityRepositoryTest} cases (ADR-002) and supplies the identity hooks and
 * entity-construction helpers specific to {@link Account}, plus the {@code getByEmail}
 * finder cases and the lapsed-registration purge cases. The known constants match the fixtures in
 * {@code account/accounts.json}; {@code YOUNG} is the one unverified fixture, and its only
 * verification token ({@code email-verification-tokens.json}) expires at {@code YOUNG_TOKEN_EXPIRES}.
 */
interface AccountRepositoryTest extends EntityRepositoryTest<AccountName, Account> {

    AccountName DURRELL = AccountName.of("acct-018f3a2e9b71");
    AccountName OWENS = AccountName.of("acct-018f3a2ec4d2");
    AccountName NOT_FOUND = AccountName.of("acct-nobody-here");
    AccountName YOUNG = AccountName.of("acct-018f3a2ef533");
    /** Verified but suspended, holding an unconsumed, expired verification token. */
    AccountName BANNED = AccountName.of("acct-018f3b01d8e5");
    Instant YOUNG_TOKEN_EXPIRES = Instant.parse("2026-09-30T00:00:00Z");

    @Override
    AccountRepository.AccountEntityRepository repository();

    /** The same adapter family's token repository: purging deletes tokens before their account. */
    AccountRepository.VerificationTokenRepository tokenRepository();

    @Override
    default TestEntitySource<AccountName, Account> source() {
        return db.getNamed(AccountTestEntitySource.class);
    }

    @Override
    default AccountName notFoundName() {
        return NOT_FOUND;
    }

    @Override
    default List<AccountName> knownEntityNames() {
        return List.of(DURRELL, OWENS);
    }

    @Override
    default Account newEntity() {
        return new Account(
                AccountName.of(RandomValue.string()),
                RandomValue.string() + "@example.org",
                "{bcrypt}$2a$10$" + RandomValue.string(),
                false,
                AccessLevel.BROWSE_ONLY,
                AccountStatus.ACTIVE);
    }

    @Override
    default Account ghostEntity() {
        return new Account(
                AccountName.of(RandomValue.string()),
                RandomValue.string() + "@example.org",
                "{bcrypt}$2a$10$" + RandomValue.string(),
                false,
                AccessLevel.BROWSE_ONLY,
                AccountStatus.ACTIVE);
    }

    @Override
    default Account modifiedEntity(Account original) {
        return new Account(
                original.name(),
                "changed-" + RandomValue.string() + "@example.org",
                "{bcrypt}$2a$10$changed" + RandomValue.string(),
                true,
                AccessLevel.VISION,
                AccountStatus.SUSPENDED);
    }

    @Test
    default void getByEmail_rejectsNull() {
        assertThatThrownBy(() -> repository().getByEmail(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("email");
    }

    @Test
    default void getByEmail_returnsAccountWithMatchingEmail() {
        Optional<Account> result = repository().getByEmail("gerald.durrell@oakvista.example");

        assertThat(result).isPresent();
        assertThat(result.get().name()).isEqualTo(DURRELL);
    }

    @Test
    default void getByEmail_returnsEmptyForUnknownEmail() {
        assertThat(repository().getByEmail("nobody@nowhere.example")).isEmpty();
    }

    @Test
    default void getByEmail_rejectsMalformedEmail() {
        assertThatThrownBy(() -> repository().getByEmail("not-an-email"))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("email");
    }

    @Test
    default void getLapsedRegistrations_rejectsNull() {
        assertThatThrownBy(() -> repository().getLapsedRegistrations(null, PageRequest.first(10)))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("now");
        assertThatThrownBy(() -> repository().getLapsedRegistrations(Instant.now(), null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("pageRequest");
    }

    @Test
    default void getLapsedRegistrations_returnsUnverifiedAccountOnceItsTokenExpired() {
        Page<Account> lapsed = repository().getLapsedRegistrations(
                YOUNG_TOKEN_EXPIRES.plusSeconds(1), PageRequest.first(10));

        assertThat(lapsed.content()).extracting(Account::name).containsExactly(YOUNG);
    }

    @Test
    default void getLapsedRegistrations_skipsAccountWhoseTokenIsStillLive() {
        Page<Account> lapsed = repository().getLapsedRegistrations(YOUNG_TOKEN_EXPIRES, PageRequest.first(10));

        assertThat(lapsed.content()).isEmpty();
    }

    @Test
    default void getLapsedRegistrations_skipsUnverifiedAccountWithNoVerificationToken() {
        Account tokenless = newEntity();
        repository().insert(tokenless);

        Page<Account> lapsed = repository().getLapsedRegistrations(
                YOUNG_TOKEN_EXPIRES.plusSeconds(1), PageRequest.first(10));

        assertThat(lapsed.content()).extracting(Account::name).doesNotContain(tokenless.name());
    }

    @Test
    default void getLapsedRegistrations_neverReturnsVerifiedAccount() {
        // BANNED is verified and holds an expired, unconsumed verification token.
        Page<Account> lapsed = repository().getLapsedRegistrations(
                Instant.parse("2030-01-01T00:00:00Z"), PageRequest.first(10));

        assertThat(lapsed.content()).extracting(Account::emailVerified).containsOnly(false);
        assertThat(lapsed.content()).extracting(Account::name).doesNotContain(BANNED, DURRELL, OWENS);
    }

    @Test
    default void deleteUnverified_rejectsNull() {
        assertThatThrownBy(() -> repository().deleteUnverified(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("names");
    }

    @Test
    default void deleteUnverified_deletesUnverifiedAccountAfterItsTokens() {
        tokenRepository().deleteForUnverifiedAccounts(Set.of(YOUNG));

        Set<AccountName> deleted = repository().deleteUnverified(Set.of(YOUNG));

        assertThat(deleted).containsExactly(YOUNG);
        assertThat(repository().getByName(YOUNG)).isEmpty();
    }

    @Test
    default void deleteUnverified_neverDeletesVerifiedAccount() {
        tokenRepository().deleteForUnverifiedAccounts(Set.of(DURRELL));

        Set<AccountName> deleted = repository().deleteUnverified(Set.of(DURRELL, NOT_FOUND));

        assertThat(deleted).isEmpty();
        assertThat(repository().getByName(DURRELL)).isPresent();
    }

    @Test
    default void deleteUnverified_refusesWhileTokensStillReferenceTheAccount() {
        assertThatThrownBy(() -> repository().deleteUnverified(Set.of(YOUNG)))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    default void deleteUnverified_emptySetDeletesNothing() {
        assertThat(repository().deleteUnverified(Set.of())).isEmpty();
    }
}
