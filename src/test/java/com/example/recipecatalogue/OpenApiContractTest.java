package com.example.recipecatalogue;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static com.atlassian.oai.validator.mockmvc.OpenApiValidationMatchers.openApi;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contract test: every interaction below is validated — request <em>and</em>
 * response — against the checked-in {@code static/openapi.yaml}. If the
 * controllers drift from the published contract (a field renamed, a status code
 * changed, the page envelope reshaped) this fails.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class OpenApiContractTest {

    private static final String FULL_RECIPE = """
            {
              "name": "Pad Thai",
              "steps": [
                {"text": "Soak the noodles.", "tools": ["bowl"]},
                {"text": "Stir-fry everything.", "tools": ["wok", "spatula"]}
              ],
              "ingredients": [
                {"ingredientId": 10, "quantity": "200g", "amount": 200, "unit": "g"},
                {"ingredientId": 11, "optional": true},
                {"ingredientId": 12, "replaceable": true,
                 "replacements": [{"ingredientId": 99, "recipeId": 7}]}
              ],
              "tags": ["cuisine:thai", "difficulty:easy"]
            }""";

    private static OpenApiInteractionValidator validator;

    @Autowired
    MockMvc mvc;

    @BeforeAll
    static void loadSpec() throws Exception {
        String spec = new String(
                new ClassPathResource("static/openapi.yaml").getInputStream().readAllBytes(),
                StandardCharsets.UTF_8);
        validator = OpenApiInteractionValidator.createForInlineApiSpecification(spec).build();
    }

    @Test
    void listRecipes_matchesContract() throws Exception {
        mvc.perform(post("/api/recipes").contentType(MediaType.APPLICATION_JSON).content(FULL_RECIPE));

        mvc.perform(get("/api/recipes?page=0&size=20"))
                .andExpect(status().isOk())
                .andExpect(openApi().isValid(validator));
    }

    @Test
    void searchRecipes_matchesContract() throws Exception {
        mvc.perform(post("/api/recipes").contentType(MediaType.APPLICATION_JSON).content(FULL_RECIPE));

        mvc.perform(get("/api/recipes?name=pad&tag=cuisine:thai&match=any&ingredientId=10"))
                .andExpect(status().isOk())
                .andExpect(openApi().isValid(validator));
    }

    @Test
    void createFullRecipe_matchesContract() throws Exception {
        mvc.perform(post("/api/recipes").contentType(MediaType.APPLICATION_JSON).content(FULL_RECIPE))
                .andExpect(status().isCreated())
                .andExpect(openApi().isValid(validator));
    }

    @Test
    void getRecipe_matchesContract() throws Exception {
        String body = mvc.perform(post("/api/recipes").contentType(MediaType.APPLICATION_JSON).content(FULL_RECIPE))
                .andReturn().getResponse().getContentAsString();
        long id = Long.parseLong(body.replaceAll(".*?\"id\":(\\d+).*", "$1"));

        mvc.perform(get("/api/recipes/{id}", id))
                .andExpect(status().isOk())
                .andExpect(openApi().isValid(validator));
    }

    @Test
    void getMissingRecipe_matchesContractAs404Problem() throws Exception {
        mvc.perform(get("/api/recipes/{id}", 999_999))
                .andExpect(status().isNotFound())
                .andExpect(openApi().isValid(validator));
    }

    @Test
    void createInvalidRecipe_matchesContractAs400Problem() throws Exception {
        mvc.perform(post("/api/recipes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Bad\",\"ingredients\":[{\"ingredientId\":1,\"replaceable\":true}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(openApi().isValid(validator));
    }

    @Test
    void byIds_matchesContract() throws Exception {
        mvc.perform(get("/api/recipes/by-ids?id=1&id=999999"))
                .andExpect(status().isOk())
                .andExpect(openApi().isValid(validator));
    }

    @Test
    void random_matchesContract() throws Exception {
        mvc.perform(post("/api/recipes").contentType(MediaType.APPLICATION_JSON).content(FULL_RECIPE));

        mvc.perform(get("/api/recipes/random"))
                .andExpect(status().isOk())
                .andExpect(openApi().isValid(validator));

        mvc.perform(get("/api/recipes/random?tag=does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(openApi().isValid(validator));
    }

    @Test
    void csvImport_returnsTheDocumentedShape() throws Exception {
        // swagger-request-validator can't reconstruct a multipart body from MockMvc.
        MockMultipartFile file = new MockMultipartFile("file", "in.csv", "text/csv",
                "name,creator,ingredients\nSoup,chef,10;11:1:cup\nBad,x,99:5\n".getBytes(StandardCharsets.UTF_8));
        mvc.perform(multipart("/api/recipes/import").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows").value(2))
                .andExpect(jsonPath("$.imported").value(1))
                .andExpect(jsonPath("$.skipped").value(1))
                .andExpect(jsonPath("$.errors[0].line").value(3));
    }

    @Test
    void listTags_matchesContract() throws Exception {
        mvc.perform(get("/api/tags?page=0&size=20"))
                .andExpect(status().isOk())
                .andExpect(openApi().isValid(validator));
    }

    @Test
    void createTag_matchesContract() throws Exception {
        mvc.perform(post("/api/tags").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"category:snack\",\"namespace\":\"category\"}"))
                .andExpect(status().isCreated())
                .andExpect(openApi().isValid(validator));
    }
}
