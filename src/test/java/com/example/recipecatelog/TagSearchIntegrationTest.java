package com.example.recipecatelog;

import com.example.recipecatelog.Repositories.RecipeRepository;
import com.example.recipecatelog.Repositories.TagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Drives the real HTTP endpoints against a Testcontainers MySQL: the full recipe
 * model (structured steps + tools, optional/replaceable ingredients with
 * substitutes), name/creator/version variations, and search by name / ingredient
 * / tag. Body assertions go through the repositories or plain substring checks to
 * stay independent of the JSON library version.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class TagSearchIntegrationTest {

    private static final Pattern TOTAL = Pattern.compile("\"totalElements\":(\\d+)");
    private static final Pattern VERSION = Pattern.compile("\"version\":(\\d+)");
    private static final Pattern ID = Pattern.compile("\"id\":(\\d+)");

    @Autowired
    MockMvc mvc;
    @Autowired
    RecipeRepository recipeRepository;
    @Autowired
    TagRepository tagRepository;

    @BeforeEach
    void reset() {
        recipeRepository.deleteAll();
        tagRepository.deleteAll();
    }

    @Test
    void createRoundTripsTheFullModel() throws Exception {
        String body = """
                {
                  "name": "Pad Thai",
                  "steps": [
                    {"text": "Soak the noodles.", "tools": ["bowl"]},
                    {"text": "Stir-fry everything.", "tools": ["wok", "spatula"]}
                  ],
                  "ingredients": [
                    {"ingredientId": 10, "quantity": "200g"},
                    {"ingredientId": 11, "optional": true},
                    {"ingredientId": 12, "replaceable": true,
                     "replacements": [{"ingredientId": 99, "recipeId": 7}]}
                  ],
                  "tags": ["Cuisine:Thai", "difficulty:easy"]
                }""";
        String created = mvc.perform(post("/api/recipes").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        assertThat(created).contains("\"creator\":\"anonymous\"", "\"version\":1");
        assertThat(created).contains("Stir-fry everything.").contains("\"wok\"");
        assertThat(created).contains("\"optional\":true").contains("\"replaceable\":true");
        assertThat(created).contains("\"ingredientId\":99").contains("\"recipeId\":7");
        assertThat(created).contains("cuisine:thai").contains("difficulty:easy");

        long id = firstMatch(ID, created);
        String fetched = mvc.perform(get("/api/recipes/" + id))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(fetched).contains("Soak the noodles.").contains("\"200g\"");
    }

    @Test
    void replaceableIngredientWithoutReplacementsIsRejected() throws Exception {
        mvc.perform(post("/api/recipes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Bad\",\"ingredients\":[{\"ingredientId\":1,\"replaceable\":true}]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sameNameSameCreatorGetsNextVersion() throws Exception {
        String v1 = mvc.perform(post("/api/recipes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Focaccia\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String v2 = mvc.perform(post("/api/recipes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Focaccia\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

        assertThat(firstMatch(VERSION, v1)).isEqualTo(1);
        assertThat(firstMatch(VERSION, v2)).isEqualTo(2);
        assertThat(recipeRepository.count()).isEqualTo(2);
    }

    @Test
    void searchByNameAndByIngredient() throws Exception {
        postRecipe("Peanut Curry", 10, 20);
        postRecipe("Peanut Noodles", 10, 30);
        postRecipe("Plain Rice", 40);

        assertThat(total("?name=peanut")).isEqualTo(2);
        assertThat(total("?ingredientId=20")).isEqualTo(1);
        assertThat(total("?name=peanut&ingredientId=30")).isEqualTo(1);
    }

    @Test
    void byIdsReturnsOnlyTheRecipesThatExist() throws Exception {
        create("Alpha");
        create("Beta");
        long alpha = recipeRepository.findAll().iterator().next().getId();

        String body = mvc.perform(get("/api/recipes/by-ids?id=" + alpha + "&id=999999"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).contains("Alpha").doesNotContain("999999").startsWith("[").endsWith("]");
    }

    @Test
    void searchMatchAllVersusAny() throws Exception {
        create("Pad Thai", "cuisine:thai", "quick");
        create("Green Curry", "cuisine:thai");
        create("Quick Toast", "quick");

        assertThat(total("?tag=cuisine:thai&tag=quick")).isEqualTo(1);
        assertThat(total("?tag=cuisine:thai&tag=quick&match=any")).isEqualTo(3);
        assertThat(total("?tag=cuisine:french")).isZero();
    }

    @Test
    void mergeMovesLinksThenDeletesSourceTag() throws Exception {
        create("Green Curry", "thai");
        create("Pad See Ew", "cuisine:thai");
        long canonical = tagRepository.findByName("cuisine:thai").orElseThrow().getId();
        long dupe = tagRepository.findByName("thai").orElseThrow().getId();

        mvc.perform(post("/api/tags/merge").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"from\":[" + dupe + "],\"into\":" + canonical + "}"))
                .andExpect(status().isOk());

        assertThat(tagRepository.findById(dupe)).isEmpty();
        assertThat(total("?tag=cuisine:thai&match=any")).isEqualTo(2);
    }

    private void create(String name, String... tags) throws Exception {
        String tagArray = String.join(",", Arrays.stream(tags).map(t -> "\"" + t + "\"").toList());
        mvc.perform(post("/api/recipes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"tags\":[" + tagArray + "]}"))
                .andExpect(status().isCreated());
    }

    private void postRecipe(String name, long... ingredientIds) throws Exception {
        String ingredients = Arrays.stream(ingredientIds)
                .mapToObj(id -> "{\"ingredientId\":" + id + "}")
                .collect(Collectors.joining(","));
        mvc.perform(post("/api/recipes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"ingredients\":[" + ingredients + "]}"))
                .andExpect(status().isCreated());
    }

    private long total(String query) throws Exception {
        String body = mvc.perform(get("/api/recipes" + query))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return firstMatch(TOTAL, body);
    }

    private static long firstMatch(Pattern pattern, String haystack) {
        Matcher matcher = pattern.matcher(haystack);
        assertThat(matcher.find()).as("pattern %s in %s", pattern, haystack).isTrue();
        return Long.parseLong(matcher.group(1));
    }
}
