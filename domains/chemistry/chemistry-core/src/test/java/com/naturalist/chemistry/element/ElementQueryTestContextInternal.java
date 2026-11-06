package com.naturalist.chemistry.element;

import com.naturalist.test.query.nofanout.SelectTrace;
import com.naturalist.data.NaturalistDatabase;

/**
 * Test-scope wiring for {@link ElementQuery}. Lives in
 * {@code com.naturalist.chemistry.element} so it can instantiate the
 * package-private {@code ElementEntityRepositoryMock} and
 * {@link ElementQueryImpl} without exposing either to the wider test classpath.
 * <p>
 * Mirrors the production {@code ElementTestContext} (in
 * {@code chemistry-test-context}); duplicated here because {@code chemistry-core}
 * cannot depend on {@code chemistry-test-context} without forming a reactor cycle.
 */
public final class ElementQueryTestContextInternal {

    private ElementQueryTestContextInternal() {
    }

    public static ElementQuery createQuery(NaturalistDatabase db) {
        return SelectTrace.traced(ElementQuery.class, new ElementQueryImpl(new ElementEntityRepositoryMock(db)));
    }
}
