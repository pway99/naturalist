package com.naturalist.persistence.test.nofanout;

import com.naturalist.data.AbstractEntityRepository;
import com.naturalist.data.Page;
import com.naturalist.data.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Minimal fake repository for {@link RepositoryHeadWeavingTest}. Its public methods stand in for
 * real repository operations so {@link RepositoryHeadAspect} weaves a head around them; the bodies
 * call {@link MapperSelectRecorder#recordSelect} directly, standing in for
 * {@link MapperSelectInterceptor} (which needs a live MyBatis {@code Executor}). The {@code do*}
 * hooks are never invoked.
 */
final class GadgetRepositoryRdbms extends AbstractEntityRepository<String, Gadget> {

    static final String SELECT = "com.naturalist.persistence.test.nofanout.GadgetMapper.selectById";

    @Override
    protected Optional<Gadget> doGetByName(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected List<Gadget> doGetByNameSet(Set<String> nameSet) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected Page<Gadget> doGetPage(PageRequest pageRequest) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void doInsert(Gadget entity) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void doUpdate(Gadget entity) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected Gadget doSave(Gadget entity) {
        throw new UnsupportedOperationException();
    }

    /** One select per element — the N+1 shape the gate must catch. */
    public List<Gadget> fannedOut(Set<String> ids) {
        for (String id : ids) {
            MapperSelectRecorder.recordSelect(SELECT);
        }
        return List.of();
    }

    /** One select for the whole set — the correct batched shape. */
    public List<Gadget> batched(Set<String> ids) {
        MapperSelectRecorder.recordSelect(SELECT);
        return List.of();
    }
}
