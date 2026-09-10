package com.example.recipecatalogue;

import com.example.cataloguecommon.tag.Tag;
import com.example.cataloguecommon.tag.TagRepository;
import com.example.recipecatalogue.Dto.RecipeResponse;
import com.example.recipecatalogue.Model.IngredientReplacement;
import com.example.recipecatalogue.Model.Recipe;
import com.example.recipecatalogue.Model.RecipeIngredient;
import com.example.recipecatalogue.Model.RecipeStep;
import com.example.recipecatalogue.Repositories.RecipeRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the recipe-list N+1 fix ({@code hibernate.default_batch_fetch_size}).
 * {@link RecipeResponse#from} pulls four lazy collections per row; batching keeps
 * the whole page to a handful of {@code select ... where x in (?, ?)} queries
 * instead of one per row per collection. If the setting is dropped this count
 * jumps into the dozens and the test fails.
 */
@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true",
        "spring.jpa.properties.hibernate.generate_statistics=true"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = "internal-auth.enabled=false")
class RecipeListQueryCountDataJpaTest {

    private static final int RECIPES = 8;

    @Autowired
    RecipeRepository recipes;
    @Autowired
    TagRepository tags;
    @Autowired
    EntityManagerFactory emf;
    @PersistenceContext
    EntityManager em;

    @BeforeEach
    void seed() {
        Tag quick = tags.save(new Tag("quick"));
        Tag baking = tags.save(new Tag("category:baking"));

        for (int r = 0; r < RECIPES; r++) {
            Recipe recipe = new Recipe("Recipe " + r);
            recipe.setCreator("woorim");
            recipe.addTag(quick);
            recipe.addTag(baking);

            RecipeStep s1 = new RecipeStep("Mix everything for recipe " + r, Set.of("bowl", "whisk"));
            RecipeStep s2 = new RecipeStep("Bake recipe " + r, Set.of("oven"));
            recipe.replaceSteps(List.of(s1, s2));

            RecipeIngredient flour = new RecipeIngredient(100L + r);
            RecipeIngredient butter = new RecipeIngredient(200L + r);
            butter.setReplaceable(true);
            butter.replaceReplacements(List.of(
                    new IngredientReplacement(900L + r, null),
                    new IngredientReplacement(901L + r, null)));
            RecipeIngredient sugar = new RecipeIngredient(300L + r);
            recipe.replaceIngredients(List.of(flour, butter, sugar));

            recipes.save(recipe);
        }
        em.flush();
        em.clear();
    }

    @Test
    void listPageStaysFlatInQueryCount() {
        Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();

        List<RecipeResponse> mapped = recipes.findAll(PageRequest.of(0, RECIPES))
                .map(RecipeResponse::from)
                .getContent();

        long queries = stats.getPrepareStatementCount();

        assertThat(mapped).hasSize(RECIPES);
        assertThat(mapped).allSatisfy(dto -> {
            assertThat(dto.steps()).hasSize(2);
            assertThat(dto.ingredients()).hasSize(3);
            assertThat(dto.tags()).hasSize(2);
        });
        // 1 page query + a bounded number of batched collection loads (steps,
        // step tools, ingredients, replacements, tags). Unbatched this is ~40+.
        assertThat(queries)
                .as("batched collection loads, not one select per row per collection")
                .isLessThanOrEqualTo(12);
    }
}
