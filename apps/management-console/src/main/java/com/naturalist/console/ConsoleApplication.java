package com.naturalist.console;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Register the rdbms adapters' package-private @Mapper interfaces as beans so the
// scanned @DomainService repositories can be constructed. Filtered to @Mapper so the
// shared com.naturalist.naturalist package's non-mapper interfaces are not swept in.
@MapperScan(basePackages = "com.naturalist.naturalist", annotationClass = Mapper.class)
@SpringBootApplication(scanBasePackages = "com.naturalist")
public class ConsoleApplication {

    static void main(String[] args) {
        // Set BEFORE SpringApplication.run so TestEntitySource captures the value
        // when its class loads. See docs/plans/runtime-data-persistence.md for the
        // structural invariant: console writes flush to source-tree JSON; tests
        // never set this property and so see false for the test JVM's lifetime.
        System.setProperty("naturalist.persistence.enabled", "true");
        SpringApplication.run(ConsoleApplication.class, args);
    }
}
