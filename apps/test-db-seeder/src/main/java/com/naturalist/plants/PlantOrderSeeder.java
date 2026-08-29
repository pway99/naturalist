package com.naturalist.plants;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.persistence.MyBatisSupport;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import javax.sql.DataSource;

/** Seeds plant orders through the real RDBMS adapter (split-package with the rdbms module). */
public final class PlantOrderSeeder {

    private PlantOrderSeeder() {}

    public static void seed(DataSource dataSource, NaturalistDatabase database) {
        var entities = database.getNamed(PlantOrderTestEntitySource.class).entityStream().toList();
        SqlSessionFactory factory = MyBatisSupport.sessionFactory(dataSource, PlantOrderMapper.class);
        try (SqlSession session = factory.openSession(false)) {
            var repo = new PlantOrderEntityRepositoryRdbms(session.getMapper(PlantOrderMapper.class));
            entities.forEach(repo::insert);
            session.commit();
        }
    }
}
