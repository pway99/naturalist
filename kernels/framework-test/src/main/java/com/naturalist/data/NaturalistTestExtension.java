package com.naturalist.data;

import com.naturalist.test.query.nofanout.AllowRepeatedSelect;
import com.naturalist.test.query.nofanout.SelectCountRecorder;
import com.naturalist.test.query.nofanout.SelectGate;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.reflect.Method;
import java.util.List;

/**
 * JUnit 5 lifecycle wrapper around {@link NaturalistDatabase}. Clears the source
 * registry before each test so methods start from a pristine catalog, and hosts the
 * N+1 select gate: arms {@link SelectCountRecorder} before each test and evaluates the
 * recorded tally against {@link SelectGate} after, honouring {@link AllowRepeatedSelect}.
 *
 * <p>Register as a field-level extension on any test class that composes
 * {@link TestEntitySource}-backed repository mocks:
 *
 * <pre>{@code
 * @RegisterExtension
 * NaturalistTestExtension nte = NaturalistTestExtension.create();
 * }</pre>
 *
 * <p>Extends {@link NaturalistDatabase} so tests can pass this instance directly to
 * constructors expecting the plain registry (e.g. {@code new InsectSpeciesRepositoryMock(db)}).
 * Main-wired code (console bootstraps, CLI tools) uses {@link NaturalistDatabase#create()}
 * and carries no JUnit coupling.
 */
public class NaturalistTestExtension extends NaturalistDatabase
        implements BeforeEachCallback, AfterEachCallback {

    private NaturalistTestExtension() {
        super();
    }

    public static NaturalistTestExtension create() {
        return new NaturalistTestExtension();
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        clear();
        SelectCountRecorder.arm();
    }

    @Override
    public void afterEach(ExtensionContext context) {
        try {
            SelectGate.evaluate(SelectCountRecorder.snapshot(), allowlist(context));
        } finally {
            SelectCountRecorder.disarm();
        }
    }

    private static List<AllowRepeatedSelect> allowlist(ExtensionContext context) {
        return context.getTestMethod()
                .map(NaturalistTestExtension::readAllowlist)
                .orElseGet(List::of);
    }

    private static List<AllowRepeatedSelect> readAllowlist(Method method) {
        return List.of(method.getAnnotationsByType(AllowRepeatedSelect.class));
    }
}
