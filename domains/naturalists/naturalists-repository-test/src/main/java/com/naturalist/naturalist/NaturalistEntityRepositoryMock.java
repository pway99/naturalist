package com.naturalist.naturalist;

import com.naturalist.account.AccountName;
import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.MockDomainService;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.observability.Level;
import com.naturalist.observability.MethodObserver;

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
        MethodObserver getByAccount = observer().forMethod("getByAccount");
        getByAccount.arguments(i -> i.entityName(account, "account"))
                .throwWhenInvalid();
        var result = testEntitySource().entityStream()
                .filter(naturalist -> account.equals(naturalist.account()))
                .findFirst();
        getByAccount.state(s -> s.namedEntityOrNull(result.orElse(null), "result")).observe(Level.WARN);
        return result;
    }
}
