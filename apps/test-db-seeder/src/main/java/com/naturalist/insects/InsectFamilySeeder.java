package com.naturalist.insects;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.persistence.MyBatisSupport;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import javax.sql.DataSource;

import java.util.List;

/** Seeds insect families through the real RDBMS adapter (split-package with the rdbms module). */
public final class InsectFamilySeeder {

    private InsectFamilySeeder() {}

    public static void seed(DataSource dataSource, NaturalistDatabase database) {
        List<InsectFamilyInfo> entities = database.getNamed(InsectFamilyTestEntitySource.class).entityStream().toList();
        SqlSessionFactory factory = MyBatisSupport.sessionFactory(dataSource, InsectFamilyMapper.class);
        try (SqlSession session = factory.openSession(false)) {
            InsectFamilyEntityRepositoryRdbms repo = new InsectFamilyEntityRepositoryRdbms(session.getMapper(InsectFamilyMapper.class),
                    new InsectFamilyTransaction(session.getMapper(InsectFamilyMapper.class)));
            entities.forEach(repo::insert);
            session.commit();
        }
    }
}
