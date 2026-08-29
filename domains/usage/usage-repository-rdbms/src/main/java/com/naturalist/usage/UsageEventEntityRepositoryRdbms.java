package com.naturalist.usage;

import com.naturalist.data.AbstractEntityRepository;
import com.naturalist.data.Page;
import com.naturalist.data.PageRequest;
import com.naturalist.exception.EntityNotFoundException;
import com.naturalist.exception.PrimaryKeyConstraintException;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.naturalist.NaturalistName;
import com.naturalist.observability.Observer;
import com.naturalist.persistence.RdbmsExceptions;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * RDBMS adapter for {@link UsageEvent} — an append-only, flat surrogate-UUID entity. {@code findByCounterSince}
 * scopes to one activity slug at or after an inclusive instant; a null naturalist returns all in-window events.
 */
@DomainService
class UsageEventEntityRepositoryRdbms
        extends AbstractEntityRepository<UsageEventId, UsageEvent>
        implements UsageRepository.EventRepository {

    private final UsageEventMapper mapper;

    UsageEventEntityRepositoryRdbms(UsageEventMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected Optional<UsageEvent> doGetByName(UsageEventId id) {
        return Optional.ofNullable(mapper.selectById(id.value().toString())).map(UsageEventDbo::toEntity);
    }

    @Override
    protected List<UsageEvent> doGetByNameSet(Set<UsageEventId> nameSet) {
        if (nameSet.isEmpty()) return List.of();
        List<String> ids = nameSet.stream().map(id -> id.value().toString()).toList();
        return mapper.selectByIdSet(ids).stream().map(UsageEventDbo::toEntity).toList();
    }

    @Override
    protected Page<UsageEvent> doGetPage(PageRequest request) {
        int pageSize = request.pageSize();
        List<UsageEventDbo> rows = mapper.selectPage(pageSize + 1, request.offset());
        boolean immediateMore = rows.size() > pageSize;
        List<UsageEvent> content = rows.stream().limit(pageSize).map(UsageEventDbo::toEntity).toList();

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
    protected void doInsert(UsageEvent entity) {
        UsageEventDbo dbo = UsageEventDbo.from(entity);
        try {
            mapper.insert(dbo);
        } catch (RuntimeException e) {
            if (RdbmsExceptions.isUniqueViolation(e)) throw new PrimaryKeyConstraintException(entity);
            throw e;
        }
    }

    @Override
    protected void doUpdate(UsageEvent entity) {
        UsageEventDbo dbo = UsageEventDbo.from(entity);
        if (mapper.updateById(dbo) == 0) throw new EntityNotFoundException(entity);
    }

    @Override
    protected UsageEvent doSave(UsageEvent entity) {
        if (mapper.selectById(entity.id().value().toString()) != null) doUpdate(entity);
        else doInsert(entity);
        return entity;
    }

    @Override
    public List<UsageEvent> findByCounterSince(UsageCounterName counter, @Nullable NaturalistName naturalist, Instant since) {
        Observer.forClass(UsageEventEntityRepositoryRdbms.class)
                .arguments("findByCounterSince", i -> i.identifier(counter, "counter").notNull(since, "since"))
                .throwWhenInvalid();
        return mapper.findByCounterSince(counter.value(), naturalist == null ? null : naturalist.value(), since)
                .stream().map(UsageEventDbo::toEntity).toList();
    }
}
