package com.naturalist.account;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.MockDomainService;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.observability.Level;
import com.naturalist.observability.MethodObserver;

import java.util.Optional;

/**
 * In-memory {@link AccountRepository.AccountEntityRepository} backed by
 * {@link AccountTestEntitySource}. Marked {@link MockDomainService} so the Spring runtime
 * bridge registers it under the {@code mock-data} profile (ADR-025).
 */
@MockDomainService
class AccountRepositoryMock
        extends AbstractTestEntityRepository<AccountName, Account, AccountTestEntitySource>
        implements AccountRepository.AccountEntityRepository {

    AccountRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public Optional<Account> getByEmail(String email) {
        MethodObserver getByEmail = observer().forMethod("getByEmail");
        getByEmail.arguments(i -> i.email(email, "email"))
                .throwWhenInvalid();
        var result = testEntitySource().entityStream()
                .filter(account -> email.equals(account.email()))
                .findFirst();
        getByEmail.state(s -> s.namedEntityOrNull(result.orElse(null), "result")).observe(Level.WARN);
        return result;
    }
}
