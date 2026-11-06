package com.naturalist.test.query.nofanout;

import com.naturalist.data.AbstractEntityRepository;
import com.naturalist.data.SuppressRepeatedSelect;
import com.naturalist.data.TestEntitySource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestTemplate;

import java.lang.StackWalker.StackFrame;
import java.lang.annotation.Annotation;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
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
 * <p>{@link #traced} sees that call start, so a test may loop over a command or query a test
 * context hands it. Without it, a loop with a single reading call in the test has the same stack as
 * a loop in the unit under test, so it fails too: batch the call or take the object from a context.
 *
 * <p>Not counted: reads under a write (foreign key checks), reads outside a test method, a test
 * calling a repository directly, continuation pages of a paged read, and any read made under a
 * method declared {@link SuppressRepeatedSelect}.
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
        return port.cast(Proxy.newProxyInstance(port.getClassLoader(), new Class<?>[]{port},
                (proxy, method, args) -> invoke(target, proxy, method, args)));
    }

    private static Object invoke(Object target, Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getDeclaringClass() == Object.class && method.getName().equals("equals")) {
            return proxy == args[0];
        }
        if (SCOPE.get().armed && calledByTest()) {
            nextInvocation();
        }
        Object result;
        try {
            result = method.invoke(target, args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
        return isPort(method.getReturnType()) && result != null ? tracedPort(method.getReturnType(), result) : result;
    }

    private static boolean isPort(Class<?> type) {
        return type.isInterface() && !type.isSealed() && type.getName().startsWith("com.naturalist.")
                && (type.getSimpleName().endsWith("Query") || type.getSimpleName().endsWith("Command"));
    }

    @SuppressWarnings("unchecked")
    private static <T> T tracedPort(Class<?> port, Object target) {
        return (T) traced((Class<Object>) port, target);
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
        String select = "";
        for (int i = 0; i < testIndex; i++) {
            StackFrame frame = frames.get(i);
            Class<?> declaring = frame.getDeclaringClass();
            if (AbstractEntityRepository.class.isAssignableFrom(declaring)) {
                select = frame.getMethodName();
            } else if (isUnitFrame(declaring)) {
                if (!outermost(declaring).equals(testClass)) {
                    site = frame;
                }
                break; // a test class here means the test called the repository directly
            }
        }
        if (site != null) {
            count(s, source, select, site, frames, testIndex);
        }
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

    private static void count(Scope s, Class<?> source, String select, StackFrame site,
                              List<StackFrame> frames, int testIndex) {
        Hit hit = s.counts.computeIfAbsent(source.getName() + "|" + select,
                k -> new Hit(source.getSimpleName(), select));
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
        return declaring == SelectTrace.class || Proxy.isProxyClass(declaring);
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
        for (Hit hit : SCOPE.get().repeats) {
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
        final Set<List<StackTraceElement>> sites = new LinkedHashSet<>();
        int count;

        Hit(String source, String select) {
            this.source = source;
            this.select = select;
        }
    }

    private static final class Scope {
        boolean armed;
        String position;
        final Map<String, Hit> counts = new LinkedHashMap<>();
        final List<Hit> repeats = new ArrayList<>();
    }
}
