package com.naturalist.plants.phytochemistry;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.persistence.MyBatisSupport;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import javax.sql.DataSource;

/** Seeds phytochemical constituents through the real RDBMS adapter (split-package with the rdbms module). */
public final class PhytochemicalConstituentSeeder {

    private PhytochemicalConstituentSeeder() {}

    public static void seed(DataSource dataSource, NaturalistDatabase database) {
        var entities = database.getNamed(PlantPhytochemicalConstituentTestEntitySource.class).entityStream().toList();
        SqlSessionFactory factory = MyBatisSupport.sessionFactory(dataSource, PhytochemicalConstituentMapper.class);
        try (SqlSession session = factory.openSession(false)) {
            var repo = new PhytochemicalConstituentEntityRepositoryRdbms(session.getMapper(PhytochemicalConstituentMapper.class));
            entities.forEach(repo::insert);
            session.commit();
        }
    }
}
