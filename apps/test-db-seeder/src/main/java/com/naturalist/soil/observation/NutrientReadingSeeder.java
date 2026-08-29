package com.naturalist.soil.observation;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.persistence.MyBatisSupport;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import javax.sql.DataSource;

/** Seeds nutrient readings through the real RDBMS adapter (split-package with the rdbms module). */
public final class NutrientReadingSeeder {

    private NutrientReadingSeeder() {}

    public static void seed(DataSource dataSource, NaturalistDatabase database) {
        var entities = database.getNamed(NutrientReadingTestEntitySource.class).entityStream().toList();
        SqlSessionFactory factory = MyBatisSupport.sessionFactory(dataSource, NutrientReadingMapper.class);
        try (SqlSession session = factory.openSession(false)) {
            var repo = new NutrientReadingEntityRepositoryRdbms(session.getMapper(NutrientReadingMapper.class));
            entities.forEach(repo::insert);
            session.commit();
        }
    }
}
