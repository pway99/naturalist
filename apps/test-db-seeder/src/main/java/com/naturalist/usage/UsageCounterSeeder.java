package com.naturalist.usage;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.persistence.MyBatisSupport;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import javax.sql.DataSource;

/** Seeds {@link UsageCounter}s through the real RDBMS adapter (split-package with the rdbms module). */
public final class UsageCounterSeeder {

    private UsageCounterSeeder() {}

    public static void seed(DataSource dataSource, NaturalistDatabase database) {
        var counters = database.getNamed(UsageCounterTestEntitySource.class).entityStream().toList();
        SqlSessionFactory factory = MyBatisSupport.sessionFactory(dataSource, UsageCounterMapper.class);
        try (SqlSession session = factory.openSession(false)) {
            var repo = new UsageCounterEntityRepositoryRdbms(session.getMapper(UsageCounterMapper.class));
            counters.forEach(repo::insert);
            session.commit();
        }
    }
}
