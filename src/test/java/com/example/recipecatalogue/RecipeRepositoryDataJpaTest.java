package com.example.recipecatalogue;

import com.example.recipecatalogue.Model.Recipe;
import com.example.cataloguecommon.tag.Tag;
import com.example.recipecatalogue.Repositories.RecipeRepository;
import com.example.recipecatalogue.Repositories.RecipeSpecifications;
import com.example.cataloguecommon.tag.TagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository-layer test against the real (Testcontainers) MySQL: the
 * auto-versioning helper, the tag-link lookup, and the "has all tags"
 * specification (one inner join per tag).
 */
@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = "internal-auth.enabled=false")
class RecipeRepositoryDataJpaTest {

    @Autowired
    RecipeRepository recipes;
    @Autowired
    TagRepository tags;

    private Tag thai;
    private Tag quick;

    @BeforeEach
    void setUp() {
        thai = tags.save(new Tag("cuisine:thai"));
        quick = tags.save(new Tag("quick"));

        Recipe padThai = new Recipe("Pad Thai");
        padThai.setCreator("woorim");
        padThai.addTag(thai);
        padThai.addTag(quick);
        recipes.save(padThai);

        Recipe curry = new Recipe("Green Curry");
        curry.setCreator("woorim");
        curry.addTag(thai);
        recipes.save(curry);
    }

    @Test
    void findMaxVersion_isZeroWhenAbsentThenTheHighest() {
        assertThat(recipes.findMaxVersion("Nonexistent", "woorim")).isZero();
        assertThat(recipes.findMaxVersion("Pad Thai", "woorim")).isEqualTo(1);
    }

    @Test
    void findByTagsIdIn_returnsRecipesCarryingAnyTag() {
        assertThat(recipes.findByTags_IdIn(List.of(quick.getId())))
                .extracting(Recipe::getName).containsExactly("Pad Thai");
    }

    @Test
    void hasAllTagsSpecification_requiresEveryTag() {
        var both = recipes.findAll(
                RecipeSpecifications.hasAllTags(List.of("cuisine:thai", "quick")), PageRequest.of(0, 10));
        assertThat(both.getTotalElements()).isEqualTo(1);
        assertThat(both.getContent()).singleElement()
                .extracting(Recipe::getName).isEqualTo("Pad Thai");

        var thaiOnly = recipes.findAll(
                RecipeSpecifications.hasAnyTag(List.of("cuisine:thai")), PageRequest.of(0, 10));
        assertThat(thaiOnly.getTotalElements()).isEqualTo(2);
    }
}
