package com.naturalist.soil.observation;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.persistence.MyBatisSupport;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import javax.sql.DataSource;

/** Seeds reported optima through the real RDBMS adapter (split-package with the rdbms module). */
public final class ReportedOptimumSeeder {

    private ReportedOptimumSeeder() {}

    public static void seed(DataSource dataSource, NaturalistDatabase database) {
        var entities = database.getNamed(ReportedOptimumTestEntitySource.class).entityStream().toList();
        SqlSessionFactory factory = MyBatisSupport.sessionFactory(dataSource, ReportedOptimumMapper.class);
        try (SqlSession session = factory.openSession(false)) {
            var repo = new ReportedOptimumEntityRepositoryRdbms(session.getMapper(ReportedOptimumMapper.class));
            entities.forEach(repo::insert);
            session.commit();
        }
    }
}
