package com.naturalist.console;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

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
