package com.naturalist.account;

import com.naturalist.account.AccountEntityCollections.AccountCollection;
import com.naturalist.data.EntityQuery;
import com.naturalist.data.EntityQueryContractTest;
import com.naturalist.data.NaturalistTestExtension;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.test.context.Wired;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Behavioral contract for {@link AccountQueryImpl}, built by the test graph — the inherited
 * {@link EntityQueryContractTest} cases for {@code getByName} / {@code findByNameSet},
 * plus the {@code getByEmail} finder. Known constants match the {@code account/accounts.json}
 * fixtures.
 */
class AccountQueryImplTest
        implements EntityQueryContractTest<AccountName, Account, AccountCollection> {

    @RegisterExtension
    NaturalistTestExtension nte = NaturalistTestExtension.create();

    @Wired
    AccountQuery query;

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

    @Test
    void getByEmail_rejectsNull() {
        assertThatThrownBy(() -> query.getByEmail(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("email");
    }

    @Test
    void getByEmail_returnsMatchingAccount() {
        var result = query.getByEmail("gerald.durrell@oakvista.example");

        assertThat(result).isPresent();
        assertThat(result.get().name()).isEqualTo(AccountName.of("acct-018f3a2e9b71"));
    }

    @Test
    void getByEmail_returnsEmptyForUnknown() {
        assertThat(query.getByEmail("nobody@nowhere.example")).isEmpty();
    }

    @Test
    void getByEmail_rejectsMalformedEmail() {
        assertThatThrownBy(() -> query.getByEmail("not-an-email"))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("email");
    }
}
