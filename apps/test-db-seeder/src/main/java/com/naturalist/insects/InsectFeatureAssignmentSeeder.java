package com.naturalist.insects;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.persistence.MyBatisSupport;
import com.naturalist.taxonomy.OrganismFeatureAssignment;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import javax.sql.DataSource;

import java.util.List;

/** Seeds insect feature assignments through the real RDBMS adapter (split-package with the rdbms module). */
public final class InsectFeatureAssignmentSeeder {

    private InsectFeatureAssignmentSeeder() {
    }

    public static void seed(DataSource dataSource, NaturalistDatabase database) {
        List<OrganismFeatureAssignment<InsectFeatureAssignmentId, InsectFeatureId, InsectRankName>> entities = database.getNamed(InsectFeatureAssignmentTestEntitySource.class).entityStream().toList();
        SqlSessionFactory factory = MyBatisSupport.sessionFactory(dataSource, InsectFeatureAssignmentMapper.class);
        try (SqlSession session = factory.openSession(false)) {
            InsectFeatureAssignmentEntityRepositoryRdbms repo = new InsectFeatureAssignmentEntityRepositoryRdbms(session.getMapper(InsectFeatureAssignmentMapper.class));
            entities.forEach(repo::insert);
            session.commit();
        }
    }
}
