package com.naturalist.account;

import com.naturalist.RandomValue;
import com.naturalist.data.EntityRepositoryTest;
import com.naturalist.data.TestEntitySource;
import com.naturalist.exception.InvariantViolationException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Behavioral contract for {@link AccountRepository}. Inherits the
 * {@link EntityRepositoryTest} cases (ADR-002) and supplies the identity hooks and
 * entity-construction helpers specific to {@link Account}, plus the {@code getByEmail}
 * finder cases. The known constants match the fixtures in {@code account/accounts.json}.
 */
interface AccountRepositoryTest extends EntityRepositoryTest<AccountName, Account> {

    AccountName DURRELL = AccountName.of("acct-018f3a2e9b71");
    AccountName OWENS = AccountName.of("acct-018f3a2ec4d2");
    AccountName NOT_FOUND = AccountName.of("acct-nobody-here");

    @Override
    AccountRepository repository();

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
        var result = repository().getByEmail("gerald.durrell@oakvista.example");

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
}
