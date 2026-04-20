package com.petgym;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PetGymApplication {
    public static void main(String[] args) {
        SpringApplication.run(PetGymApplication.class, args);
    }
}
