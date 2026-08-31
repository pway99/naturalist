package com.naturalist.account;

import com.naturalist.ddd.BehavioralCollection;

import java.util.Collection;
import java.util.List;

/**
 * Namespace for the accounts sub-context's {@link BehavioralCollection} return types
 * (ADR-020). One collection today; nested here so a second joins without a new file.
 */
public interface AccountEntityCollections {

    final class AccountCollection extends BehavioralCollection<Account> {

        AccountCollection(Collection<Account> accounts) {
            super(accounts);
        }

        public static AccountCollection of(Collection<Account> accounts) {
            return new AccountCollection(accounts);
        }

        public static AccountCollection empty() {
            return new AccountCollection(List.of());
        }
    }
}
