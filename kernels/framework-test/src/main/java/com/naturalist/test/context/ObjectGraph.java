package com.naturalist.test.context;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.test.query.nofanout.SelectTrace;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.modifier.SyntheticState;
import net.bytebuddy.description.modifier.Visibility;
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
import java.lang.reflect.Executable;
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
import java.util.Optional;
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
 * <p>A type {@code T} asked for by context {@code R} (for the test itself, the context of the test class's
 * package, or none; see {@link #requestedFrom}) resolves to, in order:
 * a fake a test handed to a context; what {@code R} provides; what the context owning {@code T}'s
 * package provides; and otherwise the one {@code @DomainService} / {@code @MockDomainService} class of
 * that package that is {@code T} or implements it, built through its single constructor (or its single
 * static {@code create} / {@code of}) with its parameters resolved for the owning context.
 *
 * <p>A type whose package has no context of its own, an outside-world port such as {@code Resilience}, is
 * the exception: after a fake, it comes from one provider for the whole graph, the test's own context when
 * it provides one and otherwise the only context that does. Every domain's classes then share one instance,
 * and two providers with no choice by the test's own context fail loudly.
 *
 * <p>What the graph hands out is traced, so each call a test makes on it starts a new count. Two senses of
 * "port" meet here. The graph's port is any {@code com.naturalist} interface that is not sealed: whatever is
 * asked for as one, built or provided, is handed out wrapped by {@link SelectTrace#traced}, one proxy per type,
 * so every requester shares it. That proxy in turn wraps what its methods return only when the return type is a
 * return-wrapping port ({@code SelectTrace.isPort}: a {@code *Query} or {@code *Command} interface).
 *
 * <p>Only a non-final concrete class the graph builds through its constructor is built, once, as a
 * {@link SelectTrace.Traced} subclass generated in its own package; its {@code getClass()} is then a
 * synthetic {@code ...$ByteBuddy$...} subclass, which {@code Observer} names for the class it extends, so an
 * {@code Observer.forClass(getClass())} metric or observation point reads as it does in production.
 * Never a Traced subclass, though still proxied when asked for as a graph port: a {@code final} class (or a
 * sealed, record, enum, anonymous or local one), the result of a static {@code create} / {@code of}, and
 * anything a context provides. A fake is handed out as it is, untraced: that can only cause a false failure,
 * never hide a fan-out.
 *
 * <p>Before building a class the graph checks that a proxy can reach every method it marks
 * {@code @jakarta.transaction.Transactional}, a class-level one inherited from a superclass included.
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
        for (Class<? extends TestContext> type : contextTypes()) {
            TestContext context;
            try {
                Constructor<? extends TestContext> constructor = type.getDeclaredConstructor();
                constructor.setAccessible(true);
                context = constructor.newInstance();
            } catch (ReflectiveOperationException e) {
                throw new TestGraphException("cannot create test context " + type.getName(), e);
            }
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
            throw new TestGraphException(context.getClass().getSimpleName() + ": a fake needs a type and a value; got "
                    + (type == null ? "no type" : "type " + type.getName())
                    + (fake == null ? " with a null value" : " with a " + fake.getClass().getName()));
        }
        if (!type.isInstance(fake)) {
            throw new TestGraphException(fake.getClass().getName() + " is not a " + type.getName());
        }
        if (state.byPackage.containsKey(type.getPackageName())) {
            throw new TestGraphException(type.getName() + " is a domain service built by "
                    + owner(type.getPackageName()).getClass().getSimpleName()
                    + ", so a test runs the real one; only an outside-world port, whose package has no test context, takes a fake");
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
        TestContext provider = state.byPackage.containsKey(type.getPackageName()) ? null : providerOf(type);
        boolean byRequester = provider == null && requester != null && state.contexts.get(requester).factory(type) != null;
        TestContext owner = provider != null ? provider : byRequester ? requester : ownerOf(type);
        state.requests.push(type);
        try {
            Object resolved = provider != null || byRequester || state.contexts.get(owner).factory(type) != null
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
                    + " is not a @DomainService or @MockDomainService class; provide it in " + context
                    + PackageScan.unlinked(owner.getClass().getPackageName()));
        }
        List<Class<?>> matches = candidates.stream()
                .filter(candidate -> isConcrete(candidate) && type.isAssignableFrom(candidate))
                .toList();
        if (matches.size() != 1) {
            throw new TestGraphException(context + " finds " + matches.size() + " candidates for "
                    + type.getName() + " " + matches.stream().map(Class::getName).toList()
                    + "; provide one in " + context + PackageScan.unlinked(owner.getClass().getPackageName()));
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
        REACHABLE.get(impl);
        java.lang.reflect.Executable builder = BUILDERS.get(impl).orElseThrow(() -> new TestGraphException(impl.getName()
                + ": no single constructor; provide it in " + requester.getClass().getSimpleName()));
        try {
            if (builder instanceof Constructor<?> constructor) {
                return constructor.newInstance(arguments(constructor.getParameterTypes()));
            }
            Method factory = (Method) builder;
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

        /** The context of the test's own package, which chooses the outside-world ports for the whole graph; null when none. */
        TestContext root;
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
                    .modifiers(Visibility.PUBLIC, SyntheticState.SYNTHETIC)
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
     * {@code @jakarta.transaction.Transactional}, on itself or on its class (an {@code @Inherited} class-level one
     * from a superclass included), where a proxy cannot reach it; the message names the class that declares the
     * method when that is a superclass. Warns once on Spring's {@code @Transactional}. Annotations are matched by
     * type name, so no Spring dependency is needed.
     */
    private static void checkReachable(Class<?> type) {
        for (Class<?> owner = type; owner != null && owner.getName().startsWith("com.naturalist."); owner = owner.getSuperclass()) {
            boolean jakartaClass = carriesOrInherits(owner, JAKARTA_TRANSACTIONAL);
            boolean springClass = carries(owner, SPRING_TRANSACTIONAL);
            for (Method method : owner.getDeclaredMethods()) {
                int modifiers = method.getModifiers();
                boolean proxied = !method.isSynthetic() && !Modifier.isPrivate(modifiers) && !Modifier.isStatic(modifiers);
                String site = type.getName() + "#" + method.getName()
                        + (owner == type ? "" : " (declared in " + owner.getName() + ")");
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

    /**
     * This graph as asked by the context registered for {@code packageName}, so what that context provides
     * resolves first, as it does for the classes of its own package. That context also becomes the graph's root:
     * it chooses the outside-world ports for every class in the graph (see {@link #providerOf}). With no context
     * registered there, the requests come from no context.
     */
    public ObjectGraph requestedFrom(String packageName) {
        if (state.byPackage.getOrDefault(packageName, List.of()).isEmpty()) {
            return new ObjectGraph(state, null);
        }
        state.root = owner(packageName);
        return new ObjectGraph(state, state.root);
    }

    /**
     * Whether {@code type} carries {@code annotation} or inherits it from a superclass; {@link Class#getAnnotations}
     * follows {@code @Inherited}, which {@code @jakarta.transaction.Transactional} is.
     */
    private static boolean carriesOrInherits(Class<?> type, String annotation) {
        for (Annotation present : type.getAnnotations()) {
            if (present.annotationType().getName().equals(annotation)) {
                return true;
            }
        }
        return false;
    }

    /**
     * How each class is built, chosen once per JVM: its single constructor (the traced subclass's when it is traced) or
     * its single static {@code create} / {@code of}, made accessible; empty when it has neither.
     */
    private static final ClassValue<Optional<Executable>> BUILDERS = new ClassValue<>() {
        @Override
        protected java.util.Optional<java.lang.reflect.Executable> computeValue(Class<?> impl) {
            Constructor<?>[] constructors = impl.getDeclaredConstructors();
            java.lang.reflect.Executable builder;
            if (constructors.length == 1) {
                try {
                    builder = tracing(constructors[0]);
                } catch (NoSuchMethodException e) {
                    throw new TestGraphException("cannot build " + impl.getName(), e);
                }
            } else {
                builder = factoryMethod(impl);
                if (builder == null) {
                    return java.util.Optional.empty();
                }
            }
            builder.setAccessible(true);
            return java.util.Optional.of(builder);
        }
    };

    /** {@link #checkReachable} once per class and JVM; a failing check is not cached, so it fails again. */
    private static final ClassValue<Boolean> REACHABLE = new ClassValue<>() {
        @Override
        protected Boolean computeValue(Class<?> type) {
            checkReachable(type);
            return Boolean.TRUE;
        }
    };

    /** The registered {@link TestContext} classes, read from the service files once per JVM; see {@link #contextTypes()}. */
    private static volatile List<Class<? extends TestContext>> contextTypes;

    /** The registered context classes; each graph still makes fresh instances of them. */
    private static List<Class<? extends TestContext>> contextTypes() {
        List<Class<? extends TestContext>> types = contextTypes;
        if (types == null) {
            types = ServiceLoader.load(TestContext.class).stream()
                    .<Class<? extends TestContext>>map(ServiceLoader.Provider::type)
                    .toList();
            contextTypes = types;
        }
        return types;
    }

    /**
     * The one context that provides {@code type}, a type whose package has no context of its own (an
     * outside-world port such as {@code Resilience} or {@code ImageStore}): the test's own context when it
     * provides one, otherwise the only registered context that does. One provider per graph means one instance,
     * whichever domain's class asks for it.
     */
    private TestContext providerOf(Class<?> type) {
        if (state.root != null && state.contexts.get(state.root).factory(type) != null) {
            return state.root;
        }
        List<TestContext> providers = state.contexts.entrySet().stream()
                .filter(entry -> entry.getValue().factory(type) != null)
                .map(Map.Entry::getKey)
                .toList();
        if (providers.size() == 1) {
            return providers.getFirst();
        }
        String names = providers.stream().map(c -> c.getClass().getSimpleName()).toList().toString();
        throw new TestGraphException(providers.isEmpty()
                ? "no test context provides " + type.getName() + " (requested by " + chain()
                        + ") and its package has no context of its own; provide it in the test's own context"
                : type.getName() + " is provided by " + names + "; the test's own context must provide it to choose one");
    }
}
