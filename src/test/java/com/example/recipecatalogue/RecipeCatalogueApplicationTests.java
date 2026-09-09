package com.example.recipecatalogue;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class RecipeCatalogueApplicationTests {

    @Test
    void contextLoads() {
    }

}
