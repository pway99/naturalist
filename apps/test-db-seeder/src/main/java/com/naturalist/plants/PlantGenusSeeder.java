package com.naturalist.plants;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.persistence.MyBatisSupport;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import javax.sql.DataSource;

/** Seeds plant genera through the real RDBMS adapter (split-package with the rdbms module). */
public final class PlantGenusSeeder {

    private PlantGenusSeeder() {}

    public static void seed(DataSource dataSource, NaturalistDatabase database) {
        var entities = database.getNamed(PlantGenusTestEntitySource.class).entityStream().toList();
        SqlSessionFactory factory = MyBatisSupport.sessionFactory(dataSource, PlantGenusMapper.class);
        try (SqlSession session = factory.openSession(false)) {
            var repo = new PlantGenusEntityRepositoryRdbms(session.getMapper(PlantGenusMapper.class));
            entities.forEach(repo::insert);
            session.commit();
        }
    }
}
