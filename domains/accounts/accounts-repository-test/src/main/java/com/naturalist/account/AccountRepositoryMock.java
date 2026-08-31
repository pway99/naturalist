package com.naturalist.account;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.MockDomainService;
import com.naturalist.data.NaturalistDatabase;

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
}
