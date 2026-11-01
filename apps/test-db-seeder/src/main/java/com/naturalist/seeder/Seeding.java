package com.naturalist.seeder;

import com.naturalist.persistence.test.SchemaApplier;
import com.naturalist.data.FileName;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Collection;
import java.nio.file.Path;
import java.nio.file.Files;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.io.IOException;

/** Shared helper for the per-domain seeders. */
final class Seeding {

    private Seeding() {}

    /** Drop + recreate a domain's tables from its {@code schema/*.sql} DDL resource. */
    static void applySchema(DataSource dataSource, String resource) {
        try (Connection connection = dataSource.getConnection()) {
            // SchemaApplier commits explicitly, so autoCommit must be off or Postgres rejects it.
            connection.setAutoCommit(false);
            SchemaApplier.applyResource(connection, resource);
        } catch (Exception e) {
            throw new IllegalStateException("Failed applying schema " + resource, e);
        }
    }

    /**
     * Copy each seed photo the seeded image rows name from the classpath resource
     * {@code <space>/images/<name>} into the image store's root ({@code naturalist.images.root},
     * default {@code data/images}, relative to the working directory) under {@code <space>/}, so the
     * app serves them from disk like any upload. A file already present is left alone; a name with no
     * classpath resource (a UUID-named upload row) is skipped.
     */
    static void copySeedPhotos(String space, Collection<FileName> names) {
        Path directory = Path.of(
                System.getProperty("naturalist.images.root", "data/images")).resolve(space);
        int copied = 0;
        int present = 0;
        int missing = 0;
        try {
            Files.createDirectories(directory);
            for (FileName name : names) {
                Path target = directory.resolve(name.value());
                if (Files.exists(target)) {
                    present++;
                    continue;
                }
                try (InputStream in = Seeding.class.getClassLoader()
                        .getResourceAsStream(space + "/images/" + name.value())) {
                    if (in == null) {
                        missing++;
                        continue;
                    }
                    Files.copy(in, target);
                    copied++;
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed copying " + space + " seed photos to " + directory, e);
        }
        System.out.println("Seed photos (" + space + "): copied " + copied + ", present " + present + ", missing " + missing);
    }
}
