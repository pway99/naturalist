package com.naturalist.insects.lifestage;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.persistence.MyBatisSupport;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import javax.sql.DataSource;

/** Seeds insect life stages through the real RDBMS adapter (split-package with the rdbms module). */
public final class InsectLifeStageSeeder {

    private InsectLifeStageSeeder() {}

    public static void seed(DataSource dataSource, NaturalistDatabase database) {
        var entities = database.getNamed(InsectLifeStageTestEntitySource.class).entityStream().toList();
        SqlSessionFactory factory = MyBatisSupport.sessionFactory(dataSource, InsectLifeStageMapper.class);
        try (SqlSession session = factory.openSession(false)) {
            var repo = new InsectLifeStageEntityRepositoryRdbms(session.getMapper(InsectLifeStageMapper.class));
            entities.forEach(repo::insert);
            session.commit();
        }
    }
}
