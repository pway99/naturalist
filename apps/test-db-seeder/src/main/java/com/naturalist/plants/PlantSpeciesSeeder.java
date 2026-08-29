package com.naturalist.plants;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.persistence.MyBatisSupport;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import javax.sql.DataSource;

/** Seeds plant species through the real RDBMS adapter (split-package with the rdbms module). */
public final class PlantSpeciesSeeder {

    private PlantSpeciesSeeder() {}

    public static void seed(DataSource dataSource, NaturalistDatabase database) {
        var entities = database.getNamed(PlantSpeciesTestEntitySource.class).entityStream().toList();
        SqlSessionFactory factory = MyBatisSupport.sessionFactory(dataSource, PlantSpeciesMapper.class);
        try (SqlSession session = factory.openSession(false)) {
            var repo = new PlantSpeciesEntityRepositoryRdbms(session.getMapper(PlantSpeciesMapper.class));
            entities.forEach(repo::insert);
            session.commit();
        }
    }
}
