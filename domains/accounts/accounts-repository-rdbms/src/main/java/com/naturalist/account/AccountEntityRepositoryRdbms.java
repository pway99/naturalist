package com.naturalist.account;

import com.naturalist.data.AbstractEntityRepository;
import com.naturalist.data.Page;
import com.naturalist.data.PageRequest;
import com.naturalist.exception.EntityNotFoundException;
import com.naturalist.exception.PrimaryKeyConstraintException;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.observability.Observer;
import com.naturalist.persistence.RdbmsExceptions;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@DomainService
class AccountEntityRepositoryRdbms
        extends AbstractEntityRepository<AccountName, Account>
        implements AccountRepository.AccountEntityRepository {

    private final AccountMapper mapper;

    AccountEntityRepositoryRdbms(AccountMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected Optional<Account> doGetByName(AccountName name) {
        AccountDbo dbo = mapper.selectByName(name.value());
        return Optional.ofNullable(dbo).map(AccountDbo::toEntity);
    }

    @Override
    protected List<Account> doGetByNameSet(Set<AccountName> nameSet) {
        if (nameSet.isEmpty()) return List.of();
        List<String> slugs = nameSet.stream().map(AccountName::value).toList();
        return mapper.selectByNameSet(slugs).stream().map(AccountDbo::toEntity).toList();
    }

    @Override
    protected Page<Account> doGetPage(PageRequest request) {
        int pageSize = request.pageSize();
        List<AccountDbo> rows = mapper.selectPage(pageSize + 1, request.offset());
        boolean immediateMore = rows.size() > pageSize;
        List<Account> content = rows.stream().limit(pageSize).map(AccountDbo::toEntity).toList();

        int pagesAheadKnown = 0;
        boolean moreBeyondLookahead = immediateMore;
        if (request.lookahead() > 0 && immediateMore) {
            int window = request.lookahead() * pageSize + 1;
            int beyond = mapper.countInWindow(request.offset() + pageSize, window);
            pagesAheadKnown = Math.min(request.lookahead(), (beyond + pageSize - 1) / pageSize);
            moreBeyondLookahead = beyond > request.lookahead() * pageSize;
        } else if (request.lookahead() > 0) {
            moreBeyondLookahead = false;
        }
        return new Page<>(content, request.pageNumber(), pageSize, pagesAheadKnown, moreBeyondLookahead);
    }

    @Override
    protected void doInsert(Account entity) {
        AccountDbo dbo = AccountDbo.from(entity);   // from(...) validates the DBO and throws if invalid
        try {
            mapper.insert(dbo);
        } catch (RuntimeException e) {
            if (RdbmsExceptions.isUniqueViolation(e)) {
                throw new PrimaryKeyConstraintException(entity);
            }
            throw e;
        }
    }

    @Override
    protected void doUpdate(Account entity) {
        AccountDbo dbo = AccountDbo.from(entity);   // from(...) validates the DBO and throws if invalid
        if (mapper.updateByName(dbo) == 0) {
            throw new EntityNotFoundException(entity);
        }
    }

    @Override
    protected Account doSave(Account entity) {
        if (mapper.selectByName(entity.name().value()) != null) {
            doUpdate(entity);
        } else {
            doInsert(entity);
        }
        return entity;
    }

    @Override
    public Optional<Account> getByEmail(String email) {
        Observer.forClass(AccountEntityRepositoryRdbms.class)
                .arguments("getByEmail", i -> i.email(email, "email"))
                .throwWhenInvalid();
        return Optional.ofNullable(mapper.selectByEmail(email)).map(AccountDbo::toEntity);
    }
}
