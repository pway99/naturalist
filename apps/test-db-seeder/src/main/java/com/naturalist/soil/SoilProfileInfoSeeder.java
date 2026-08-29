package com.naturalist.soil;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.persistence.MyBatisSupport;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import javax.sql.DataSource;

/** Seeds soil profiles through the real RDBMS adapter (split-package with the rdbms module). */
public final class SoilProfileInfoSeeder {

    private SoilProfileInfoSeeder() {}

    public static void seed(DataSource dataSource, NaturalistDatabase database) {
        var entities = database.getNamed(SoilProfileInfoTestEntitySource.class).entityStream().toList();
        SqlSessionFactory factory = MyBatisSupport.sessionFactory(dataSource, SoilProfileInfoMapper.class);
        try (SqlSession session = factory.openSession(false)) {
            var repo = new SoilProfileInfoEntityRepositoryRdbms(session.getMapper(SoilProfileInfoMapper.class));
            entities.forEach(repo::insert);
            session.commit();
        }
    }
}
