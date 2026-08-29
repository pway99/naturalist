package com.naturalist.plants;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.persistence.MyBatisSupport;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import javax.sql.DataSource;

/** Seeds plant families through the real RDBMS adapter (split-package with the rdbms module). */
public final class PlantFamilySeeder {

    private PlantFamilySeeder() {}

    public static void seed(DataSource dataSource, NaturalistDatabase database) {
        var entities = database.getNamed(PlantFamilyTestEntitySource.class).entityStream().toList();
        SqlSessionFactory factory = MyBatisSupport.sessionFactory(dataSource, PlantFamilyMapper.class);
        try (SqlSession session = factory.openSession(false)) {
            var repo = new PlantFamilyEntityRepositoryRdbms(session.getMapper(PlantFamilyMapper.class));
            entities.forEach(repo::insert);
            session.commit();
        }
    }
}
