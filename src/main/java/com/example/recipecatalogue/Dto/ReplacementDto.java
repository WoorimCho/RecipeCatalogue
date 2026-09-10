package com.example.recipecatalogue.Dto;

import com.example.recipecatalogue.Model.IngredientReplacement;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * A substitute for a recipe ingredient. {@code ingredientId} must be a real,
 * positive Ingredient-catalogue id. {@code recipeId} is optional - present when
 * the substitute is something you can make (links to a recipe here).
 */
public record ReplacementDto(
        @NotNull @Positive Long ingredientId,
        @Positive Long recipeId) {

    public static ReplacementDto from(IngredientReplacement replacement) {
        return new ReplacementDto(replacement.getIngredientId(), replacement.getRecipeId());
    }
}
