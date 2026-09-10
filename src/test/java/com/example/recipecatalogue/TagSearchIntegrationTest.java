package com.example.recipecatalogue;

import com.example.recipecatalogue.Repositories.RecipeRepository;
import com.example.cataloguecommon.tag.TagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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
@TestPropertySource(properties = "internal-auth.enabled=false")
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
                    {"ingredientId": 10, "quantity": "200g, dry", "amount": 200, "unit": "grams"},
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
        assertThat(created).contains("\"amount\":200").contains("\"unit\":\"g\"");   // "grams" normalised

        long id = firstMatch(ID, created);
        String fetched = mvc.perform(get("/api/recipes/" + id))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(fetched).contains("Soak the noodles.").contains("\"200g, dry\"");
        assertThat(fetched).contains("\"amount\":200").contains("\"unit\":\"g\"");
    }

    @Test
    void replaceableIngredientWithoutReplacementsIsRejected() throws Exception {
        mvc.perform(post("/api/recipes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Bad\",\"ingredients\":[{\"ingredientId\":1,\"replaceable\":true}]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownUnitIsRejected() throws Exception {
        mvc.perform(post("/api/recipes").contentType(MediaType.APPLICATION_JSON).content(
                        "{\"name\":\"Bad\",\"ingredients\":[{\"ingredientId\":1,\"amount\":2,\"unit\":\"smidgen\"}]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void amountWithoutUnitIsRejected() throws Exception {
        mvc.perform(post("/api/recipes").contentType(MediaType.APPLICATION_JSON).content(
                        "{\"name\":\"Bad\",\"ingredients\":[{\"ingredientId\":1,\"amount\":2}]}"))
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
        // a fragment matches the tag anywhere in its name (like the tag search)
        assertThat(total("?tag=thai")).isEqualTo(2);
        assertThat(total("?tag=thai&tag=quick")).isEqualTo(1);
    }

    @Test
    void excludeTagsDropRecipesCarryingThem() throws Exception {
        create("Tofu Scramble", "diet:vegan", "quick");
        create("Bacon Butty", "quick");
        create("Garden Salad", "diet:vegan");

        // everything "quick", minus the vegan ones -> just Bacon Butty
        assertThat(total("?tag=quick&notTag=diet:vegan")).isEqualTo(1);
        // pure exclusion: everything that isn't vegan
        assertThat(total("?notTag=diet:vegan")).isEqualTo(1);
        // several exclusions: carrying ANY of them drops the recipe
        assertThat(total("?notTag=diet:vegan&notTag=quick")).isZero();
        // normalised like every other tag param
        assertThat(total("?notTag=DIET:Vegan")).isEqualTo(1);
        // a fragment excludes any tag containing it
        assertThat(total("?notTag=vegan")).isEqualTo(1);
        assertThat(total("?tag=quick&notTag=vega")).isEqualTo(1);
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

    @Test
    void randomPicksFromTheFilteredSetAnd404sWhenEmpty() throws Exception {
        create("Green Curry", "cuisine:thai");
        create("Massaman", "cuisine:thai");
        create("Ratatouille", "cuisine:french");

        String picked = mvc.perform(get("/api/recipes/random?tag=cuisine:thai"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(picked).containsAnyOf("Green Curry", "Massaman").doesNotContain("Ratatouille");

        mvc.perform(get("/api/recipes/random?tag=cuisine:italian"))
                .andExpect(status().isNotFound());
    }

    @Test
    void csvImportCreatesRecipeSkeletons() throws Exception {
        String csv = """
                name,creator,tags,ingredients
                "Weeknight Dal","raj","cuisine:indian;quick","10:200:g;11;12:1:tbsp:opt"
                "Broken","x","","10:200"
                """;
        MockMultipartFile file = new MockMultipartFile(
                "file", "recipes.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        String body = mvc.perform(multipart("/api/recipes/import").file(file))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).contains("\"rows\":2", "\"imported\":1", "\"skipped\":1");
        assertThat(body).contains("no unit");                 // the "10:200" token
        assertThat(recipeRepository.count()).isEqualTo(1);

        long id = recipeRepository.findAll().iterator().next().getId();
        String fetched = mvc.perform(get("/api/recipes/" + id))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(fetched).contains("Weeknight Dal").contains("\"creator\":\"raj\"");
        assertThat(fetched).contains("\"amount\":200").contains("\"unit\":\"g\"");
        assertThat(fetched).contains("\"optional\":true");    // the 12:1:tbsp:opt line
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
