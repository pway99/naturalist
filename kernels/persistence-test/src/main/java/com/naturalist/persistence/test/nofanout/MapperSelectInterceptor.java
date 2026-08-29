package com.naturalist.persistence.test.nofanout;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

/**
 * MyBatis plugin that feeds {@link MapperSelectRecorder} — the select-side feeder of the RDBMS
 * mapper-select fan-out gate. It intercepts {@code Executor.query} (selects only; inserts and
 * updates route through {@code Executor.update} and are never seen), records one select against
 * the current repository head keyed by {@link MappedStatement#getId()}
 * (e.g. {@code com.naturalist.usage.UsageEventMapper.selectById}), then proceeds unchanged.
 *
 * <p>Never evaluates and never throws — the rule lives in
 * {@code com.naturalist.test.query.nofanout.SelectGate}, invoked from {@code RdbmsTestExtension}.
 * Registered only by that test extension via {@code Configuration.addInterceptor}; never on a
 * production {@code SqlSessionFactory}.
 */
@Intercepts(@Signature(
        type = Executor.class,
        method = "query",
        args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}))
public final class MapperSelectInterceptor implements Interceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        MappedStatement statement = (MappedStatement) invocation.getArgs()[0];
        MapperSelectRecorder.recordSelect(statement.getId());
        return invocation.proceed();
    }
}
