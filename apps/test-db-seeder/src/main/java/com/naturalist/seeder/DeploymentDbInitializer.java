package com.naturalist.seeder;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.secrets.FileSecretProvider;
import com.naturalist.secrets.SecretName;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Creates a deployment's database: every domain's schema, then the public catalog. Unlike
 * {@link TestDbSeeder} it never drops anything and loads no accounts, naturalists, garden or soil
 * records, so it is safe to run on every start of a deployment.
 *
 * <p>The database decides what happens. Empty, it is initialized and a marker row records
 * completion. Already marked, this exits successfully without touching it. Holding tables but no
 * marker, it may be live data or a failed earlier run, and this refuses and exits non-zero. Later
 * schema changes are out of scope: the project has no migration tool yet.
 *
 * <p>Connection settings are the system properties {@code naturalist.db.url} and
 * {@code naturalist.db.username}, deliberately with no default so it can never reach the standing
 * test database by accident. The credential is the {@code database-credential} file in
 * {@code naturalist.secrets.directory} (ADR-028). Seed photos go to {@code naturalist.images.root}.
 */
public final class DeploymentDbInitializer {
    private static final String CREDENTIAL = "database-credential";

    private static final String MARKER_TABLE = "deployment_initialization";

    /** In dependency order: {@code catalog} is last because its view reads the domains' views. */
    private static final List<String> SCHEMAS = List.of("naturalists", "accounts", "chemistry", "library", "plants", "insects", "garden", "soil", "usage", "catalog");

    private DeploymentDbInitializer() {
    }

    public static void main(String[] args) throws SQLException {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(requiredProperty("naturalist.db.url"));
        config.setUsername(requiredProperty("naturalist.db.username"));
        config.setPassword(new FileSecretProvider(Path.of(requiredProperty("naturalist.secrets.directory")))
                .require(SecretName.of(CREDENTIAL)).value());
        config.setMaximumPoolSize(4);
        config.setPoolName("naturalist-init");

        try (HikariDataSource dataSource = new HikariDataSource(config)) {
            switch (stateOf(dataSource)) {
                case INITIALIZED -> System.out.println("Database already initialized; nothing to do.");
                case NOT_EMPTY -> throw new IllegalStateException(
                        "Refusing to initialize: the database holds tables but no " + MARKER_TABLE
                                + " record, so it is either live data or a failed earlier run. Nothing was changed.");
                case EMPTY -> {
                    initialize(dataSource);
                    System.out.println("Database initialized.");
                }
            }
        }
    }

    private enum State {
        EMPTY,
        INITIALIZED,
        NOT_EMPTY
    }

    private static State stateOf(DataSource dataSource) throws SQLException {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            long marked = count(statement, "SELECT count(*) FROM information_schema.tables"
                    + " WHERE table_schema = 'public' AND table_name = '" + MARKER_TABLE + "'");
            if (marked > 0 && count(statement, "SELECT count(*) FROM " + MARKER_TABLE) > 0) {
                return State.INITIALIZED;
            }
            long tables = count(statement, "SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public'");
            return tables == 0 ? State.EMPTY : State.NOT_EMPTY;
        }
    }

    private static long count(Statement statement, String sql) throws SQLException {
        try (ResultSet rows = statement.executeQuery(sql)) {
            rows.next();
            return rows.getLong(1);
        }
    }

    private static void initialize(DataSource dataSource) throws SQLException {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            for (String schema : SCHEMAS) {
                for (String ddl : withoutDrops(read("schema/" + schema + ".sql"))) {
                    statement.execute(ddl);
                }
            }
        }

        NaturalistDatabase database = NaturalistDatabase.create();
        ChemistrySeeding.loadData(dataSource, database);
        LibrarySeeding.loadData(dataSource, database);
        PlantsSeeding.loadData(dataSource, database);
        InsectsSeeding.loadData(dataSource, database);
        UsageSeeding.loadData(dataSource, database);
        try {
            copySeedImages();
        } catch (IOException e) {
            throw new IllegalStateException("Failed copying the seed images", e);
        }

        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE " + MARKER_TABLE + " (completed_at TIMESTAMPTZ NOT NULL)");
            statement.execute("INSERT INTO " + MARKER_TABLE + " VALUES (now())");
        }
    }

    /** Splits on {@code ;} as {@code SchemaApplier} does, and leaves out every {@code DROP}. */
    private static List<String> withoutDrops(String sql) {
        return Arrays.stream(sql.split(";"))
                .filter(statement -> !statement.isBlank())
                .filter(statement -> !withoutLeadingComments(statement).toUpperCase(Locale.ROOT).startsWith("DROP "))
                .toList();
    }

    private static String withoutLeadingComments(String statement) {
        String remaining = statement.strip();
        while (remaining.startsWith("--")) {
            int lineEnd = remaining.indexOf('\n');
            remaining = lineEnd < 0 ? "" : remaining.substring(lineEnd + 1).strip();
        }
        return remaining;
    }

    private static String read(String resource) {
        try (InputStream in = DeploymentDbInitializer.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Schema resource not found: " + resource);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed reading " + resource, e);
        }
    }

    private static String requiredProperty(String name) {
        String value = System.getProperty(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("System property " + name + " must be set");
        }
        return value;
    }

    /**
     * Copies the catalog photos tracked under {@code data/images}, which the image ships at
     * {@code naturalist.seed-images}, into the image store. Runs only during initialization, so a photo
     * later removed from a deployment does not come back, and it never overwrites a file.
     */
    private static void copySeedImages() throws IOException {
        Path source = Path.of(requiredProperty("naturalist.seed-images"));
        Path target = Path.of(requiredProperty("naturalist.images.root"));
        try (Stream<Path> paths = Files.walk(source)) {
            for (Path path : (Iterable<Path>) paths::iterator) {
                Path destination = target.resolve(source.relativize(path).toString());
                if (Files.isDirectory(path)) {
                    Files.createDirectories(destination);
                } else if (!Files.exists(destination)) {
                    Files.copy(path, destination);
                }
            }
        }
    }
}
