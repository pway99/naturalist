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
 * RDBMS adapter for {@link UsageAlert} — a flat surrogate-UUID entity. {@code findDedupKey} looks up the
 * single alert for a {@code (counter, scope, kind, period)} tuple; {@code getUnacknowledged} / {@code getUnsent}
 * return the operator work queues newest-first.
 */
@DomainService
class UsageAlertEntityRepositoryRdbms
        extends AbstractEntityRepository<UsageAlertId, UsageAlert>
        implements UsageRepository.AlertRepository {

    private final UsageAlertMapper mapper;

    UsageAlertEntityRepositoryRdbms(UsageAlertMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected Optional<UsageAlert> doGetByName(UsageAlertId id) {
        return Optional.ofNullable(mapper.selectById(id.value().toString())).map(UsageAlertDbo::toEntity);
    }

    @Override
    protected List<UsageAlert> doGetByNameSet(Set<UsageAlertId> nameSet) {
        if (nameSet.isEmpty()) return List.of();
        List<String> ids = nameSet.stream().map(id -> id.value().toString()).toList();
        return mapper.selectByIdSet(ids).stream().map(UsageAlertDbo::toEntity).toList();
    }

    @Override
    protected Page<UsageAlert> doGetPage(PageRequest request) {
        int pageSize = request.pageSize();
        List<UsageAlertDbo> rows = mapper.selectPage(pageSize + 1, request.offset());
        boolean immediateMore = rows.size() > pageSize;
        List<UsageAlert> content = rows.stream().limit(pageSize).map(UsageAlertDbo::toEntity).toList();

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
    protected void doInsert(UsageAlert entity) {
        UsageAlertDbo dbo = UsageAlertDbo.from(entity);
        try {
            mapper.insert(dbo);
        } catch (RuntimeException e) {
            if (RdbmsExceptions.isUniqueViolation(e)) throw new PrimaryKeyConstraintException(entity);
            throw e;
        }
    }

    @Override
    protected void doUpdate(UsageAlert entity) {
        UsageAlertDbo dbo = UsageAlertDbo.from(entity);
        if (mapper.updateById(dbo) == 0) throw new EntityNotFoundException(entity);
    }

    @Override
    protected UsageAlert doSave(UsageAlert entity) {
        if (mapper.selectById(entity.id().value().toString()) != null) doUpdate(entity);
        else doInsert(entity);
        return entity;
    }

    @Override
    public Optional<UsageAlert> findDedupKey(UsageCounterName counter, AlertScope scope, AlertKind kind, String period) {
        Observer.forClass(UsageAlertEntityRepositoryRdbms.class)
                .arguments("findDedupKey", i -> i
                        .identifier(counter, "counter")
                        .notNull(scope, "scope")
                        .notNull(kind, "kind")
                        .notBlank(period, "period"))
                .throwWhenInvalid();
        return Optional.ofNullable(mapper.selectDedupKey(counter.value(), scope.name(), kind.name(), period))
                .map(UsageAlertDbo::toEntity);
    }

    @Override
    public List<UsageAlert> getUnacknowledged() {
        return mapper.selectUnacknowledged().stream().map(UsageAlertDbo::toEntity).toList();
    }

    @Override
    public List<UsageAlert> getUnsent() {
        return mapper.selectUnsent().stream().map(UsageAlertDbo::toEntity).toList();
    }
}
