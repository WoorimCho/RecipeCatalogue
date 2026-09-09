package com.example.recipecatalogue;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * {@code scanBasePackages} / {@code @EntityScan} / {@code @EnableJpaRepositories}
 * are widened to {@code com.example.cataloguecommon} so the shared Tag entity,
 * its repository, the {@code TagServiceImpl}, the exception advice and the
 * Zipkin sender config are picked up from the {@code catalogue-common} module.
 */
@SpringBootApplication(scanBasePackages = {
        "com.example.recipecatalogue",
        "com.example.cataloguecommon"
})
@EntityScan(basePackages = {
        "com.example.recipecatalogue",
        "com.example.cataloguecommon.tag"
})
@EnableJpaRepositories(basePackages = {
        "com.example.recipecatalogue.Repositories",
        "com.example.cataloguecommon.tag"
})
public class RecipeCatalogueApplication {

    public static void main(String[] args) {
        SpringApplication.run(RecipeCatalogueApplication.class, args);
    }

}
