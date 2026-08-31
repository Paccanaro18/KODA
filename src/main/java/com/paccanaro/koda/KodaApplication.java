package com.paccanaro.koda;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class KodaApplication {

    public static void main(String[] args) {
        SpringApplication.run(KodaApplication.class, args);
    }

}
