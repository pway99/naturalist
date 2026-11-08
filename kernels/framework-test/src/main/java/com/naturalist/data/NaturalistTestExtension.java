package com.naturalist.data;

import com.naturalist.test.context.Fake;
import com.naturalist.test.context.ObjectGraph;
import com.naturalist.test.context.TestContext;
import com.naturalist.test.context.TestGraphException;
import com.naturalist.test.context.Wired;
import com.naturalist.test.query.nofanout.SelectTrace;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * JUnit 5 lifecycle wrapper around {@link NaturalistDatabase}. Clears the source
 * registry before each test so methods start from a pristine catalog, and hosts the
 * N+1 select gate: arms {@link SelectTrace} before each test and evaluates it after.
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
 *
 * <p>It is also the access point for anything under test in a {@code *-core} module:
 * {@link #get} hands out one wired, traced instance per type from an {@link ObjectGraph} built
 * under the registered {@link TestContext}s, and before each test every {@link Fake} field is
 * handed to the context of the test's own package and every {@link Wired} field is filled.
 * A fake stays in the graph's memory; one for a domain service fails as it is handed over.
 */
public class NaturalistTestExtension extends NaturalistDatabase
        implements BeforeEachCallback, AfterEachCallback {

    /** This test's object graph, created on first use and dropped before each test. */
    private ObjectGraph graph;

    private NaturalistTestExtension() {
        super();
    }

    public static NaturalistTestExtension create() {
        return new NaturalistTestExtension();
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        clear();
        graph = null;
        testPackage = context.getRequiredTestClass().getPackageName();
        for (Object instance : context.getRequiredTestInstances().getAllInstances()) {
            inject(instance);
        }
        SelectTrace.arm();
    }

    @Override
    public void afterEach(ExtensionContext context) {
        try {
            SelectTrace.evaluate(context.getRequiredTestClass().getSimpleName() + "." + context.getRequiredTestMethod().getName());
        } finally {
            SelectTrace.disarm();
        }
    }

    /** The one {@code type} of this test's object graph, wired and traced. */
    public <T> T get(Class<T> type) {
        return graph().get(type);
    }

    /** This test's instance of a registered test context, to hand it a fake before the first {@code get}. */
    public <C extends TestContext> C context(Class<C> type) {
        return graph().context(type);
    }

    private ObjectGraph graph() {
        if (graph == null) {
            graph = ObjectGraph.create(this).requestedFrom(testPackage);
        }
        return graph;
    }

    /**
     * Hands every {@code @Fake} field of the test to the context of the test's own package, then fills every
     * {@code @Wired} field from the graph. Runs before the test's own {@code @BeforeEach} methods.
     */
    private void inject(Object test) {
        List<Field> fakes = new ArrayList<>();
        List<Field> wired = new ArrayList<>();
        for (Class<?> type = test.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (field.isAnnotationPresent(Fake.class)) {
                    fakes.add(field);
                }
                if (field.isAnnotationPresent(Wired.class)) {
                    wired.add(field);
                }
            }
        }
        for (Field field : fakes) {
            field.setAccessible(true);
            Object value = read(field, test);
            if (value == null) {
                throw new TestGraphException("@Fake " + field + " is null; set it in its initialiser");
            }
            handOver(graph().owner(test.getClass().getPackageName()), portOf(field, value), value);
        }
        for (Field field : wired) {
            if (Modifier.isFinal(field.getModifiers()) || Modifier.isStatic(field.getModifiers())) {
                throw new TestGraphException("@Wired " + field + " must be neither final nor static");
            }
            field.setAccessible(true);
            try {
                field.set(test, graph().get(field.getType()));
            } catch (IllegalAccessException e) {
                throw new TestGraphException("cannot set @Wired " + field, e);
            }
        }
    }

    private static Object read(Field field, Object test) {
        try {
            return field.get(test);
        } catch (IllegalAccessException e) {
            throw new TestGraphException("cannot read @Fake " + field, e);
        }
    }

    @SuppressWarnings("unchecked")
    private static void handOver(TestContext context, Class<?> type, Object fake) {
        context.fake((Class<Object>) type, fake);
    }

    /**
     * The package of the running test class: its registered context is the requester of the test's own
     * {@code @Wired} fields and {@link #get} calls. Null outside a test.
     */
    private String testPackage;

    /**
     * The port a {@code @Fake} field replaces: its annotation's value, which the field's value must be an
     * instance of, or else the field's declared type, which must then be an interface or abstract class.
     * Fakes register under that exact type, so a field declared as a concrete fake class would replace nothing.
     */
    private static Class<?> portOf(Field field, Object value) {
        Class<?> port = field.getAnnotation(Fake.class).value();
        if (port != Void.class) {
            if (!port.isInstance(value)) {
                throw new TestGraphException("@Fake(" + port.getSimpleName() + ".class) " + field + " holds a "
                        + value.getClass().getName() + ", which is not a " + port.getName());
            }
            return port;
        }
        Class<?> declared = field.getType();
        if (!declared.isInterface() && !Modifier.isAbstract(declared.getModifiers())) {
            throw new TestGraphException("@Fake " + field + " is declared as the concrete " + declared.getName()
                    + ", so it would replace nothing; declare the field as the port type it stands in for,"
                    + " or use @Fake(Port.class)");
        }
        return declared;
    }
}
