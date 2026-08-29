package com.naturalist.plants;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.persistence.MyBatisSupport;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import javax.sql.DataSource;

/** Seeds plant observations through the real RDBMS adapter (split-package with the rdbms module). */
public final class PlantObservationSeeder {

    private PlantObservationSeeder() {}

    public static void seed(DataSource dataSource, NaturalistDatabase database) {
        var entities = database.getNamed(PlantObservationTestEntitySource.class).entityStream().toList();
        SqlSessionFactory factory = MyBatisSupport.sessionFactory(dataSource, PlantObservationMapper.class);
        try (SqlSession session = factory.openSession(false)) {
            var repo = new PlantObservationEntityRepositoryRdbms(session.getMapper(PlantObservationMapper.class));
            entities.forEach(repo::insert);
            session.commit();
        }
    }
}
