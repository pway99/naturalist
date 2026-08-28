package com.naturalist.naturalist;

import com.naturalist.persistence.MyBatisSupport;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import javax.sql.DataSource;
import java.util.Collection;

/** Public seed entrypoint: replays already-loaded naturalists + credentials through this
 *  module's rdbms adapters' insert(), FK-ordered (naturalists before credentials),
 *  committing once. Callers (the seeder app) load the JSON-backed TestEntitySources and
 *  pass the entities in — this module never depends on the test-fixture module itself. */
public final class NaturalistRdbmsSeed {

    private NaturalistRdbmsSeed() {}

    public static void seed(DataSource dataSource,
                             Collection<Naturalist> naturalists,
                             Collection<NaturalistCredential> credentials) {
        SqlSessionFactory factory = MyBatisSupport.sessionFactory(
                dataSource, NaturalistMapper.class, NaturalistCredentialMapper.class);

        try (SqlSession session = factory.openSession(false)) {
            var naturalistRepo = new NaturalistEntityRepositoryRdbms(session.getMapper(NaturalistMapper.class));
            naturalists.forEach(naturalistRepo::insert);

            var credentialRepo = new NaturalistCredentialRepositoryRdbms(session.getMapper(NaturalistCredentialMapper.class));
            credentials.forEach(credentialRepo::insert);

            session.commit();
        }
    }
}
