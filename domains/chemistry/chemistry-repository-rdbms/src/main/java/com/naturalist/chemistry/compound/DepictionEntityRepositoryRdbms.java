package com.naturalist.chemistry.compound;

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
 * RDBMS adapter for {@link CompoundDepiction} — a surrogate-UUID entity keyed by
 * {@link DepictionId}. Its compound reference is stored as the numeric {@code compound_id},
 * resolved from the compound name by the mapper's nested-select on write and recovered by JOIN
 * on read. A depiction for a missing compound writes zero rows (the nested-select yields none),
 * which the adapter turns into a loud {@link EntityNotFoundException}.
 */
@DomainService
class DepictionEntityRepositoryRdbms
        extends AbstractEntityRepository<DepictionId, CompoundDepiction>
        implements CompoundRepository.DepictionRepository {

    private final DepictionMapper mapper;

    DepictionEntityRepositoryRdbms(DepictionMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected Optional<CompoundDepiction> doGetByName(DepictionId id) {
        return Optional.ofNullable(mapper.selectById(id.value().toString())).map(CompoundDepictionDbo::toEntity);
    }

    @Override
    protected List<CompoundDepiction> doGetByNameSet(Set<DepictionId> nameSet) {
        if (nameSet.isEmpty()) return List.of();
        List<String> ids = nameSet.stream().map(id -> id.value().toString()).toList();
        return mapper.selectByIdSet(ids).stream().map(CompoundDepictionDbo::toEntity).toList();
    }

    @Override
    protected Page<CompoundDepiction> doGetPage(PageRequest request) {
        int pageSize = request.pageSize();
        List<CompoundDepictionDbo> rows = mapper.selectPage(pageSize + 1, request.offset());
        boolean immediateMore = rows.size() > pageSize;
        List<CompoundDepiction> content = rows.stream().limit(pageSize).map(CompoundDepictionDbo::toEntity).toList();

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
    protected void doInsert(CompoundDepiction entity) {
        CompoundDepictionDbo dbo = CompoundDepictionDbo.from(entity);   // validates + throws
        int inserted;
        try {
            inserted = mapper.insert(dbo);
        } catch (RuntimeException e) {
            if (RdbmsExceptions.isUniqueViolation(e)) throw new PrimaryKeyConstraintException(entity);
            throw e;
        }
        if (inserted == 0) throw new EntityNotFoundException(entity);   // referenced compound absent
    }

    @Override
    protected void doUpdate(CompoundDepiction entity) {
        CompoundDepictionDbo dbo = CompoundDepictionDbo.from(entity);
        if (mapper.updateById(dbo) == 0) throw new EntityNotFoundException(entity);
    }

    @Override
    protected CompoundDepiction doSave(CompoundDepiction entity) {
        if (mapper.selectById(entity.id().value().toString()) != null) doUpdate(entity);
        else doInsert(entity);
        return entity;
    }

    @Override
    public Optional<CompoundDepiction> getByCompoundName(CompoundName compoundName) {
        Observer.forClass(DepictionEntityRepositoryRdbms.class)
                .arguments("getByCompoundName", i -> i.identifier(compoundName, "compoundName"))
                .throwWhenInvalid();
        return Optional.ofNullable(mapper.selectByCompoundName(compoundName.value()))
                .map(CompoundDepictionDbo::toEntity);
    }

    @Override
    public List<CompoundName> getAllDepictedCompoundNames() {
        return mapper.selectAllDepictedCompoundNames().stream().map(CompoundName::of).toList();
    }
}
