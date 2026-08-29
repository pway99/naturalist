package com.naturalist.insects;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.persistence.MyBatisSupport;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import javax.sql.DataSource;

/** Seeds insect species through the real RDBMS adapter (split-package with the rdbms module). */
public final class InsectSpeciesSeeder {

    private InsectSpeciesSeeder() {}

    public static void seed(DataSource dataSource, NaturalistDatabase database) {
        var entities = database.getNamed(InsectSpeciesTestEntitySource.class).entityStream().toList();
        SqlSessionFactory factory = MyBatisSupport.sessionFactory(dataSource, InsectSpeciesMapper.class);
        try (SqlSession session = factory.openSession(false)) {
            var repo = new InsectSpeciesEntityRepositoryRdbms(session.getMapper(InsectSpeciesMapper.class));
            entities.forEach(repo::insert);
            session.commit();
        }
    }
}
