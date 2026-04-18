package com.naturalist.console;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.naturalist")
public class ConsoleApplication {

    static void main(String[] args) {
        SpringApplication.run(ConsoleApplication.class, args);
    }
}
