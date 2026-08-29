package com.naturalist.seeder;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.library.CitationAssociationSeeder;
import com.naturalist.library.CitationSeeder;
import com.naturalist.library.ConceptSeeder;
import com.naturalist.library.GlossaryTermSeeder;

import javax.sql.DataSource;

/**
 * Seeds the library domain: the three flat entities (concepts, glossary terms, citations) in any
 * order, then citation associations last — those carry a within-library {@code citation_id} FK, so
 * citations must exist first. Each seeder lives in the entity's own package (split-package with the
 * rdbms module) and this orchestrator only sequences them.
 */
final class LibrarySeeding {

    private LibrarySeeding() {}

    static void seed(DataSource dataSource, NaturalistDatabase database) {
        Seeding.applySchema(dataSource, "schema/library.sql");
        ConceptSeeder.seed(dataSource, database);
        GlossaryTermSeeder.seed(dataSource, database);
        CitationSeeder.seed(dataSource, database);
        CitationAssociationSeeder.seed(dataSource, database);
    }
}
