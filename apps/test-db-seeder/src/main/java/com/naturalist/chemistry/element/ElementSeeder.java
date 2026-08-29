package com.naturalist.chemistry.element;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.persistence.MyBatisSupport;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import javax.sql.DataSource;

/**
 * Seeds {@link Element}s. Lives in the domain's package (split-package with the rdbms module) so
 * it reaches the package-private adapter, mapper, and test-entity-source directly — nothing in the
 * rdbms module has to be public for the seeder.
 */
public final class ElementSeeder {

    private ElementSeeder() {}

    public static void seed(DataSource dataSource, NaturalistDatabase database) {
        var elements = database.getNamed(ElementTestEntitySource.class).entityStream().toList();
        SqlSessionFactory factory = MyBatisSupport.sessionFactory(dataSource, ElementMapper.class);
        try (SqlSession session = factory.openSession(false)) {
            var repo = new ElementEntityRepositoryRdbms(session.getMapper(ElementMapper.class));
            elements.forEach(repo::insert);
            session.commit();
        }
    }
}
