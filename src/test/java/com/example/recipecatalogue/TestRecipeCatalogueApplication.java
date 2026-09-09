package com.example.recipecatalogue;

import org.springframework.boot.SpringApplication;

public class TestRecipeCatalogueApplication {

    public static void main(String[] args) {
        SpringApplication.from(RecipeCatalogueApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
