package com.naturalist.test.context;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.test.query.nofanout.SelectTrace;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.SuperMethodCall;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import static net.bytebuddy.matcher.ElementMatchers.isAbstract;
import static net.bytebuddy.matcher.ElementMatchers.isFinal;
import static net.bytebuddy.matcher.ElementMatchers.isPrivate;
import static net.bytebuddy.matcher.ElementMatchers.isStatic;
import static net.bytebuddy.matcher.ElementMatchers.isSynthetic;
import static net.bytebuddy.matcher.ElementMatchers.isVisibleTo;
import static net.bytebuddy.matcher.ElementMatchers.not;

/**
 * One test's object graph, assembled by reflection under the guidance of every registered
 * {@link TestContext}. One instance per class per graph; one graph and one fake registry per test.
 *
 * <p>A type {@code T} asked for by context {@code R} (the test itself has none) resolves to, in order:
 * a fake a test handed to a context; what {@code R} provides; what the context owning {@code T}'s
 * package provides; and otherwise the one {@code @DomainService} / {@code @MockDomainService} class of
 * that package that is {@code T} or implements it, built through its single constructor (or its single
 * static {@code create} / {@code of}) with its parameters resolved for the owning context.
 *
 * <p>Everything the graph builds is traced, so each call a test makes on it starts a new count. A
 * {@code com.naturalist} interface that is not sealed is handed out wrapped by {@link SelectTrace#traced},
 * one proxy per type, so every requester shares it. A non-final concrete class is built, once, as a
 * {@link SelectTrace.Traced} subclass generated in its own package; a {@code final} class is built plain and
 * untraced. Before building a class the graph checks that a proxy can reach every method it marks
 * {@code @jakarta.transaction.Transactional}.
 */
public final class ObjectGraph {

    private static final String JAKARTA_TRANSACTIONAL = "jakarta.transaction.Transactional";

    private static final String SPRING_TRANSACTIONAL = "org.springframework.transaction.annotation.Transactional";

    /** The sites already warned about, so each Spring {@code @Transactional} is reported once per run. */
    private static final Set<String> SPRING_WARNINGS = ConcurrentHashMap.newKeySet();

    /** The traced subclass of each class a graph builds, generated once per class for the whole run. */
    private static final ClassValue<Class<?>> TRACED_SUBCLASSES = new ClassValue<>() {
        @Override
        protected Class<?> computeValue(Class<?> type) {
            return tracedSubclass(type);
        }
    };

    private final State state;
    /** The context this view asks for; null when the test itself asks. */
    private final TestContext requester;

    private ObjectGraph(State state, TestContext requester) {
        this.state = state;
        this.requester = requester;
    }

    /** A new graph over {@code db}, with one fresh instance of every registered {@link TestContext}. */
    public static ObjectGraph create(NaturalistDatabase db) {
        ObjectGraph graph = new ObjectGraph(new State(db), null);
        for (TestContext context : ServiceLoader.load(TestContext.class)) {
            context.attach(graph);
            Provisions provisions = new Provisions();
            context.provisions(provisions);
            graph.state.contexts.put(context, provisions);
            graph.state.byPackage.computeIfAbsent(context.getClass().getPackageName(), k -> new ArrayList<>())
                    .add(context);
        }
        return graph;
    }

    public <T> T get(Class<T> type) {
        return type.cast(resolve(type));
    }

    /** The registered instance of {@code type} in this graph. */
    public <C extends TestContext> C context(Class<C> type) {
        for (TestContext context : state.contexts.keySet()) {
            if (context.getClass() == type) {
                return type.cast(context);
            }
        }
        throw new TestGraphException(type.getName() + " is not registered in META-INF/services/"
                + TestContext.class.getName());
    }

    /** The context registered for {@code packageName}: where a test of that package hands over its fakes. */
    public TestContext owner(String packageName) {
        return contextFor(packageName,
                () -> "register one in META-INF/services/" + TestContext.class.getName());
    }

    /** Every fake handed to a context of this graph, in the order they were handed over. */
    public List<RegisteredFake> fakes() {
        return List.copyOf(state.registeredFakes);
    }

