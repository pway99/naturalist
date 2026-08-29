package com.naturalist.library;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.persistence.MyBatisSupport;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import javax.sql.DataSource;

/**
 * Seeds {@link Concept}s. Lives in the domain's package (split-package with the rdbms module) so it
 * reaches the package-private adapter, mapper, and test-entity-source directly — nothing in the rdbms
 * module has to be public for the seeder.
 */
public final class ConceptSeeder {

    private ConceptSeeder() {}

    public static void seed(DataSource dataSource, NaturalistDatabase database) {
        var concepts = database.getNamed(ConceptTestEntitySource.class).entityStream().toList();
        SqlSessionFactory factory = MyBatisSupport.sessionFactory(dataSource, ConceptMapper.class);
        try (SqlSession session = factory.openSession(false)) {
            var repo = new ConceptEntityRepositoryRdbms(session.getMapper(ConceptMapper.class));
            concepts.forEach(repo::insert);
            session.commit();
        }
    }
}
