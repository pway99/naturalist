package com.naturalist.naturalist;

import com.naturalist.persistence.MyBatisSupport;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import javax.sql.DataSource;
import java.util.Collection;

/** Public seed entrypoint: replays already-loaded naturalists through this module's rdbms
 *  adapter's insert(), committing once. Callers (the seeder app) load the JSON-backed
 *  TestEntitySource and pass the entities in — this module never depends on the
 *  test-fixture module itself. */
public final class NaturalistRdbmsSeed {

    private NaturalistRdbmsSeed() {}

    public static void seed(DataSource dataSource, Collection<Naturalist> naturalists) {
        SqlSessionFactory factory = MyBatisSupport.sessionFactory(dataSource, NaturalistMapper.class);

        try (SqlSession session = factory.openSession(false)) {
            var naturalistRepo = new NaturalistEntityRepositoryRdbms(session.getMapper(NaturalistMapper.class));
            naturalists.forEach(naturalistRepo::insert);

            session.commit();
        }
    }
}
