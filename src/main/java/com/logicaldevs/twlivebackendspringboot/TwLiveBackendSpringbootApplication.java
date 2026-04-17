package com.logicaldevs.twlivebackendspringboot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TwLiveBackendSpringbootApplication {

    public static void main(String[] args) {
        SpringApplication.run(TwLiveBackendSpringbootApplication.class, args);
    }

}
