package com.naturalist.seeder;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.naturalist.Naturalist;
import com.naturalist.naturalist.NaturalistRdbmsSeed;
import com.naturalist.naturalist.NaturalistTestEntitySource;

import javax.sql.DataSource;
import java.util.List;

/** Seeds the naturalists domain: its schema, then naturalists. */
final class NaturalistSeeding {

    private NaturalistSeeding() {}

    static void seed(DataSource dataSource, NaturalistDatabase database) {
        Seeding.applySchema(dataSource, "schema/naturalists.sql");

        List<Naturalist> naturalists =
                database.getNamed(NaturalistTestEntitySource.class).entityStream().toList();

        NaturalistRdbmsSeed.seed(dataSource, naturalists);
    }
}