    <T> void registerFake(TestContext context, Class<T> type, T fake) {
        if (type == null || fake == null) {
            throw new TestGraphException(context.getClass().getSimpleName() + ": a fake needs a type and a value");
        }
        if (!type.isInstance(fake)) {
            throw new TestGraphException(fake.getClass().getName() + " is not a " + type.getName());
        }
        if (state.resolved.contains(type)) {
            throw new TestGraphException(type.getName() + " was already built; hand the fake over before the first get");
        }
        if (state.fakes.putIfAbsent(type, fake) != null) {
            throw new TestGraphException(type.getName() + " already has a fake");
        }
        state.registeredFakes.add(new RegisteredFake(context.getClass().getSimpleName(), type, fake.getClass()));
    }

    private Object resolve(Class<?> type) {
        if (NaturalistDatabase.class.isAssignableFrom(type) && type.isInstance(state.db)) {
            return state.db;
        }
        Object fake = state.fakes.get(type);
        if (fake != null) {
            return fake;
        }
        if (state.requests.contains(type)) {
            throw new TestGraphException("cycle: " + chain() + " > " + type.getSimpleName());
        }
        boolean byRequester = requester != null && state.contexts.get(requester).factory(type) != null;
        TestContext owner = byRequester ? requester : ownerOf(type);
        state.requests.push(type);
        try {
            Object resolved = byRequester || state.contexts.get(owner).factory(type) != null
                    ? provided(owner, type)
                    : built(type, owner);
            state.resolved.add(type);
            return resolved;
        } finally {
            state.requests.pop();
        }
    }

    private TestContext ownerOf(Class<?> type) {
        return contextFor(type.getPackageName(), () -> type.getName() + " requested by " + chain());
    }

    private TestContext contextFor(String packageName, Supplier<String> detail) {
        List<TestContext> owners = state.byPackage.getOrDefault(packageName, List.of());
        if (owners.isEmpty()) {
            throw new TestGraphException("no test context for package " + packageName + "; " + detail.get());
        }
        if (owners.size() > 1) {
            throw new TestGraphException("two test contexts registered for package " + packageName + ": "
                    + owners.stream().map(c -> c.getClass().getName()).toList());
        }
        return owners.getFirst();
    }

    /** What {@code context} provides for {@code type}, built once per (context, type). */
    private Object provided(TestContext context, Class<?> type) {
        Map<Class<?>, Object> cache = state.provided.computeIfAbsent(context, k -> new HashMap<>());
        Object cached = cache.get(type);
        if (cached != null) {
            return cached;
        }
        Object value = state.contexts.get(context).factory(type).apply(new ObjectGraph(state, context));
        if (!type.isInstance(value)) {
            throw new TestGraphException(context.getClass().getSimpleName() + " provided "
                    + (value == null ? "null" : value.getClass().getName()) + " for " + type.getName());
        }
        Object handed = handOut(type, value);
        cache.put(type, handed);
        return handed;
    }

    /** {@code type} built by reflection over {@code owner}'s package, handed out once per type. */
    private Object built(Class<?> type, TestContext owner) {
        Object handed = state.handedOut.get(type);
        if (handed != null) {
            return handed;
        }
        Object result = handOut(type, instance(implementation(type, owner), owner));
        state.handedOut.put(type, result);
        return result;
    }

    private static Class<?> implementation(Class<?> type, TestContext owner) {
        String context = owner.getClass().getSimpleName();
        List<Class<?>> candidates = PackageScan.candidates(owner.getClass().getPackageName());
        if (isConcrete(type)) {
            if (candidates.contains(type)) {
                return type;
            }
            throw new TestGraphException(type.getName()
                    + " is not a @DomainService or @MockDomainService class; provide it in " + context);
        }
        List<Class<?>> matches = candidates.stream()
                .filter(candidate -> isConcrete(candidate) && type.isAssignableFrom(candidate))
                .toList();
        if (matches.size() != 1) {
            throw new TestGraphException(context + " finds " + matches.size() + " candidates for "
                    + type.getName() + " " + matches.stream().map(Class::getName).toList()
                    + "; provide one in " + context);
        }
        return matches.getFirst();
    }

    private static boolean isConcrete(Class<?> type) {
        return !type.isInterface() && !type.isPrimitive() && !type.isArray()
                && !Modifier.isAbstract(type.getModifiers());
    }

