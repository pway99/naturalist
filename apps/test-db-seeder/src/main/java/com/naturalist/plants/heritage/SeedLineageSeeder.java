package com.naturalist.plants.heritage;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.persistence.MyBatisSupport;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import javax.sql.DataSource;

/** Seeds seed lineages through the real RDBMS adapter (split-package with the rdbms module). */
public final class SeedLineageSeeder {

    private SeedLineageSeeder() {}

    public static void seed(DataSource dataSource, NaturalistDatabase database) {
        var entities = database.getNamed(PlantSeedLineageTestEntitySource.class).entityStream().toList();
        SqlSessionFactory factory = MyBatisSupport.sessionFactory(dataSource, SeedLineageMapper.class);
        try (SqlSession session = factory.openSession(false)) {
            var repo = new SeedLineageEntityRepositoryRdbms(session.getMapper(SeedLineageMapper.class));
            entities.forEach(repo::insert);
            session.commit();
        }
    }
}
