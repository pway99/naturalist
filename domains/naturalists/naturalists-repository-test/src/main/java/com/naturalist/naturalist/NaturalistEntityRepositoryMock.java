package com.naturalist.naturalist;

import com.naturalist.account.AccountName;
import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.MockDomainService;
import com.naturalist.data.NaturalistDatabase;

import java.util.Optional;

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
}
