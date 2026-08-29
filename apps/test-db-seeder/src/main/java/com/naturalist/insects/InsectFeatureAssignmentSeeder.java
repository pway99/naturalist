package com.naturalist.insects;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.persistence.MyBatisSupport;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import javax.sql.DataSource;

/** Seeds insect feature assignments through the real RDBMS adapter (split-package with the rdbms module). */
public final class InsectFeatureAssignmentSeeder {

    private InsectFeatureAssignmentSeeder() {}

    public static void seed(DataSource dataSource, NaturalistDatabase database) {
        var entities = database.getNamed(InsectFeatureAssignmentTestEntitySource.class).entityStream().toList();
        SqlSessionFactory factory = MyBatisSupport.sessionFactory(dataSource, InsectFeatureAssignmentMapper.class);
        try (SqlSession session = factory.openSession(false)) {
            var repo = new InsectFeatureAssignmentEntityRepositoryRdbms(session.getMapper(InsectFeatureAssignmentMapper.class));
            entities.forEach(repo::insert);
            session.commit();
        }
    }
}
