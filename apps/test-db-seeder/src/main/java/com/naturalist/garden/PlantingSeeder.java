package com.naturalist.garden;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.persistence.MyBatisSupport;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import javax.sql.DataSource;

/** Seeds {@link Planting}s through the real RDBMS adapter (split-package with the rdbms module). */
public final class PlantingSeeder {

    private PlantingSeeder() {}

    public static void seed(DataSource dataSource, NaturalistDatabase database) {
        var plantings = database.getNamed(PlantingTestEntitySource.class).entityStream().toList();
        SqlSessionFactory factory = MyBatisSupport.sessionFactory(dataSource, PlantingMapper.class);
        try (SqlSession session = factory.openSession(false)) {
            var repo = new PlantingEntityRepositoryRdbms(session.getMapper(PlantingMapper.class));
            plantings.forEach(repo::insert);
            session.commit();
        }
    }
}
