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

/**
 * RDBMS adapter for {@link EmailVerificationToken} — a surrogate-UUID entity keyed by
 * {@link EmailVerificationTokenId}. Its account reference is stored as the numeric
 * {@code account_id}, resolved from the account name by the mapper's nested-select on write
 * and recovered by JOIN on read. A token for a missing account writes zero rows (the
 * nested-select yields none), which the adapter turns into a loud
 * {@link EntityNotFoundException}.
 */
@DomainService
class EmailVerificationTokenRepositoryRdbms
        extends AbstractEntityRepository<EmailVerificationTokenId, EmailVerificationToken>
        implements AccountRepository.VerificationTokenRepository {

    private final EmailVerificationTokenMapper mapper;

    EmailVerificationTokenRepositoryRdbms(EmailVerificationTokenMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected Optional<EmailVerificationToken> doGetByName(EmailVerificationTokenId id) {
        return Optional.ofNullable(mapper.selectById(id.value().toString())).map(EmailVerificationTokenDbo::toEntity);
    }

    @Override
    protected List<EmailVerificationToken> doGetByNameSet(Set<EmailVerificationTokenId> nameSet) {
        if (nameSet.isEmpty()) return List.of();
        List<String> ids = nameSet.stream().map(id -> id.value().toString()).toList();
        return mapper.selectByIdSet(ids).stream().map(EmailVerificationTokenDbo::toEntity).toList();
    }

    @Override
    protected Page<EmailVerificationToken> doGetPage(PageRequest request) {
        int pageSize = request.pageSize();
        List<EmailVerificationTokenDbo> rows = mapper.selectPage(pageSize + 1, request.offset());
        boolean immediateMore = rows.size() > pageSize;
        List<EmailVerificationToken> content =
                rows.stream().limit(pageSize).map(EmailVerificationTokenDbo::toEntity).toList();

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
    protected void doInsert(EmailVerificationToken entity) {
        EmailVerificationTokenDbo dbo = EmailVerificationTokenDbo.from(entity);   // validates + throws
        int inserted;
        try {
            inserted = mapper.insert(dbo);
        } catch (RuntimeException e) {
            if (RdbmsExceptions.isUniqueViolation(e)) throw new PrimaryKeyConstraintException(entity);
            throw e;
        }
        if (inserted == 0) throw new EntityNotFoundException(entity);   // referenced account absent
    }

    @Override
    protected void doUpdate(EmailVerificationToken entity) {
        EmailVerificationTokenDbo dbo = EmailVerificationTokenDbo.from(entity);
        if (mapper.updateById(dbo) == 0) throw new EntityNotFoundException(entity);
    }

    @Override
    protected EmailVerificationToken doSave(EmailVerificationToken entity) {
        if (mapper.selectById(entity.id().value().toString()) != null) doUpdate(entity);
        else doInsert(entity);
        return entity;
    }

    @Override
    public Optional<EmailVerificationToken> getByTokenHash(String tokenHash) {
        Observer.forClass(EmailVerificationTokenRepositoryRdbms.class)
                .arguments("getByTokenHash", i -> i.notBlank(tokenHash, "tokenHash"))
                .throwWhenInvalid();
        return Optional.ofNullable(mapper.selectByTokenHash(tokenHash)).map(EmailVerificationTokenDbo::toEntity);
    }
}
