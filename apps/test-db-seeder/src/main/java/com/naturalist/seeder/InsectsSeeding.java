package com.naturalist.seeder;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.insects.InsectFamilySeeder;
import com.naturalist.insects.InsectFeatureAssignmentSeeder;
import com.naturalist.insects.InsectFeatureSeeder;
import com.naturalist.insects.InsectFunctionalRoleSeeder;
import com.naturalist.insects.InsectGenusSeeder;
import com.naturalist.insects.InsectImageSeeder;
import com.naturalist.insects.InsectObservationSeeder;
import com.naturalist.insects.InsectOrderSeeder;
import com.naturalist.insects.InsectSpeciesSeeder;
import com.naturalist.insects.lifestage.InsectLifeStageSeeder;

import javax.sql.DataSource;

/**
 * Seeds the insects domain in foreign-key order: the rank chain (order→family→genus→species) first, then
 * features before their assignments, observations before the images that reference them; the cross-rank /
 * cross-domain refs (functional role, life stage) carry no FK and can seed any time. Each seeder commits its
 * own session, so a parent is durable before its child seeder runs.
 */
final class InsectsSeeding {

    private InsectsSeeding() {}

    static void seed(DataSource dataSource, NaturalistDatabase database) {
        Seeding.applySchema(dataSource, "schema/insects.sql");
        InsectOrderSeeder.seed(dataSource, database);
        InsectFamilySeeder.seed(dataSource, database);
        InsectGenusSeeder.seed(dataSource, database);
        InsectSpeciesSeeder.seed(dataSource, database);
        InsectFeatureSeeder.seed(dataSource, database);
        InsectFeatureAssignmentSeeder.seed(dataSource, database);
        InsectFunctionalRoleSeeder.seed(dataSource, database);
        InsectObservationSeeder.seed(dataSource, database);
        InsectImageSeeder.seed(dataSource, database);
        InsectLifeStageSeeder.seed(dataSource, database);
    }
}
