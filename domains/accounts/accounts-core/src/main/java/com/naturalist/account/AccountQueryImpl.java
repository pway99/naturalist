package com.naturalist.account;

import com.naturalist.account.AccountEntityCollections.AccountCollection;
import com.naturalist.data.AbstractEntityQuery;
import com.naturalist.infrastructure.DomainService;

import java.util.Optional;
import java.util.Set;

/**
 * Thin adapter for {@link AccountQuery} — observe, dispatch, delegate (ADR-010).
 * {@code getByName} is inherited from {@link AbstractEntityQuery}. Marked
 * {@link DomainService} for the Spring runtime bridge (ADR-025).
 */
@DomainService
class AccountQueryImpl
        extends AbstractEntityQuery<AccountName, Account, AccountCollection, AccountRepository.AccountEntityRepository>
        implements AccountQuery {

    AccountQueryImpl(AccountRepository.AccountEntityRepository repository) {
        super(repository);
    }

    @Override
    public AccountCollection findByNameSet(Set<AccountName> names) {
        observer().arguments("findByNameSet", i -> i.identifierSet(names, "names"))
                .throwWhenInvalid();
        return AccountCollection.of(repository().getByEntityNameSet(names));
    }

    @Override
    public Optional<Account> getByEmail(String email) {
        observer().arguments("getByEmail", i -> i.email(email, "email"))
                .throwWhenInvalid();
        return repository().getByEmail(email);
    }
}
