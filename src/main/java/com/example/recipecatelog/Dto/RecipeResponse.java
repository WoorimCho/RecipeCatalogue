package com.example.recipecatelog.Dto;

import com.example.recipecatelog.Model.Recipe;
import com.example.recipecatelog.Model.RecipeStep;

import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

public record RecipeResponse(
        Long id,
        String name,
        String creator,
        int version,
        List<RecipeStepDto> steps,
        List<RecipeIngredientResponse> ingredients,
        List<TagResponse> tags) {

    public static RecipeResponse from(Recipe recipe) {
        List<RecipeStep> stepEntities = recipe.getSteps();
        List<RecipeStepDto> steps = IntStream.range(0, stepEntities.size())
                .mapToObj(i -> RecipeStepDto.of(i, stepEntities.get(i).getText(), stepEntities.get(i).getTools()))
                .toList();

        List<RecipeIngredientResponse> ingredients = recipe.getIngredients().stream()
                .map(RecipeIngredientResponse::from)
                .toList();

        List<TagResponse> tags = recipe.getTags().stream()
                .map(TagResponse::from)
                .sorted(Comparator.comparing(TagResponse::name, String.CASE_INSENSITIVE_ORDER))
                .toList();

        return new RecipeResponse(recipe.getId(), recipe.getName(), recipe.getCreator(),
                recipe.getVersion(), steps, ingredients, tags);
    }
}
