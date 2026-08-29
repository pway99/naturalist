package com.naturalist.usage;

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
 * RDBMS adapter for {@link UsageCounter} — a flat surrogate-UUID entity. {@code findByCounterName} returns
 * every rule (active or inactive) for one activity slug.
 */
@DomainService
class UsageCounterEntityRepositoryRdbms
        extends AbstractEntityRepository<UsageCounterId, UsageCounter>
        implements UsageRepository.CounterRepository {

    private final UsageCounterMapper mapper;

    UsageCounterEntityRepositoryRdbms(UsageCounterMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected Optional<UsageCounter> doGetByName(UsageCounterId id) {
        return Optional.ofNullable(mapper.selectById(id.value().toString())).map(UsageCounterDbo::toEntity);
    }

    @Override
    protected List<UsageCounter> doGetByNameSet(Set<UsageCounterId> nameSet) {
        if (nameSet.isEmpty()) return List.of();
        List<String> ids = nameSet.stream().map(id -> id.value().toString()).toList();
        return mapper.selectByIdSet(ids).stream().map(UsageCounterDbo::toEntity).toList();
    }

    @Override
    protected Page<UsageCounter> doGetPage(PageRequest request) {
        int pageSize = request.pageSize();
        List<UsageCounterDbo> rows = mapper.selectPage(pageSize + 1, request.offset());
        boolean immediateMore = rows.size() > pageSize;
        List<UsageCounter> content = rows.stream().limit(pageSize).map(UsageCounterDbo::toEntity).toList();

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
    protected void doInsert(UsageCounter entity) {
        UsageCounterDbo dbo = UsageCounterDbo.from(entity);
        try {
            mapper.insert(dbo);
        } catch (RuntimeException e) {
            if (RdbmsExceptions.isUniqueViolation(e)) throw new PrimaryKeyConstraintException(entity);
            throw e;
        }
    }

    @Override
    protected void doUpdate(UsageCounter entity) {
        UsageCounterDbo dbo = UsageCounterDbo.from(entity);
        if (mapper.updateById(dbo) == 0) throw new EntityNotFoundException(entity);
    }

    @Override
    protected UsageCounter doSave(UsageCounter entity) {
        if (mapper.selectById(entity.id().value().toString()) != null) doUpdate(entity);
        else doInsert(entity);
        return entity;
    }

    @Override
    public List<UsageCounter> findByCounterName(UsageCounterName counterName) {
        Observer.forClass(UsageCounterEntityRepositoryRdbms.class)
                .arguments("findByCounterName", i -> i.identifier(counterName, "counterName"))
                .throwWhenInvalid();
        return mapper.selectByCounterName(counterName.value()).stream().map(UsageCounterDbo::toEntity).toList();
    }
}
