package com.naturalist.insects;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.persistence.MyBatisSupport;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import javax.sql.DataSource;

/** Seeds insect observations through the real RDBMS adapter (split-package with the rdbms module). */
public final class InsectObservationSeeder {

    private InsectObservationSeeder() {}

    public static void seed(DataSource dataSource, NaturalistDatabase database) {
        var entities = database.getNamed(InsectObservationTestEntitySource.class).entityStream().toList();
        SqlSessionFactory factory = MyBatisSupport.sessionFactory(dataSource, InsectObservationMapper.class);
        try (SqlSession session = factory.openSession(false)) {
            var repo = new InsectObservationEntityRepositoryRdbms(session.getMapper(InsectObservationMapper.class));
            entities.forEach(repo::insert);
            session.commit();
        }
    }
}
