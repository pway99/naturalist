package com.naturalist.plants;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.persistence.MyBatisSupport;
import com.naturalist.taxonomy.OrganismFeatureAssignment;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import javax.sql.DataSource;

import java.util.List;

/** Seeds plant feature assignments through the real RDBMS adapter (split-package with the rdbms module). */
public final class PlantFeatureAssignmentSeeder {

    private PlantFeatureAssignmentSeeder() {
    }

    public static void seed(DataSource dataSource, NaturalistDatabase database) {
        List<OrganismFeatureAssignment<PlantFeatureAssignmentId, PlantFeatureId, PlantRankName>> entities = database.getNamed(PlantFeatureAssignmentTestEntitySource.class).entityStream().toList();
        SqlSessionFactory factory = MyBatisSupport.sessionFactory(dataSource, PlantFeatureAssignmentMapper.class);
        try (SqlSession session = factory.openSession(false)) {
            PlantFeatureAssignmentEntityRepositoryRdbms repo = new PlantFeatureAssignmentEntityRepositoryRdbms(session.getMapper(PlantFeatureAssignmentMapper.class));
            entities.forEach(repo::insert);
            session.commit();
        }
    }
}
