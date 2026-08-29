package com.naturalist.persistence.test.nofanout;

import org.apache.ibatis.builder.StaticSqlSource;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MapperSelectInterceptorTest {

    @Test
    void recordsStatementIdAgainstCurrentHeadAndProceeds() throws Throwable {
        Configuration cfg = new Configuration();
        SqlSource src = new StaticSqlSource(cfg, "select 1");
        MappedStatement ms = new MappedStatement
                .Builder(cfg, "com.example.FooMapper.selectThing", src, SqlCommandType.SELECT)
                .build();

        Executor target = (Executor) Proxy.newProxyInstance(
                Executor.class.getClassLoader(),
                new Class[]{Executor.class},
                (proxy, method, args) -> "query".equals(method.getName()) ? List.of("row") : null);

        Method query = Executor.class.getMethod(
                "query", MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class);
        Invocation invocation = new Invocation(
                target, query, new Object[]{ms, null, RowBounds.DEFAULT, null});

        MapperSelectRecorder.arm();
        MapperSelectRecorder.enterRepository("TestHead");
        try {
            Object result = new MapperSelectInterceptor().intercept(invocation);
            assertThat(result).isEqualTo(List.of("row"));
        } finally {
            MapperSelectRecorder.exitRepository();
        }

        assertThat(MapperSelectRecorder.snapshot())
                .containsEntry("TestHead", Map.of("com.example.FooMapper.selectThing", 1));
        MapperSelectRecorder.disarm();
    }
}
