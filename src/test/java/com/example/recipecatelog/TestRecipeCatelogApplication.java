package com.example.recipecatelog;

import org.springframework.boot.SpringApplication;

public class TestRecipeCatelogApplication {

    public static void main(String[] args) {
        SpringApplication.from(RecipeCatelogApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
