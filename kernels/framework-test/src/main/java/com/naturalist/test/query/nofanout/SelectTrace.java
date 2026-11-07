package com.naturalist.test.query.nofanout;

import com.naturalist.data.AbstractEntityRepository;
import com.naturalist.data.SuppressRepeatedSelect;
import com.naturalist.data.TestEntitySource;
import net.bytebuddy.asm.Advice;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestTemplate;

import java.lang.StackWalker.StackFrame;
import java.lang.annotation.Annotation;
import java.lang.invoke.MethodType;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The N+1 tracer. Each {@link TestEntitySource} read walks the stack to the enclosing test
 * method and counts per (source, repository method). A count of two fails the test when it ends.
 *
 * <p>A count covers one call from the test. The tracer only runs on a read, so it never sees a
 * call return: it starts the count again when a read arrives from a different test position,
 * meaning a different chain of test frames and bytecode indexes. A repeat inside one call all
 * arrives from one position, so it is caught wherever it happens in the unit under test.
 *
 * <p>A test graph's wrappers see that call start, so a test may loop over anything a test context
 * hands it: {@link #traced} proxies for its commands and queries, and {@link Traced} subclasses, entered
 * through {@link #enterTraced}, for the concrete classes it builds. A wrapper starts a new count only
 * when the test is its direct caller. Without one, a loop with a single reading call in the test has
 * the same stack as a loop in the unit under test, so it fails too: batch the call or take the object
 * from a context.
 *
 * <p>Not counted: reads under a write (foreign key checks), reads outside a test method, a test
 * calling a repository directly, continuation pages of a paged read, any read made under a method
 * declared {@link SuppressRepeatedSelect}, and any read by a repository method seen writing during
 * the test, because a stack walk at the read cannot know a write is coming.
 */
public final class SelectTrace {

    private static final StackWalker WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
    private static final Set<String> WRITES = Set.of(
            "insert", "insertCommon", "update", "save", "preSaveChecks", "checkForeignKey", "loadFile", "loadFiles");
    private static final Map<String, Boolean> TEST_METHODS = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> SUPPRESSING_METHODS = new ConcurrentHashMap<>();
    private static final ThreadLocal<Scope> SCOPE = ThreadLocal.withInitial(Scope::new);

    private SelectTrace() {
    }

    public static void arm() {
        Scope s = SCOPE.get();
        s.armed = true;
        nextInvocation();
        s.repeats.clear();
        s.writers.clear();
    }

    public static void disarm() {
        SCOPE.remove();
    }

    /**
     * Wraps a command or query a test context hands out, so each call a test makes on it is counted
     * separately. A call from inside the unit under test does not start a new count. Commands and
     * queries it returns are wrapped too.
     */
    public static <T> T traced(Class<T> port, T target) {
        return port.cast(Proxy.newProxyInstance(port.getClassLoader(), new Class<?>[]{port}, new Handler(target)));
    }

    private static Object invoke(Object target, Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getDeclaringClass() == Object.class && method.getName().equals("equals")) {
            return proxy == args[0];
        }
        if (SCOPE.get().armed && calledByTest()) {
            nextInvocation();
        }
        if (!method.canAccess(target)) {
            method.setAccessible(true); // a package-private port, such as a repository a test graph wraps
        }
        Object result;
        try {
            result = method.invoke(target, args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
        if (result == null || isTraced(result)) {
            return result; // already traced: keep the instance a graph handed out
        }
        return isPort(method.getReturnType()) ? tracedPort(method.getReturnType(), result) : result;
    }

    private static boolean isPort(Class<?> type) {
        return type.isInterface() && !type.isSealed() && type.getName().startsWith("com.naturalist.")
                && (type.getSimpleName().endsWith("Query") || type.getSimpleName().endsWith("Command"));
    }

    @SuppressWarnings("unchecked")
    private static <T> T tracedPort(Class<?> port, Object target) {
        synchronized (WRAPPERS) {
            Map<Class<?>, Wrapper> byPort = WRAPPERS.get(target);
            if (byPort == null) {
                byPort = new HashMap<>();
                WRAPPERS.put(target, byPort);
            }
            Wrapper cached = byPort.get(port);
            Object proxy = cached == null || cached.target().get() != target ? null : cached.proxy().get();
            if (proxy == null) {
                proxy = traced((Class<Object>) port, target);
                byPort.put(port, new Wrapper(new java.lang.ref.WeakReference<>(target), new java.lang.ref.WeakReference<>(proxy)));
            }
            return (T) proxy;
        }
    }

    /** Whether only test code stands between this wrapped call and the test method. */
    private static boolean calledByTest() {
        List<StackFrame> frames = WALKER.walk(stream -> stream.skip(1).toList());
        int testIndex = testFrameIndex(frames);
        if (testIndex < 0) {
            return false;
        }
        String testClass = outermost(frames.get(testIndex).getDeclaringClass());
        for (int i = 0; i < testIndex; i++) {
            Class<?> declaring = frames.get(i).getDeclaringClass();
            if (declaring.getName().startsWith("com.naturalist.") && !isWrapperFrame(declaring)
                    && !outermost(declaring).equals(testClass)) {
                return false;
            }
        }
        return true;
    }

    /** Starts every count again: what follows is a separate call from the test. */
    private static void nextInvocation() {
        Scope s = SCOPE.get();
        s.position = null;
        s.counts.clear();
    }

    public static void record(Class<?> source) {
        Scope s = SCOPE.get();
        if (s.armed) {
            trace(s, source);
        }
    }

    /** Marks the repository method making this write, so its reads are not reported. */
    public static void recordWrite() {
        Scope s = SCOPE.get();
        if (s.armed) {
            StackFrame repository = repositoryFrame(WALKER.walk(stream -> stream.skip(1).toList()));
            if (repository != null) {
                s.writers.add(repositoryMethod(repository));
            }
        }
    }

    private static void trace(Scope s, Class<?> source) {
        List<StackFrame> frames = WALKER.walk(stream -> stream.skip(2).toList());
        int testIndex = testFrameIndex(frames);
        if (testIndex < 0) {
            return;
        }
        String testClass = outermost(frames.get(testIndex).getDeclaringClass());
        if (isUnderSuppression(frames, testIndex, testClass)) {
            return;
        }
        String position = testPosition(frames, testIndex, testClass);
        if (!position.equals(s.position)) {
            s.position = position;
            s.counts.clear();
        }
        StackFrame site = null;
        for (int i = 0; i < testIndex; i++) {
            Class<?> declaring = frames.get(i).getDeclaringClass();
            if (isUnitFrame(declaring) && !AbstractEntityRepository.class.isAssignableFrom(declaring)) {
                if (!outermost(declaring).equals(testClass)) {
                    site = frames.get(i);
                }
                break; // a test class here means the test called the repository directly
            }
        }
        if (site != null) {
            count(s, source, repositoryFrame(frames), frames, testIndex);
        }
    }

    /** The outermost repository frame beneath the unit under test, or null when none made this call. */
    private static StackFrame repositoryFrame(List<StackFrame> frames) {
        StackFrame repository = null;
        for (StackFrame frame : frames) {
            Class<?> declaring = frame.getDeclaringClass();
            if (AbstractEntityRepository.class.isAssignableFrom(declaring) && !isWrapperFrame(declaring)) {
                repository = frame; // a Traced subclass is skipped, so the repository itself is named
            } else if (isUnitFrame(declaring)) {
                break;
            }
        }
        return repository;
    }

    private static String repositoryMethod(StackFrame repository) {
        return repository.getClassName() + "#" + repository.getMethodName();
    }

    /** The test method's index, or -1 for a read under a write or outside any test method. */
    private static int testFrameIndex(List<StackFrame> frames) {
        for (int i = 0; i < frames.size(); i++) {
            StackFrame frame = frames.get(i);
            if (TestEntitySource.class.isAssignableFrom(frame.getDeclaringClass())
                    && WRITES.contains(frame.getMethodName())) {
                return -1;
            }
            if (isTestMethod(frame)) {
                return i;
            }
        }
        return -1;
    }

    private static boolean isUnderSuppression(List<StackFrame> frames, int testIndex, String testClass) {
        for (int i = 0; i < testIndex; i++) {
            StackFrame frame = frames.get(i);
            if (!outermost(frame.getDeclaringClass()).equals(testClass) && suppressesRepeats(frame)) {
                return true;
            }
        }
        return false;
    }

    private static boolean suppressesRepeats(StackFrame frame) {
        String key = frame.getClassName() + "#" + frame.getMethodName() + frame.getDescriptor();
        return SUPPRESSING_METHODS.computeIfAbsent(key, k -> {
            if (!frame.getClassName().startsWith("com.naturalist.")) {
                return false;
            }
            for (Method method : frame.getDeclaringClass().getDeclaredMethods()) {
                if (method.isAnnotationPresent(SuppressRepeatedSelect.class)
                        && method.getName().equals(frame.getMethodName())
                        && MethodType.methodType(method.getReturnType(), method.getParameterTypes())
                                .equals(frame.getMethodType())) {
                    return true;
                }
            }
            return false;
        });
    }

    private static void count(Scope s, Class<?> source, StackFrame repository,
                              List<StackFrame> frames, int testIndex) {
        String select = repository == null ? "" : repository.getMethodName();
        String repositoryMethod = repository == null ? "" : repositoryMethod(repository);
        Hit hit = s.counts.computeIfAbsent(source.getName() + "|" + repositoryMethod,
                k -> new Hit(source.getSimpleName(), select, repositoryMethod));
        hit.count++;
        hit.sites.add(trail(frames, testIndex));
        if (hit.count == 2) {
            s.repeats.add(hit);
        }
    }

    /** The test frames from the test method inward, up to the first frame of the unit under test. */
    private static String testPosition(List<StackFrame> frames, int testIndex, String testClass) {
        StringBuilder position = new StringBuilder();
        for (int i = testIndex; i >= 0; i--) {
            StackFrame frame = frames.get(i);
            Class<?> declaring = frame.getDeclaringClass();
            if (outermost(declaring).equals(testClass)) {
                position.append(frame.getMethodName()).append('@').append(frame.getByteCodeIndex()).append('>');
            } else if (declaring.getName().startsWith("com.naturalist.")) {
                break;
            }
        }
        return position.toString();
    }

    private static boolean isUnitFrame(Class<?> declaring) {
        String name = declaring.getName();
        return name.startsWith("com.naturalist.")
                && !name.startsWith("com.naturalist.data.")
                && !TestEntitySource.class.isAssignableFrom(declaring)
                && !isWrapperFrame(declaring);
    }

    private static boolean isWrapperFrame(Class<?> declaring) {
        return declaring.getNestHost() == SelectTrace.class || Proxy.isProxyClass(declaring)
                || Traced.class.isAssignableFrom(declaring);
    }

    private static List<StackTraceElement> trail(List<StackFrame> frames, int testIndex) {
        List<StackTraceElement> trail = new ArrayList<>();
        for (int i = 0; i <= testIndex; i++) {
            String name = frames.get(i).getClassName();
            if (name.startsWith("com.naturalist.") && !name.startsWith("com.naturalist.data.")
                    && !isWrapperFrame(frames.get(i).getDeclaringClass())) {
                trail.add(frames.get(i).toStackTraceElement());
            }
        }
        return trail;
    }

    private static String outermost(Class<?> type) {
        Class<?> current = type;
        while (current.getEnclosingClass() != null) {
            current = current.getEnclosingClass();
        }
        return current.getName();
    }

    private static boolean isTestMethod(StackFrame frame) {
        String key = frame.getClassName() + "#" + frame.getMethodName() + frame.getDescriptor();
        return TEST_METHODS.computeIfAbsent(key, k -> {
            for (Method method : frame.getDeclaringClass().getDeclaredMethods()) {
                if (method.getName().equals(frame.getMethodName()) && hasTestAnnotation(method)) {
                    return true;
                }
            }
            return false;
        });
    }

    private static boolean hasTestAnnotation(Method method) {
        for (Annotation annotation : method.getAnnotations()) {
            Class<? extends Annotation> type = annotation.annotationType();
            if (type == Test.class || type.isAnnotationPresent(TestTemplate.class)) {
                return true;
            }
        }
        return false;
    }

    /** Fails the test on any repeat, naming every line that made the read. */
    public static void evaluate(String testName) {
        StringBuilder out = new StringBuilder();
        Scope s = SCOPE.get();
        for (Hit hit : s.repeats) {
            if (s.writers.contains(hit.repositoryMethod)) {
                continue;
            }
            out.append("N+1 select: ").append(hit.source).append('.').append(hit.select)
                    .append(" read ").append(hit.count).append(" times by one call in ").append(testName).append('\n');
            hit.sites.forEach(trail -> {
                trail.forEach(element -> out.append("\tat ").append(element).append('\n'));
                out.append('\n');
            });
        }
        if (!out.isEmpty()) {
            throw new RepeatedSelectException(out.toString());
        }
    }

    private static final class Hit {
        final String source;
        final String select;
        final String repositoryMethod;
        final Set<List<StackTraceElement>> sites = new LinkedHashSet<>();
        int count;

        Hit(String source, String select, String repositoryMethod) {
            this.source = source;
            this.select = select;
            this.repositoryMethod = repositoryMethod;
        }
    }

    private static final class Scope {
        boolean armed;
        String position;
        final Map<String, Hit> counts = new LinkedHashMap<>();
        final List<Hit> repeats = new ArrayList<>();
        final Set<String> writers = new HashSet<>();
    }

    /** One cached wrapper: the target it wraps and its proxy, both held weakly. */
    private record Wrapper(WeakReference<Object> target, WeakReference<Object> proxy) {
    }

    /**
     * The proxy {@link #tracedPort} made for each (target, port), so a port a traced call returns is the same
     * instance every time. Weak keys let targets be collected; the proxy is held weakly too, because it
     * references its target. A WeakHashMap matches keys by equals, so a hit is used only when its target is the
     * same instance.
     */
    private static final Map<Object, Map<Class<?>, Wrapper>> WRAPPERS = java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

    /**
     * Marks a class a test graph generated to trace a concrete class it builds: each overridden method
     * enters through {@link TraceAdvice}, so a call from the test starts a new count.
     */
    public interface Traced {
    }

    /**
     * Entry of a {@link Traced} method: starts a new count when the test made this call, as
     * {@link #traced} does for a port.
     */
    public static void enterTraced() {
        if (SCOPE.get().armed && calledByTest()) {
            nextInvocation();
        }
    }

    /** The ByteBuddy advice inlined at the entry of every {@link Traced} method. */
    public static final class TraceAdvice {
        private TraceAdvice() {
        }

        @Advice.OnMethodEnter
        static void enter() {
            enterTraced();
        }
    }

    /** The handler of every proxy {@link #traced} makes, so {@link #isTraced} can tell them from other proxies. */
    private record Handler(Object target) implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return SelectTrace.invoke(target, proxy, method, args);
        }
    }

    /** Whether {@code value} is already traced: a {@link Traced} graph object or a proxy {@link #traced} made. */
    private static boolean isTraced(Object value) {
        return value instanceof Traced
                || Proxy.isProxyClass(value.getClass()) && Proxy.getInvocationHandler(value) instanceof Handler;
    }
}
