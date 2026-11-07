package com.naturalist.test.context;

import com.naturalist.data.MockDomainService;
import com.naturalist.infrastructure.DomainService;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

/**
 * Finds the {@code @DomainService} and {@code @MockDomainService} classes of one Java package, in every
 * directory and jar on the class path that holds that package, and nowhere else. Each package is scanned
 * once per JVM.
 */
final class PackageScan {
    private static final Map<String, List<Class<?>>> CANDIDATES = new ConcurrentHashMap<>();

    private PackageScan() {
    }

    static List<Class<?>> candidates(String packageName) {
        return CANDIDATES.computeIfAbsent(packageName, PackageScan::scan);
    }

    private static List<Class<?>> scan(String packageName) {
        ClassLoader loader = loader();
        String path = packageName.replace('.', '/');
        Set<String> classNames = new TreeSet<>();
        try {
            Enumeration<URL> roots = loader.getResources(path);
            while (roots.hasMoreElements()) {
                URL root = roots.nextElement();
                switch (root.getProtocol()) {
                    case "file" -> listDirectory(Path.of(root.toURI()), packageName, classNames);
                    case "jar" -> listJar(root, path, packageName, classNames);
                    default -> throw new TestGraphException("cannot scan package " + packageName + " at " + root);
                }
            }
        } catch (IOException | URISyntaxException e) {
            throw new TestGraphException("cannot scan package " + packageName, e);
        }
        List<Class<?>> candidates = new ArrayList<>();
        List<String> unlinked = new ArrayList<>();
        for (String name : classNames) {
            Class<?> type;
            try {
                type = Class.forName(name, false, loader);
            } catch (ClassNotFoundException e) {
                throw new TestGraphException("listed but cannot load " + name, e);
            } catch (LinkageError e) {
                // its supertypes are not on this class path; kept so a resolution failure can name it
                unlinked.add(name + " (" + e + ")");
                continue;
            }
            if (type.isAnnotationPresent(DomainService.class) || type.isAnnotationPresent(MockDomainService.class)) {
                candidates.add(type);
            }
        }
        UNLINKED.put(packageName, List.copyOf(unlinked));
        return List.copyOf(candidates);
    }

    private static void listDirectory(Path directory, String packageName, Set<String> classNames) throws IOException {
        try (Stream<Path> files = Files.list(directory)) {
            files.filter(Files::isRegularFile)
                    .map(file -> file.getFileName().toString())
                    .filter(PackageScan::isClassFile)
                    .forEach(file -> classNames.add(packageName + "." + file.substring(0, file.length() - ".class".length())));
        }
    }

    private static void listJar(URL root, String path, String packageName, Set<String> classNames) throws IOException {
        JarURLConnection connection = (JarURLConnection) root.openConnection();
        connection.setUseCaches(false);
        String prefix = path + "/";
        try (JarFile jar = connection.getJarFile()) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if (name.startsWith(prefix) && name.indexOf('/', prefix.length()) < 0) {
                    String file = name.substring(prefix.length());
                    if (isClassFile(file)) {
                        classNames.add(packageName + "." + file.substring(0, file.length() - ".class".length()));
                    }
                }
            }
        }
    }

    private static boolean isClassFile(String file) {
        return file.endsWith(".class") && !file.equals("package-info.class") && !file.equals("module-info.class");
    }

    private static ClassLoader loader() {
        ClassLoader context = Thread.currentThread().getContextClassLoader();
        return context != null ? context : PackageScan.class.getClassLoader();
    }

    /** Per package, each listed class that failed to link here and why, so a later resolution failure can say so. */
    private static final Map<String, List<String>> UNLINKED = new ConcurrentHashMap<>();

    /**
     * For an error message: the classes of {@code packageName} that were listed but failed to link here, with
     * the error each threw, or the empty string when every listed class linked.
     */
    static String unlinked(String packageName) {
        candidates(packageName);
        List<String> unlinked = UNLINKED.getOrDefault(packageName, List.of());
        return unlinked.isEmpty() ? "" : "; listed but could not link " + unlinked;
    }
}
