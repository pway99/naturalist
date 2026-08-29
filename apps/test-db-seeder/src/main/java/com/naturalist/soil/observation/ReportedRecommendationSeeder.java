package com.naturalist.soil.observation;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.persistence.MyBatisSupport;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import javax.sql.DataSource;

/** Seeds reported recommendations through the real RDBMS adapter (split-package with the rdbms module). */
public final class ReportedRecommendationSeeder {

    private ReportedRecommendationSeeder() {}

    public static void seed(DataSource dataSource, NaturalistDatabase database) {
        var entities = database.getNamed(ReportedRecommendationTestEntitySource.class).entityStream().toList();
        SqlSessionFactory factory = MyBatisSupport.sessionFactory(dataSource, ReportedRecommendationMapper.class);
        try (SqlSession session = factory.openSession(false)) {
            var repo = new ReportedRecommendationEntityRepositoryRdbms(session.getMapper(ReportedRecommendationMapper.class));
            entities.forEach(repo::insert);
            session.commit();
        }
    }
}
