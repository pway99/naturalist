package com.naturalist.chemistry.compound;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.persistence.MyBatisSupport;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import javax.sql.DataSource;

/**
 * Seeds {@link Compound}s then {@link CompoundDepiction}s (FK order) in one session — the
 * depictions' nested-select sees the just-inserted, not-yet-committed compound rows. Split-package
 * with the rdbms module, so it reaches the package-private adapters/mappers directly.
 */
public final class CompoundSeeder {

    private CompoundSeeder() {}

    public static void seed(DataSource dataSource, NaturalistDatabase database) {
        var compounds = database.getNamed(CompoundTestEntitySource.class).entityStream().toList();
        var depictions = database.getNamed(CompoundDepictionTestEntitySource.class).entityStream().toList();

        SqlSessionFactory factory =
                MyBatisSupport.sessionFactory(dataSource, CompoundMapper.class, DepictionMapper.class);
        try (SqlSession session = factory.openSession(false)) {
            var compoundRepo = new CompoundEntityRepositoryRdbms(session.getMapper(CompoundMapper.class));
            compounds.forEach(compoundRepo::insert);

            var depictionRepo = new DepictionEntityRepositoryRdbms(session.getMapper(DepictionMapper.class));
            depictions.forEach(depictionRepo::insert);

            session.commit();
        }
    }
}