    /** The one instance of {@code impl} in this graph, so a diamond builds it once. */
    private Object instance(Class<?> impl, TestContext owner) {
        Object existing = state.instances.get(impl);
        if (existing != null) {
            return existing;
        }
        if (!state.building.add(impl)) {
            throw new TestGraphException("cycle building " + impl.getName() + ": " + chain());
        }
        try {
            Object built = new ObjectGraph(state, owner).construct(impl);
            state.instances.put(impl, built);
            state.resolved.add(impl);
            return built;
        } finally {
            state.building.remove(impl);
        }
    }

    private Object construct(Class<?> impl) {
        checkReachable(impl);
        Constructor<?>[] constructors = impl.getDeclaredConstructors();
        try {
            if (constructors.length == 1) {
                Constructor<?> constructor = tracing(constructors[0]);
                constructor.setAccessible(true);
                return constructor.newInstance(arguments(constructor.getParameterTypes()));
            }
            Method factory = factoryMethod(impl);
            if (factory == null) {
                throw new TestGraphException(impl.getName() + ": no single constructor; provide it in "
                        + requester.getClass().getSimpleName());
            }
            factory.setAccessible(true);
            return factory.invoke(null, arguments(factory.getParameterTypes()));
        } catch (InvocationTargetException e) {
            throw new TestGraphException("building " + impl.getName() + " threw " + e.getCause(), e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new TestGraphException("cannot build " + impl.getName(), e);
        }
    }

    private Object[] arguments(Class<?>[] types) {
        Object[] arguments = new Object[types.length];
        for (int i = 0; i < types.length; i++) {
            arguments[i] = resolve(types[i]);
        }
        return arguments;
    }

    /** The single static {@code create} or {@code of} returning {@code impl}, or null. */
    private static Method factoryMethod(Class<?> impl) {
        Method found = null;
        for (Method method : impl.getDeclaredMethods()) {
            if (Modifier.isStatic(method.getModifiers()) && method.getReturnType() == impl
                    && (method.getName().equals("create") || method.getName().equals("of"))) {
                if (found != null) {
                    return null;
                }
                found = method;
            }
        }
        return found;
    }

    private static Object handOut(Class<?> type, Object value) {
        return isTraced(type) ? traced(type, value) : value;
    }

    private static boolean isTraced(Class<?> type) {
        return type.isInterface() && !type.isAnnotation() && !type.isSealed()
                && type.getName().startsWith("com.naturalist.");
    }

    @SuppressWarnings("unchecked")
    private static Object traced(Class<?> type, Object value) {
        return SelectTrace.traced((Class<Object>) type, value);
    }

    /** The types being resolved, outermost first, for error messages. */
    private String chain() {
        StringJoiner chain = new StringJoiner(" > ");
        chain.add("the test");
        Iterator<Class<?>> requests = state.requests.descendingIterator();
        while (requests.hasNext()) {
            chain.add(requests.next().getSimpleName());
        }
        return chain.toString();
    }

    /** What every view of one graph shares. */
    private static final class State {
        final NaturalistDatabase db;
        final Map<TestContext, Provisions> contexts = new LinkedHashMap<>();
        final Map<String, List<TestContext>> byPackage = new HashMap<>();
        final Map<Class<?>, Object> fakes = new HashMap<>();
        final List<RegisteredFake> registeredFakes = new ArrayList<>();
        final Map<TestContext, Map<Class<?>, Object>> provided = new HashMap<>();
        final Map<Class<?>, Object> instances = new HashMap<>();
        final Map<Class<?>, Object> handedOut = new HashMap<>();
        final Set<Class<?>> resolved = new HashSet<>();
        final Set<Class<?>> building = new LinkedHashSet<>();
        final Deque<Class<?>> requests = new ArrayDeque<>();

        State(NaturalistDatabase db) {
            this.db = db;
        }
    }

    /**
     * A {@link SelectTrace.Traced} subclass of {@code type}, defined in its package, whose every overridable
     * method declared by {@code type} or a {@code com.naturalist} superclass enters through
     * {@link SelectTrace.TraceAdvice} before it runs the original.
     */
    private static Class<?> tracedSubclass(Class<?> type) {
        Set<String> owners = new HashSet<>();
        for (Class<?> owner = type; owner != null && owner.getName().startsWith("com.naturalist."); owner = owner.getSuperclass()) {
            owners.add(owner.getName());
        }
        ElementMatcher<TypeDescription> declaredByOwner = declaring -> owners.contains(declaring.getName());
        try {
            return new ByteBuddy()
                    .subclass(type, ConstructorStrategy.Default.IMITATE_SUPER_CLASS)
                    .implement(SelectTrace.Traced.class)
                    .method(ElementMatchers.<MethodDescription>isDeclaredBy(declaredByOwner)
                            .and(isVisibleTo(TypeDescription.ForLoadedType.of(type)))
                            .and(not(isPrivate())).and(not(isStatic())).and(not(isFinal()))
                            .and(not(isAbstract())).and(not(isSynthetic())))
                    .intercept(Advice.to(SelectTrace.TraceAdvice.class).wrap(SuperMethodCall.INSTANCE))
                    .make()
                    .load(type.getClassLoader(),
                            ClassLoadingStrategy.UsingLookup.of(MethodHandles.privateLookupIn(type, MethodHandles.lookup())))
                    .getLoaded();
        } catch (IllegalAccessException e) {
            throw new TestGraphException("cannot trace " + type.getName() + ": " + e.getMessage(), e);
        }
    }

    /**
     * The same constructor on the class's {@link SelectTrace.Traced} subclass, so the object is built once, as
     * the subclass. A class that cannot be subclassed, a {@code final} one above all, is built plain and untraced.
     */
    private static Constructor<?> tracing(Constructor<?> constructor) throws NoSuchMethodException {
        Class<?> type = constructor.getDeclaringClass();
        if (Modifier.isFinal(type.getModifiers()) || type.isSealed() || type.isEnum() || type.isRecord()
                || type.isAnonymousClass() || type.isLocalClass() || Modifier.isPrivate(constructor.getModifiers())) {
            return constructor;
        }
        return TRACED_SUBCLASSES.get(type).getDeclaredConstructor(constructor.getParameterTypes());
    }

    /**
     * Fails when a method of {@code type} or a {@code com.naturalist} superclass carries
     * {@code @jakarta.transaction.Transactional}, on itself or on its class, where a proxy cannot reach it; warns
     * once on Spring's {@code @Transactional}. Annotations are matched by type name, so no Spring dependency is needed.
     */
    private static void checkReachable(Class<?> type) {
        for (Class<?> owner = type; owner != null && owner.getName().startsWith("com.naturalist."); owner = owner.getSuperclass()) {
            boolean jakartaClass = carries(owner, JAKARTA_TRANSACTIONAL);
            boolean springClass = carries(owner, SPRING_TRANSACTIONAL);
            for (Method method : owner.getDeclaredMethods()) {
                int modifiers = method.getModifiers();
                boolean proxied = !method.isSynthetic() && !Modifier.isPrivate(modifiers) && !Modifier.isStatic(modifiers);
                String site = type.getName() + "#" + method.getName();
                if (carries(method, JAKARTA_TRANSACTIONAL) || jakartaClass && proxied) {
                    String reason = unreachable(type, modifiers);
                    if (reason != null) {
                        throw new TestGraphException(site
                                + " carries @jakarta.transaction.Transactional but a proxy cannot reach it: " + reason);
                    }
                }
                if ((carries(method, SPRING_TRANSACTIONAL) || springClass && proxied) && SPRING_WARNINGS.add(site)) {
                    System.err.println("WARN test-context: Spring @Transactional on " + site
                            + "; core uses jakarta.transaction.Transactional");
                }
            }
        }
    }

    /** Why a proxy of {@code type} cannot reach a method with {@code modifiers}, or null when it can. */
    private static String unreachable(Class<?> type, int modifiers) {
        if (Modifier.isFinal(type.getModifiers())) {
            return "the class is final";
        }
        if (Modifier.isFinal(modifiers)) {
            return "the method is final";
        }
        if (Modifier.isPrivate(modifiers)) {
            return "the method is private";
        }
        return Modifier.isStatic(modifiers) ? "the method is static" : null;
    }

    private static boolean carries(AnnotatedElement element, String annotation) {
        for (Annotation declared : element.getDeclaredAnnotations()) {
            if (declared.annotationType().getName().equals(annotation)) {
                return true;
            }
        }
        return false;
    }
}
