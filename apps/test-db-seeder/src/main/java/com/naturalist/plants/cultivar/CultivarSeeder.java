package com.naturalist.plants.cultivar;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.persistence.MyBatisSupport;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import javax.sql.DataSource;

/** Seeds cultivars through the real RDBMS adapter (split-package with the rdbms module). */
public final class CultivarSeeder {

    private CultivarSeeder() {}

    public static void seed(DataSource dataSource, NaturalistDatabase database) {
        var entities = database.getNamed(PlantCultivarTestEntitySource.class).entityStream().toList();
        SqlSessionFactory factory = MyBatisSupport.sessionFactory(dataSource, CultivarMapper.class);
        try (SqlSession session = factory.openSession(false)) {
            var repo = new CultivarEntityRepositoryRdbms(session.getMapper(CultivarMapper.class));
            entities.forEach(repo::insert);
            session.commit();
        }
    }
}
