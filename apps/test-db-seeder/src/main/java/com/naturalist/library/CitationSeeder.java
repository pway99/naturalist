package com.naturalist.library;

import com.naturalist.authority.Citation;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.persistence.MyBatisSupport;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import javax.sql.DataSource;

import java.util.List;

/**
 * Seeds {@link com.naturalist.authority.Citation}s. Lives in the domain's package (split-package with
 * the rdbms module) so it reaches the package-private adapter, mapper, and test-entity-source directly.
 */
public final class CitationSeeder {

    private CitationSeeder() {}

    public static void seed(DataSource dataSource, NaturalistDatabase database) {
        List<Citation> citations = database.getNamed(CitationTestEntitySource.class).entityStream().toList();
        SqlSessionFactory factory = MyBatisSupport.sessionFactory(dataSource, CitationMapper.class);
        try (SqlSession session = factory.openSession(false)) {
            CitationEntityRepositoryRdbms repo = new CitationEntityRepositoryRdbms(session.getMapper(CitationMapper.class));
            citations.forEach(repo::insert);
            session.commit();
        }
    }
}
