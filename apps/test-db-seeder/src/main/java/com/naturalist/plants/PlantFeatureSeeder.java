package com.naturalist.plants;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.persistence.MyBatisSupport;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import javax.sql.DataSource;

/** Seeds plant features through the real RDBMS adapter (split-package with the rdbms module). */
public final class PlantFeatureSeeder {

    private PlantFeatureSeeder() {}

    public static void seed(DataSource dataSource, NaturalistDatabase database) {
        var entities = database.getNamed(PlantFeatureTestEntitySource.class).entityStream().toList();
        SqlSessionFactory factory = MyBatisSupport.sessionFactory(dataSource, PlantFeatureMapper.class);
        try (SqlSession session = factory.openSession(false)) {
            var repo = new PlantFeatureEntityRepositoryRdbms(session.getMapper(PlantFeatureMapper.class));
            entities.forEach(repo::insert);
            session.commit();
        }
    }
}
