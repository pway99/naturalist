package com.naturalist.insects;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.persistence.MyBatisSupport;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import javax.sql.DataSource;

import java.util.List;

/** Seeds insect genera through the real RDBMS adapter (split-package with the rdbms module). */
public final class InsectGenusSeeder {

    private InsectGenusSeeder() {}

    public static void seed(DataSource dataSource, NaturalistDatabase database) {
        List<InsectGenusInfo> entities = database.getNamed(InsectGenusTestEntitySource.class).entityStream().toList();
        SqlSessionFactory factory = MyBatisSupport.sessionFactory(dataSource, InsectGenusMapper.class);
        try (SqlSession session = factory.openSession(false)) {
            InsectGenusEntityRepositoryRdbms repo = new InsectGenusEntityRepositoryRdbms(session.getMapper(InsectGenusMapper.class),
                    new InsectGenusTransaction(session.getMapper(InsectGenusMapper.class)));
            entities.forEach(repo::insert);
            session.commit();
        }
    }
}
