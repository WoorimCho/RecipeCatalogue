package com.example.recipecatalogue;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = "internal-auth.enabled=false")
@SpringBootTest
class RecipeCatalogueApplicationTests {

    @Test
    void contextLoads() {
    }

}
