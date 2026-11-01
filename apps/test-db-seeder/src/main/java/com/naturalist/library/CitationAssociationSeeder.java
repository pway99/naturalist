package com.naturalist.library;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.persistence.MyBatisSupport;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import javax.sql.DataSource;
import java.util.List;

/**
 * Seeds {@link CitationAssociation}s — must run after {@link CitationSeeder} (the numeric
 * {@code citation_id} FK is nested-selected from the citation name). Lives in the domain's package
 * (split-package with the rdbms module) so it reaches the package-private adapter and mapper.
 *
 * <h2>Resolver must cover every domain the fixture holds</h2>
 * The adapter needs an {@link EntityRefResolver} to flatten each association's cross-domain
 * subject. This used to pass the insects resolver alone, which silently bound the seeder to
 * insects-only fixtures: a plant subject in {@code citation-associations.json} would load fine in
 * the mock stack and then throw here, mid-run, leaving the standing database half-seeded — the
 * commit at the end of this method rolls back only this seeder's own work, not the seeders that
 * already committed before it.
 *
 * <p>So it composes the same resolvers the mock source does, keyed by domain slug. Reaching into
 * {@code library-repository-test} for them is deliberate rather than expedient: this seeder's whole
 * job is pushing that module's catalog into Postgres, and it already reads its
 * {@code TestEntitySource} below. The app composes the production {@code @DomainService} beans
 * instead, via {@code CompositeEntityRefResolver}; an app may not depend on another app, so the two
 * cannot share one composite.
 */
public final class CitationAssociationSeeder {

    private CitationAssociationSeeder() {}

    public static void seed(DataSource dataSource, NaturalistDatabase database) {
        var associations = database.getNamed(CitationAssociationTestEntitySource.class).entityStream().toList();
        SqlSessionFactory factory = MyBatisSupport.sessionFactory(dataSource, CitationAssociationMapper.class);
        try (SqlSession session = factory.openSession(false)) {
            var repo = new CitationAssociationEntityRepositoryRdbms(
                    session.getMapper(CitationAssociationMapper.class),
                    new CompositeEntityRefResolverMock(List.of(
                            new InsectsEntityRefResolverMock(),
                            new PlantsEntityRefResolverMock())));
            associations.forEach(repo::insert);
            session.commit();
        }
    }
}
