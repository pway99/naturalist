package com.naturalist.insects;

import com.naturalist.data.AbstractEntityRepository;
import com.naturalist.data.Page;
import com.naturalist.data.PageRequest;
import com.naturalist.exception.EntityNotFoundException;
import com.naturalist.exception.PrimaryKeyConstraintException;
import com.naturalist.fieldnotes.CommonName;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.persistence.RdbmsExceptions;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * RDBMS adapter for the {@link InsectOrder} rank record — a {@code NamedEntity} at the top of the
 * insect Linnaean chain, so it has no upward FK. Reads assemble the parent row with its
 * {@code commonNames} loaded in one batched query keyed on the order-name set, so a page of orders
 * costs two selects, not two-per-order — no N+1. Writes persist the parent then its common-name
 * children; an update replaces the child rows wholesale.
 */
@DomainService
class InsectOrderEntityRepositoryRdbms
        extends AbstractEntityRepository<InsectOrderName, InsectOrder>
        implements InsectRepository.OrderRepository {

    private final InsectOrderMapper mapper;

    InsectOrderEntityRepositoryRdbms(InsectOrderMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected Optional<InsectOrder> doGetByName(InsectOrderName name) {
        InsectOrderDbo parent = mapper.selectByName(name.value());
        if (parent == null) return Optional.empty();
        return Optional.of(assemble(List.of(parent), Set.of(name.value())).get(0));
    }

    @Override
    protected List<InsectOrder> doGetByNameSet(Set<InsectOrderName> nameSet) {
        if (nameSet.isEmpty()) return List.of();
        Set<String> slugs = nameSet.stream().map(InsectOrderName::value).collect(Collectors.toSet());
        return assemble(mapper.selectByNameSet(slugs), slugs);
    }

    @Override
    protected Page<InsectOrder> doGetPage(PageRequest request) {
        int pageSize = request.pageSize();
        List<InsectOrderDbo> rows = mapper.selectPage(pageSize + 1, request.offset());
        boolean immediateMore = rows.size() > pageSize;
        List<InsectOrderDbo> parents = rows.stream().limit(pageSize).toList();
        Set<String> names = parents.stream().map(p -> p.name).collect(Collectors.toSet());
        List<InsectOrder> content = assemble(parents, names);

        int pagesAheadKnown = 0;
        boolean moreBeyondLookahead = immediateMore;
        if (request.lookahead() > 0 && immediateMore) {
            int window = request.lookahead() * pageSize + 1;
            int beyond = mapper.countInWindow(request.offset() + pageSize, window);
            pagesAheadKnown = Math.min(request.lookahead(), beyond / pageSize);
            moreBeyondLookahead = beyond > request.lookahead() * pageSize;
        } else if (request.lookahead() > 0) {
            moreBeyondLookahead = false;
        }
        return new Page<>(content, request.pageNumber(), pageSize, pagesAheadKnown, moreBeyondLookahead);
    }

    @Override
    protected void doInsert(InsectOrder entity) {
        InsectOrderDbo dbo = InsectOrderDbo.from(entity);   // validates the parent scalars + throws
        try {
            mapper.insert(dbo);
        } catch (RuntimeException e) {
            if (RdbmsExceptions.isUniqueViolation(e)) throw new PrimaryKeyConstraintException(entity);
            throw e;
        }
        insertChildren(entity);
    }

    @Override
    protected void doUpdate(InsectOrder entity) {
        InsectOrderDbo dbo = InsectOrderDbo.from(entity);
        if (mapper.updateByName(dbo) == 0) throw new EntityNotFoundException(entity);
        mapper.deleteCommonNames(entity.name().value());
        insertChildren(entity);
    }

    @Override
    protected InsectOrder doSave(InsectOrder entity) {
        if (mapper.selectByName(entity.name().value()) != null) doUpdate(entity);
        else doInsert(entity);
        return entity;
    }

    private void insertChildren(InsectOrder o) {
        InsectOrderName name = o.name();
        for (CommonName commonName : o.commonNames()) {
            mapper.insertCommonName(InsectOrderCommonNameDbo.from(name, commonName));
        }
    }

    /** Assemble records from parent rows + one batched common-name load. */
    private List<InsectOrder> assemble(List<InsectOrderDbo> parents, Set<String> names) {
        if (parents.isEmpty()) return List.of();
        var commonNames = mapper.selectCommonNames(names).stream()
                .collect(Collectors.groupingBy(cn -> cn.orderName,
                        Collectors.mapping(InsectOrderCommonNameDbo::toCommonName, Collectors.toSet())));
        return parents.stream()
                .map(p -> p.toEntity(commonNames.getOrDefault(p.name, Set.of())))
                .toList();
    }
}
