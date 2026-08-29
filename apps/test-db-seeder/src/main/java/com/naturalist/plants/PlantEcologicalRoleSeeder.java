package com.naturalist.plants;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.persistence.MyBatisSupport;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import javax.sql.DataSource;

/** Seeds plant ecological roles through the real RDBMS adapter (split-package with the rdbms module). */
public final class PlantEcologicalRoleSeeder {

    private PlantEcologicalRoleSeeder() {}

    public static void seed(DataSource dataSource, NaturalistDatabase database) {
        var entities = database.getNamed(PlantEcologicalRoleTestEntitySource.class).entityStream().toList();
        SqlSessionFactory factory = MyBatisSupport.sessionFactory(dataSource, PlantEcologicalRoleMapper.class);
        try (SqlSession session = factory.openSession(false)) {
            var repo = new PlantEcologicalRoleEntityRepositoryRdbms(session.getMapper(PlantEcologicalRoleMapper.class));
            entities.forEach(repo::insert);
            session.commit();
        }
    }
}
