package com.naturalist.naturalist;

import com.naturalist.data.AbstractEntityRepository;
import com.naturalist.data.Page;
import com.naturalist.data.PageRequest;
import com.naturalist.exception.EntityNotFoundException;
import com.naturalist.exception.PrimaryKeyConstraintException;
import com.naturalist.persistence.RdbmsExceptions;

import java.util.List;
import java.util.Optional;
import java.util.Set;

class NaturalistEntityRepositoryRdbms
        extends AbstractEntityRepository<NaturalistName, Naturalist>
        implements NaturalistRepository.NaturalistEntityRepository {

    private final NaturalistMapper mapper;

    NaturalistEntityRepositoryRdbms(NaturalistMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected Optional<Naturalist> doGetByName(NaturalistName name) {
        NaturalistDbo dbo = mapper.selectByName(name.value());
        return Optional.ofNullable(dbo).map(NaturalistDbo::toEntity);
    }

    @Override
    protected List<Naturalist> doGetByNameSet(Set<NaturalistName> nameSet) {
        if (nameSet.isEmpty()) return List.of();
        List<String> slugs = nameSet.stream().map(NaturalistName::value).toList();
        return mapper.selectByNameSet(slugs).stream().map(NaturalistDbo::toEntity).toList();
    }

    @Override
    protected Page<Naturalist> doGetPage(PageRequest request) {
        int pageSize = request.pageSize();
        List<NaturalistDbo> rows = mapper.selectPage(pageSize + 1, request.offset());
        boolean immediateMore = rows.size() > pageSize;
        List<Naturalist> content = rows.stream().limit(pageSize).map(NaturalistDbo::toEntity).toList();

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
    protected void doInsert(Naturalist entity) {
        NaturalistDbo dbo = NaturalistDbo.from(entity);
        observer().arguments("doInsert", i -> i.observable(dbo, "dbo")).throwWhenInvalid();
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
    protected void doUpdate(Naturalist entity) {
        NaturalistDbo dbo = NaturalistDbo.from(entity);
        observer().arguments("doUpdate", i -> i.observable(dbo, "dbo")).throwWhenInvalid();
        if (mapper.updateByName(dbo) == 0) {
            throw new EntityNotFoundException(entity);
        }
    }

    @Override
    protected Naturalist doSave(Naturalist entity) {
        if (mapper.selectByName(entity.name().value()) != null) {
            doUpdate(entity);
        } else {
            doInsert(entity);
        }
        return entity;
    }
}
