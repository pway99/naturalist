package com.naturalist.persistence;

import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;

import javax.sql.DataSource;

/** Builds a MyBatis {@link SqlSessionFactory} for the naturalist schema. Auto-mapping only:
 *  underscore-to-camel is OFF because DBO field names already equal the snake_case columns. */
public final class MyBatisSupport {

    private MyBatisSupport() {}

    public static SqlSessionFactory sessionFactory(DataSource dataSource, Class<?>... mappers) {
        Environment environment = new Environment("naturalist", new JdbcTransactionFactory(), dataSource);
        Configuration configuration = new Configuration(environment);
        configuration.setMapUnderscoreToCamelCase(true);
        for (Class<?> mapper : mappers) {
            configuration.addMapper(mapper);
        }
        return new SqlSessionFactoryBuilder().build(configuration);
    }
}
