package com.naturalist.insects;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.persistence.MyBatisSupport;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import javax.sql.DataSource;

/** Seeds insect orders through the real RDBMS adapter (split-package with the rdbms module). */
public final class InsectOrderSeeder {

    private InsectOrderSeeder() {}

    public static void seed(DataSource dataSource, NaturalistDatabase database) {
        var entities = database.getNamed(InsectOrderTestEntitySource.class).entityStream().toList();
        SqlSessionFactory factory = MyBatisSupport.sessionFactory(dataSource, InsectOrderMapper.class);
        try (SqlSession session = factory.openSession(false)) {
            var repo = new InsectOrderEntityRepositoryRdbms(session.getMapper(InsectOrderMapper.class));
            entities.forEach(repo::insert);
            session.commit();
        }
    }
}
