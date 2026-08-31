package com.naturalist.account;

import com.naturalist.account.AccountEntityCollections.AccountCollection;
import com.naturalist.data.EntityQuery;
import com.naturalist.data.EntityQueryContractTest;
import com.naturalist.data.NaturalistTestExtension;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;

/**
 * Behavioral contract for {@link AccountQueryImpl} (ADR-002 cases for {@code getByName}
 * and {@code findByNameSet}). Known constants match the {@code account/accounts.json}
 * fixtures.
 */
class AccountQueryImplTest
        implements EntityQueryContractTest<AccountName, Account, AccountCollection> {

    @RegisterExtension
    NaturalistTestExtension nte = NaturalistTestExtension.create();

    AccountRepositoryMock repository = new AccountRepositoryMock(nte);
    AccountQuery query = new AccountQueryImpl(repository);

    @Override
    public EntityQuery<AccountName, Account, AccountCollection> query() {
        return query;
    }

    @Override
    public AccountName notFoundName() {
        return AccountName.of("acct-nobody-here");
    }

    @Override
    public List<AccountName> knownEntityNames() {
        return List.of(
                AccountName.of("acct-018f3a2e9b71"),
                AccountName.of("acct-018f3a2ec4d2"));
    }
}
