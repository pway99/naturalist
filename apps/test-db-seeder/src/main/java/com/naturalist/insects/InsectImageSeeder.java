package com.naturalist.insects;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.persistence.MyBatisSupport;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import javax.sql.DataSource;

/** Seeds insect images through the real RDBMS adapter (split-package with the rdbms module). */
public final class InsectImageSeeder {

    private InsectImageSeeder() {}

    public static void seed(DataSource dataSource, NaturalistDatabase database) {
        var entities = database.getNamed(InsectImageTestEntitySource.class).entityStream().toList();
        SqlSessionFactory factory = MyBatisSupport.sessionFactory(dataSource, InsectImageMapper.class);
        try (SqlSession session = factory.openSession(false)) {
            var repo = new InsectImageEntityRepositoryRdbms(session.getMapper(InsectImageMapper.class));
            entities.forEach(repo::insert);
            session.commit();
        }
    }
}
