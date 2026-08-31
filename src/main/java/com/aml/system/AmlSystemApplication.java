package com.aml.system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class AmlSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(AmlSystemApplication.class, args);
    }
}
