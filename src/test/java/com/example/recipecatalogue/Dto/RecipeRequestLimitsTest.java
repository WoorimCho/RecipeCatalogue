package com.example.recipecatalogue.Dto;

import com.example.cataloguecommon.tag.TagService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SECURITY.md N4, at the annotation level: no Spring context, no database. The
 * HTTP-level proof (a clean 400) is in {@code TagSearchIntegrationTest}.
 */
class RecipeRequestLimitsTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void startValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void stopValidator() {
        factory.close();
    }

    private static RecipeIngredientDto line(long id) {
        return new RecipeIngredientDto(id, null, null, null, null, null, null);
    }

    private static List<RecipeIngredientDto> lines(int count) {
        return IntStream.rangeClosed(1, count).mapToObj(RecipeRequestLimitsTest::line).collect(Collectors.toList());
    }

    private static RecipeRequest recipe(List<RecipeStepDto> steps, List<RecipeIngredientDto> ingredients, Set<String> tags) {
        return new RecipeRequest("Soup", null, null, steps, ingredients, tags);
    }

    private static Set<String> violatedPaths(RecipeRequest request) {
        return validator.validate(request).stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Test
    void aTypicalRecipeIsValid() {
        RecipeRequest ok = recipe(List.of(new RecipeStepDto(null, "Boil.", Set.of("pot"))), lines(5), Set.of("diet:vegan"));
        assertThat(validator.validate(ok)).isEmpty();
    }

    @Test
    void theMaximumsThemselvesAreValid() {
        List<RecipeStepDto> steps = IntStream.range(0, Limits.STEPS)
                .mapToObj(i -> new RecipeStepDto(null, "x".repeat(Limits.STEP_TEXT_LENGTH), tools(Limits.TOOLS_PER_STEP)))
                .collect(Collectors.toList());
        List<RecipeIngredientDto> ingredients = new ArrayList<>();
        for (int i = 1; i <= Limits.INGREDIENTS; i++) {
            List<ReplacementDto> replacements = IntStream.rangeClosed(1, Limits.REPLACEMENTS_PER_INGREDIENT)
                    .mapToObj(r -> new ReplacementDto((long) r, null)).collect(Collectors.toList());
            ingredients.add(new RecipeIngredientDto((long) i, "q".repeat(Limits.QUANTITY_LENGTH), 1.0,
                    "u".repeat(Limits.UNIT_INPUT_LENGTH), null, true, replacements));
        }
        Set<String> tags = IntStream.range(0, TagService.MAX_TAGS_PER_REQUEST)
                .mapToObj(i -> "t" + i).collect(Collectors.toCollection(LinkedHashSet::new));

        assertThat(validator.validate(recipe(steps, ingredients, tags))).isEmpty();
    }

    private static Set<String> tools(int count) {
        return IntStream.range(0, count).mapToObj(i -> "tool" + i).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Test
    void tooManyIngredientLines() {
        assertThat(violatedPaths(recipe(List.of(), lines(Limits.INGREDIENTS + 1), Set.of()))).containsExactly("ingredients");
    }

    @Test
    void tooManySteps() {
        List<RecipeStepDto> steps = IntStream.range(0, Limits.STEPS + 1)
                .mapToObj(i -> new RecipeStepDto(null, "x", null)).collect(Collectors.toList());
        assertThat(violatedPaths(recipe(steps, lines(1), Set.of()))).containsExactly("steps");
    }

    @Test
    void tooManyReplacementsOnOneLine() {
        List<ReplacementDto> reps = IntStream.rangeClosed(1, Limits.REPLACEMENTS_PER_INGREDIENT + 1)
                .mapToObj(r -> new ReplacementDto((long) r, null)).collect(Collectors.toList());
        RecipeRequest r = recipe(List.of(), List.of(new RecipeIngredientDto(1L, null, null, null, null, true, reps)), Set.of());
        assertThat(violatedPaths(r)).containsExactly("ingredients[0].replacements");
    }

    @Test
    void tooManyTagsAndATagThatIsTooLong() {
        Set<String> many = IntStream.rangeClosed(1, TagService.MAX_TAGS_PER_REQUEST + 1)
                .mapToObj(i -> "t" + i).collect(Collectors.toCollection(LinkedHashSet::new));
        assertThat(violatedPaths(recipe(List.of(), lines(1), many))).containsExactly("tags");

        Set<String> longTag = Set.of("a".repeat(TagService.MAX_TAG_LENGTH + 1));
        assertThat(violatedPaths(recipe(List.of(), lines(1), longTag))).hasSize(1)
                .allMatch(p -> p.startsWith("tags"));
    }

    @Test
    void aStepWithTooMuchTextOrTooManyOrTooLongTools() {
        RecipeStepDto bigText = new RecipeStepDto(null, "x".repeat(Limits.STEP_TEXT_LENGTH + 1), null);
        assertThat(violatedPaths(recipe(List.of(bigText), lines(1), Set.of()))).containsExactly("steps[0].text");

        RecipeStepDto manyTools = new RecipeStepDto(null, "x", tools(Limits.TOOLS_PER_STEP + 1));
        assertThat(violatedPaths(recipe(List.of(manyTools), lines(1), Set.of()))).containsExactly("steps[0].tools");

        RecipeStepDto longTool = new RecipeStepDto(null, "x", Set.of("t".repeat(Limits.TOOL_LENGTH + 1)));
        assertThat(violatedPaths(recipe(List.of(longTool), lines(1), Set.of()))).hasSize(1)
                .allMatch(p -> p.startsWith("steps[0].tools"));
    }

    @Test
    void overlongCreatorQuantityAndUnit() {
        RecipeRequest creator = new RecipeRequest("Soup", "c".repeat(Limits.NAME_LENGTH + 1), null, List.of(), lines(1), Set.of());
        assertThat(violatedPaths(creator)).containsExactly("creator");

        RecipeIngredientDto bad = new RecipeIngredientDto(1L, "q".repeat(Limits.QUANTITY_LENGTH + 1), null,
                "u".repeat(Limits.UNIT_INPUT_LENGTH + 1), null, null, null);
        assertThat(violatedPaths(recipe(List.of(), List.of(bad), Set.of())))
                .containsExactlyInAnyOrder("ingredients[0].quantity", "ingredients[0].unit");
    }

    @Test
    void violationMessagesNameTheOffendingLimit() {
        Set<ConstraintViolation<RecipeRequest>> v = validator.validate(recipe(List.of(), lines(Limits.INGREDIENTS + 1), Set.of()));
        assertThat(v).singleElement().satisfies(x -> assertThat(x.getMessage()).contains(String.valueOf(Limits.INGREDIENTS)));
    }
}
