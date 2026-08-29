package com.naturalist.library;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.persistence.MyBatisSupport;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import javax.sql.DataSource;

/**
 * Seeds {@link GlossaryTerm}s. Lives in the domain's package (split-package with the rdbms module) so
 * it reaches the package-private adapter, mapper, and test-entity-source directly.
 */
public final class GlossaryTermSeeder {

    private GlossaryTermSeeder() {}

    public static void seed(DataSource dataSource, NaturalistDatabase database) {
        var terms = database.getNamed(GlossaryTermTestEntitySource.class).entityStream().toList();
        SqlSessionFactory factory = MyBatisSupport.sessionFactory(dataSource, GlossaryTermMapper.class);
        try (SqlSession session = factory.openSession(false)) {
            var repo = new GlossaryTermEntityRepositoryRdbms(session.getMapper(GlossaryTermMapper.class));
            terms.forEach(repo::insert);
            session.commit();
        }
    }
}
