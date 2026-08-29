package com.naturalist.plants;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.persistence.MyBatisSupport;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import javax.sql.DataSource;

/** Seeds plant images through the real RDBMS adapter (split-package with the rdbms module). */
public final class PlantImageSeeder {

    private PlantImageSeeder() {}

    public static void seed(DataSource dataSource, NaturalistDatabase database) {
        var entities = database.getNamed(PlantImageTestEntitySource.class).entityStream().toList();
        SqlSessionFactory factory = MyBatisSupport.sessionFactory(dataSource, PlantImageMapper.class);
        try (SqlSession session = factory.openSession(false)) {
            var repo = new PlantImageEntityRepositoryRdbms(session.getMapper(PlantImageMapper.class));
            entities.forEach(repo::insert);
            session.commit();
        }
    }
}
