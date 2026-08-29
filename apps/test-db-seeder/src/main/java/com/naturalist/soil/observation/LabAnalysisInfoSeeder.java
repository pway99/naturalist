package com.naturalist.soil.observation;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.persistence.MyBatisSupport;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import javax.sql.DataSource;

/** Seeds lab analyses through the real RDBMS adapter (split-package with the rdbms module). */
public final class LabAnalysisInfoSeeder {

    private LabAnalysisInfoSeeder() {}

    public static void seed(DataSource dataSource, NaturalistDatabase database) {
        var entities = database.getNamed(LabAnalysisInfoTestEntitySource.class).entityStream().toList();
        SqlSessionFactory factory = MyBatisSupport.sessionFactory(dataSource, LabAnalysisInfoMapper.class);
        try (SqlSession session = factory.openSession(false)) {
            var repo = new LabAnalysisInfoEntityRepositoryRdbms(session.getMapper(LabAnalysisInfoMapper.class));
            entities.forEach(repo::insert);
            session.commit();
        }
    }
}
