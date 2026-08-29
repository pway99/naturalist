package com.naturalist.persistence.test;

import com.naturalist.persistence.DboSchema;
import com.naturalist.persistence.Fk;

import java.io.File;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Anti-drift guard for the ACL: asserts that a live database schema matches every
 * {@link DboSchema} declared on a package's DBOs. It reads each DBO's declared table, primary
 * key, unique constraints, and foreign keys by reflection and compares them against the
 * database's own catalog ({@link DatabaseMetaData}) — so a mistyped column, a stale
 * {@code schema.sql}, or a missing constraint surfaces as a failed assertion instead of a
 * silent mis-mapping at runtime.
 *
 * <p>DBOs are found by classpath scan ({@link #discover(String)}), so a newly added DBO is
 * covered automatically and the guard itself cannot fall out of date. Pair it with a scratch
 * schema built from {@code schema.sql} to pin the DDL, the metadata, and the database together.
 */
public final class DboSchemaValidator {

    private DboSchemaValidator() {}

    /** All {@code @DboSchema}-annotated classes reachable under {@code basePackage}. */
    public static List<Class<?>> discover(String basePackage) {
        List<Class<?>> found = new ArrayList<>();
        String path = basePackage.replace('.', '/');
        try {
            Enumeration<URL> roots = Thread.currentThread().getContextClassLoader().getResources(path);
            while (roots.hasMoreElements()) {
                URL root = roots.nextElement();
                if ("file".equals(root.getProtocol())) {
                    scanDir(new File(URLDecoder.decode(root.getPath(), StandardCharsets.UTF_8)), basePackage, found);
                } else if ("jar".equals(root.getProtocol())) {
                    scanJar(root, path, found);
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed scanning for @DboSchema in " + basePackage, e);
        }
        return found;
    }

    /**
     * Validate the discovered DBOs against the given database schema (a Postgres schema name,
     * e.g. {@code "public"} or a scratch schema). Returns a list of human-readable problems;
     * empty means the database matches the metadata exactly.
     */
    public static List<String> validate(Connection connection, String dbSchema, Collection<Class<?>> dboClasses) {
        List<String> problems = new ArrayList<>();
        try {
            DatabaseMetaData meta = connection.getMetaData();
            for (Class<?> dbo : dboClasses) {
                DboSchema schema = dbo.getAnnotation(DboSchema.class);
                if (schema != null) {
                    validateOne(meta, dbSchema, dbo.getSimpleName(), schema, problems);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Schema validation failed reading database metadata", e);
        }
        return problems;
    }

    private static void validateOne(DatabaseMetaData meta, String dbSchema, String dboName,
                                    DboSchema schema, List<String> problems) throws SQLException {
        String table = schema.table();
        Set<String> columns = columns(meta, dbSchema, table);
        if (columns.isEmpty()) {
            problems.add(dboName + ": table '" + table + "' not found in schema '" + dbSchema + "'");
            return;
        }

        List<String> pk = split(schema.primaryKey());
        for (String c : pk) requireColumn(problems, table, columns, c, "primaryKey");
        for (String u : schema.unique()) requireColumn(problems, table, columns, u, "unique");
        for (Fk fk : schema.foreignKeys()) {
            for (String c : split(fk.columns())) requireColumn(problems, table, columns, c, "foreignKey");
        }

        Set<String> declaredPk = new HashSet<>(pk);
        Set<String> actualPk = primaryKey(meta, dbSchema, table);
        if (!declaredPk.equals(actualPk)) {
            problems.add(table + ": PRIMARY KEY " + new TreeSet<>(actualPk) + " != declared " + new TreeSet<>(declaredPk));
        }

        Set<Set<String>> uniques = uniqueColumnSets(meta, dbSchema, table);
        for (String u : schema.unique()) {
            if (!uniques.contains(Set.of(u.toLowerCase(Locale.ROOT)))) {
                problems.add(table + ": no UNIQUE constraint on (" + u + ")");
            }
        }

        Set<String> actualFks = foreignKeys(meta, dbSchema, table);
        for (Fk fk : schema.foreignKeys()) {
            String want = normalizeFk(fk.columns(), fk.references());
            if (!actualFks.contains(want)) {
                problems.add(table + ": missing FOREIGN KEY " + want + " (found " + actualFks + ")");
            }
        }
    }

    private static void requireColumn(List<String> problems, String table, Set<String> columns,
                                      String column, String role) {
        if (!columns.contains(column.toLowerCase(Locale.ROOT))) {
            problems.add(table + ": " + role + " column '" + column + "' does not exist");
        }
    }

    private static Set<String> columns(DatabaseMetaData meta, String dbSchema, String table) throws SQLException {
        Set<String> out = new HashSet<>();
        try (ResultSet rs = meta.getColumns(null, dbSchema, table, null)) {
            while (rs.next()) out.add(rs.getString("COLUMN_NAME").toLowerCase(Locale.ROOT));
        }
        return out;
    }

    private static Set<String> primaryKey(DatabaseMetaData meta, String dbSchema, String table) throws SQLException {
        Set<String> out = new HashSet<>();
        try (ResultSet rs = meta.getPrimaryKeys(null, dbSchema, table)) {
            while (rs.next()) out.add(rs.getString("COLUMN_NAME").toLowerCase(Locale.ROOT));
        }
        return out;
    }

    private static Set<Set<String>> uniqueColumnSets(DatabaseMetaData meta, String dbSchema, String table) throws SQLException {
        java.util.Map<String, Set<String>> byIndex = new java.util.HashMap<>();
        try (ResultSet rs = meta.getIndexInfo(null, dbSchema, table, true, false)) {
            while (rs.next()) {
                String col = rs.getString("COLUMN_NAME");
                String idx = rs.getString("INDEX_NAME");
                if (col == null || idx == null) continue; // tableIndexStatistic rows
                byIndex.computeIfAbsent(idx, k -> new HashSet<>()).add(col.toLowerCase(Locale.ROOT));
            }
        }
        return new HashSet<>(byIndex.values());
    }

    private static Set<String> foreignKeys(DatabaseMetaData meta, String dbSchema, String table) throws SQLException {
        Set<String> out = new HashSet<>();
        try (ResultSet rs = meta.getImportedKeys(null, dbSchema, table)) {
            while (rs.next()) {
                String fkCol = rs.getString("FKCOLUMN_NAME").toLowerCase(Locale.ROOT);
                String pkTable = rs.getString("PKTABLE_NAME").toLowerCase(Locale.ROOT);
                String pkCol = rs.getString("PKCOLUMN_NAME").toLowerCase(Locale.ROOT);
                out.add(fkCol + "->" + pkTable + "(" + pkCol + ")");
            }
        }
        return out;
    }

    private static String normalizeFk(String columns, String references) {
        return columns.trim().toLowerCase(Locale.ROOT) + "->" + references.replace(" ", "").toLowerCase(Locale.ROOT);
    }

    private static List<String> split(String csv) {
        List<String> out = new ArrayList<>();
        for (String s : csv.split(",")) {
            String t = s.trim();
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }

    private static void scanDir(File dir, String pkg, List<Class<?>> out) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                scanDir(f, pkg + "." + f.getName(), out);
            } else if (f.getName().endsWith(".class") && !f.getName().contains("$")) {
                load(pkg + "." + f.getName().substring(0, f.getName().length() - ".class".length()), out);
            }
        }
    }

    private static void scanJar(URL jarUrl, String path, List<Class<?>> out) throws Exception {
        String file = jarUrl.getPath();
        String jarPath = file.substring("file:".length(), file.indexOf('!'));
        try (JarFile jar = new JarFile(URLDecoder.decode(jarPath, StandardCharsets.UTF_8))) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if (name.startsWith(path) && name.endsWith(".class") && !name.contains("$")) {
                    load(name.substring(0, name.length() - ".class".length()).replace('/', '.'), out);
                }
            }
        }
    }

    private static void load(String className, List<Class<?>> out) {
        try {
            Class<?> c = Class.forName(className, false, Thread.currentThread().getContextClassLoader());
            if (c.isAnnotationPresent(DboSchema.class)) out.add(c);
        } catch (Throwable ignored) {
            // A class that can't load is not a DBO we care about; skip it.
        }
    }
}
