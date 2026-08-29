package com.naturalist.insects;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.persistence.MyBatisSupport;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import javax.sql.DataSource;

/** Seeds insect functional roles through the real RDBMS adapter (split-package with the rdbms module). */
public final class InsectFunctionalRoleSeeder {

    private InsectFunctionalRoleSeeder() {}

    public static void seed(DataSource dataSource, NaturalistDatabase database) {
        var entities = database.getNamed(InsectFunctionalRoleTestEntitySource.class).entityStream().toList();
        SqlSessionFactory factory = MyBatisSupport.sessionFactory(dataSource, InsectFunctionalRoleMapper.class);
        try (SqlSession session = factory.openSession(false)) {
            var repo = new InsectFunctionalRoleEntityRepositoryRdbms(session.getMapper(InsectFunctionalRoleMapper.class));
            entities.forEach(repo::insert);
            session.commit();
        }
    }
}
