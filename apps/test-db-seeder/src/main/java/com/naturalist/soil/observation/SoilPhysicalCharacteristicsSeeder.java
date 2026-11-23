package com.naturalist.soil.observation;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.persistence.MyBatisSupport;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import javax.sql.DataSource;

import java.util.List;

/** Seeds soil physical characteristics through the real RDBMS adapter (split-package with the rdbms module). */
public final class SoilPhysicalCharacteristicsSeeder {

    private SoilPhysicalCharacteristicsSeeder() {}

    public static void seed(DataSource dataSource, NaturalistDatabase database) {
        List<SoilPhysicalCharacteristics> entities = database.getNamed(SoilPhysicalCharacteristicsTestEntitySource.class).entityStream().toList();
        SqlSessionFactory factory = MyBatisSupport.sessionFactory(dataSource, SoilPhysicalCharacteristicsMapper.class);
        try (SqlSession session = factory.openSession(false)) {
            SoilPhysicalCharacteristicsEntityRepositoryRdbms repo = new SoilPhysicalCharacteristicsEntityRepositoryRdbms(session.getMapper(SoilPhysicalCharacteristicsMapper.class));
            entities.forEach(repo::insert);
            session.commit();
        }
    }
}
