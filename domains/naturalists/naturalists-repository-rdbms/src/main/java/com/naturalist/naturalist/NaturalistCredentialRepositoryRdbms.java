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

class NaturalistCredentialRepositoryRdbms
        extends AbstractEntityRepository<NaturalistName, NaturalistCredential>
        implements NaturalistRepository.CredentialRepository {

    private final NaturalistCredentialMapper mapper;

    NaturalistCredentialRepositoryRdbms(NaturalistCredentialMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected Optional<NaturalistCredential> doGetByName(NaturalistName name) {
        NaturalistCredentialDbo dbo = mapper.selectByName(name.value());
        return Optional.ofNullable(dbo).map(NaturalistCredentialDbo::toEntity);
    }

    @Override
    protected List<NaturalistCredential> doGetByNameSet(Set<NaturalistName> nameSet) {
        if (nameSet.isEmpty()) return List.of();
        List<String> slugs = nameSet.stream().map(NaturalistName::value).toList();
        return mapper.selectByNameSet(slugs).stream().map(NaturalistCredentialDbo::toEntity).toList();
    }

    @Override
    protected Page<NaturalistCredential> doGetPage(PageRequest request) {
        int pageSize = request.pageSize();
        List<NaturalistCredentialDbo> rows = mapper.selectPage(pageSize + 1, request.offset());
        boolean immediateMore = rows.size() > pageSize;
        List<NaturalistCredential> content =
                rows.stream().limit(pageSize).map(NaturalistCredentialDbo::toEntity).toList();
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
    protected void doInsert(NaturalistCredential entity) {
        // naturalist_id is resolved inline by the mapper's INSERT … SELECT; DBO carries name.
        NaturalistCredentialDbo dbo = NaturalistCredentialDbo.from(entity, -1L);
        // validation asserts password_hash width; naturalist_id is filled by SQL, so skip its notNull here
        observer().arguments("doInsert", i -> i
                .notBlank(dbo.password_hash, "password_hash")
                .maxLength(dbo.password_hash, 80, "password_hash")).throwWhenInvalid();
        int inserted;
        try {
            inserted = mapper.insert(dbo);
        } catch (RuntimeException e) {
            if (RdbmsExceptions.isUniqueViolation(e)) {
                throw new PrimaryKeyConstraintException(entity);
            }
            throw e;
        }
        // The mapper's INSERT … SELECT resolves naturalist_id from naturalist.name inline:
        // if no naturalist row matches, the SELECT yields zero rows and the INSERT silently
        // affects zero rows (no FK/NOT NULL violation is ever triggered). Detect that case
        // explicitly so a credential for a missing naturalist fails loudly instead of no-op.
        if (inserted == 0) {
            throw new EntityNotFoundException(entity);
        }
    }

    @Override
    protected void doUpdate(NaturalistCredential entity) {
        NaturalistCredentialDbo dbo = NaturalistCredentialDbo.from(entity, -1L);
        observer().arguments("doUpdate", i -> i
                .notBlank(dbo.password_hash, "password_hash")
                .maxLength(dbo.password_hash, 80, "password_hash")).throwWhenInvalid();
        if (mapper.updateByName(dbo) == 0) {
            throw new EntityNotFoundException(entity);
        }
    }

    @Override
    protected NaturalistCredential doSave(NaturalistCredential entity) {
        if (mapper.selectByName(entity.name().value()) != null) {
            doUpdate(entity);
        } else {
            doInsert(entity);
        }
        return entity;
    }
}
