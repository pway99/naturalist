package com.naturalist.chemistry.product;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.persistence.MyBatisSupport;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import javax.sql.DataSource;

/**
 * Seeds {@link Product}s (after compounds are committed — {@code product_compound} references them).
 * Split-package with the rdbms module, so it reaches the package-private adapter/mapper directly.
 */
public final class ProductSeeder {

    private ProductSeeder() {}

    public static void seed(DataSource dataSource, NaturalistDatabase database) {
        var products = database.getNamed(ProductTestEntitySource.class).entityStream().toList();
        SqlSessionFactory factory = MyBatisSupport.sessionFactory(dataSource, ProductMapper.class);
        try (SqlSession session = factory.openSession(false)) {
            var repo = new ProductEntityRepositoryRdbms(session.getMapper(ProductMapper.class));
            products.forEach(repo::insert);
            session.commit();
        }
    }
}
