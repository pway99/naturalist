package com.naturalist.console.account;

import com.naturalist.account.AccountName;
import com.naturalist.account.AccountQuery;
import com.naturalist.naturalist.NaturalistQuery;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The purge composed at the app root, over the in-memory doubles. The seeded
 * {@code acct-018f3a2ef533} is the one unverified fixture; its only verification token expired on
 * 2026-09-30, so against the wall clock it has lapsed, and its linked naturalist must go with it.
 * {@link DirtiesContext}: the purge deletes seeded rows from the shared in-memory database, which
 * other test classes reuse with a cached context.
 */
@SpringBootTest
@DirtiesContext
class UnverifiedAccountPurgeTransactionTest {

    private static final AccountName LAPSED = AccountName.of("acct-018f3a2ef533");
    private static final AccountName VERIFIED = AccountName.of("acct-018f3a2e9b71");

    @Autowired
    UnverifiedAccountPurgeTransaction transaction;
    @Autowired
    AccountQuery accountQuery;
    @Autowired
    NaturalistQuery naturalistQuery;

    @Test
    void purge_deletesLapsedAccountAndItsNaturalist_andLeavesVerifiedAccounts() {
        assertThat(naturalistQuery.byAccount(LAPSED)).isPresent();

        assertThat(transaction.purge()).isEqualTo(1);

        assertThat(accountQuery.getByName(LAPSED)).isEmpty();
        assertThat(naturalistQuery.byAccount(LAPSED)).isEmpty();
        assertThat(accountQuery.getByName(VERIFIED)).isPresent();
        assertThat(naturalistQuery.byAccount(VERIFIED)).isPresent();
        // Nothing left to purge on the next run.
        assertThat(transaction.purge()).isZero();
    }
}
