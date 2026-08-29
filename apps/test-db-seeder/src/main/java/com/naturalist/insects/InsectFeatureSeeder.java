package com.naturalist.insects;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.persistence.MyBatisSupport;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import javax.sql.DataSource;

/** Seeds insect features through the real RDBMS adapter (split-package with the rdbms module). */
public final class InsectFeatureSeeder {

    private InsectFeatureSeeder() {}

    public static void seed(DataSource dataSource, NaturalistDatabase database) {
        var entities = database.getNamed(InsectFeatureTestEntitySource.class).entityStream().toList();
        SqlSessionFactory factory = MyBatisSupport.sessionFactory(dataSource, InsectFeatureMapper.class);
        try (SqlSession session = factory.openSession(false)) {
            var repo = new InsectFeatureEntityRepositoryRdbms(session.getMapper(InsectFeatureMapper.class));
            entities.forEach(repo::insert);
            session.commit();
        }
    }
}
