package com.naturalist.insects;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.observation.OrganismObservation;
import com.naturalist.persistence.MyBatisSupport;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import javax.sql.DataSource;

import java.util.List;

/** Seeds insect observations through the real RDBMS adapter (split-package with the rdbms module). */
public final class InsectObservationSeeder {

    private InsectObservationSeeder() {}

    public static void seed(DataSource dataSource, NaturalistDatabase database) {
        List<OrganismObservation<InsectObservationId, InsectRankName>> entities = database.getNamed(InsectObservationTestEntitySource.class).entityStream().toList();
        SqlSessionFactory factory = MyBatisSupport.sessionFactory(dataSource, InsectObservationMapper.class);
        try (SqlSession session = factory.openSession(false)) {
            InsectObservationEntityRepositoryRdbms repo = new InsectObservationEntityRepositoryRdbms(session.getMapper(InsectObservationMapper.class),
                    new InsectObservationTransaction(session.getMapper(InsectObservationMapper.class)));
            entities.forEach(repo::insert);
            session.commit();
        }
    }
}
