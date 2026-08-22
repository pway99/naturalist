package com.naturalist.data;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * JUnit 5 lifecycle wrapper around {@link NaturalistDatabase}. Clears the source
 * registry before each test so methods start from a pristine catalog.
 *
 * <p>Register as a field-level extension on any test class that composes
 * {@link TestEntitySource}-backed repository mocks:
 *
 * <pre>{@code
 * @RegisterExtension
 * NaturalistTestExtension db = NaturalistTestExtension.create();
 * }</pre>
 *
 * <p>Extends {@link NaturalistDatabase} so tests can pass this instance directly to
 * constructors expecting the plain registry (e.g. {@code new InsectSpeciesRepositoryMock(db)}).
 * Main-wired code (console bootstraps, CLI tools) uses {@link NaturalistDatabase#create()}
 * and carries no JUnit coupling.
 */
public class NaturalistTestExtension extends NaturalistDatabase implements BeforeEachCallback {

    private NaturalistTestExtension() {
        super();
    }

    public static NaturalistTestExtension create() {
        return new NaturalistTestExtension();
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        clear();
    }
}
