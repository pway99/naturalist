package com.naturalist.account;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.MockDomainService;
import com.naturalist.data.NaturalistDatabase;

import java.util.Optional;

/**
 * In-memory {@link AccountRepository} backed by {@link AccountTestEntitySource}. Marked
 * {@link MockDomainService} so the Spring runtime bridge registers it under the
 * {@code mock-data} profile (ADR-025). Mirrors {@code NaturalistEntityRepositoryMock}.
 */
@MockDomainService
class AccountRepositoryMock
        extends AbstractTestEntityRepository<AccountName, Account, AccountTestEntitySource>
        implements AccountRepository {

    AccountRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public Optional<Account> getByEmail(String email) {
        observer().arguments("getByEmail", i -> i.notBlank(email, "email"))
                .throwWhenInvalid();
        return testEntitySource().entityStream()
                .filter(account -> email.equals(account.email()))
                .findFirst();
    }
}
