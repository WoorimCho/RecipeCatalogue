package com.example.recipecatelog.Dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Set;

/**
 * Body for creating or updating a recipe.
 *
 * <p>{@code creator} defaults to "anonymous" when null. {@code version} is
 * assigned automatically (next for this name+creator) when null; supply it only
 * to target a specific variation slot.
 */
public record RecipeRequest(
        @NotBlank @Size(max = 255) String name,
        String creator,
        Integer version,
        @Valid List<RecipeStepDto> steps,
        @Valid List<RecipeIngredientDto> ingredients,
        Set<String> tags) {

    public List<RecipeStepDto> stepsOrEmpty() {
        return steps == null ? List.of() : steps;
    }

    public List<RecipeIngredientDto> ingredientsOrEmpty() {
        return ingredients == null ? List.of() : ingredients;
    }

    public Set<String> tagsOrEmpty() {
        return tags == null ? Set.of() : tags;
    }
}
