package com.naturalist.library;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.insects.InsectsEntityRefResolver;
import com.naturalist.persistence.MyBatisSupport;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import javax.sql.DataSource;

/**
 * Seeds {@link CitationAssociation}s — must run after {@link CitationSeeder} (the numeric
 * {@code citation_id} FK is nested-selected from the citation name). Lives in the domain's package
 * (split-package with the rdbms module) so it reaches the package-private adapter and mapper. The
 * adapter needs an {@link com.naturalist.library.EntityRefResolver} to serialize each association's
 * cross-domain subject; the app's production bean is the insects resolver, constructed directly here.
 */
public final class CitationAssociationSeeder {

    private CitationAssociationSeeder() {}

    public static void seed(DataSource dataSource, NaturalistDatabase database) {
        var associations = database.getNamed(CitationAssociationTestEntitySource.class).entityStream().toList();
        SqlSessionFactory factory = MyBatisSupport.sessionFactory(dataSource, CitationAssociationMapper.class);
        try (SqlSession session = factory.openSession(false)) {
            var repo = new CitationAssociationEntityRepositoryRdbms(
                    session.getMapper(CitationAssociationMapper.class), new InsectsEntityRefResolver());
            associations.forEach(repo::insert);
            session.commit();
        }
    }
}
