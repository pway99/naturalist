package com.naturalist.naturalist;

import com.naturalist.account.AccountName;
import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.MockDomainService;
import com.naturalist.data.NaturalistDatabase;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@MockDomainService
class NaturalistEntityRepositoryMock
        extends AbstractTestEntityRepository<NaturalistName, Naturalist, NaturalistTestEntitySource>
        implements NaturalistRepository.NaturalistEntityRepository {

    NaturalistEntityRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public Optional<Naturalist> getByAccount(AccountName account) {
        observer().arguments("getByAccount", i -> i.entityName(account, "account"))
                .throwWhenInvalid();
        return testEntitySource().entityStream()
                .filter(naturalist -> account.equals(naturalist.account()))
                .findFirst();
    }

    @Override
    public int deleteByAccounts(Set<AccountName> accounts) {
        observer().arguments("deleteByAccounts", i -> i.entityNameCollection(accounts, "accounts"))
                .throwWhenInvalid();
        Set<NaturalistName> linked = testEntitySource().entityStream()
                .filter(naturalist -> accounts.contains(naturalist.account()))
                .map(Naturalist::name)
                .collect(Collectors.toUnmodifiableSet());
        return testEntitySource().delete(linked);
    }
}
