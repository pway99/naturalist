package com.naturalist.library;

import com.naturalist.test.context.TestContext;

/**
 * The test context of {@code com.naturalist.library}. It declares no provisions: every library class has
 * one constructor, and {@code CladeQueryImpl} takes no arguments, so the graph builds them all by
 * reflection over the {@code @DomainService} implementations and the {@code @MockDomainService}
 * repository mocks.
 */
public class LibraryTestContextInternal extends TestContext {
    public LibraryTestContextInternal() {
    }
}
