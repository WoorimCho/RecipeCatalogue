package com.example.recipecatalogue.Dto;

import com.example.recipecatalogue.Model.IngredientReplacement;
import jakarta.validation.constraints.NotNull;

/**
 * A substitute for a recipe ingredient. {@code recipeId} is optional - present
 * when the substitute is something you can make (links to a recipe here).
 */
public record ReplacementDto(
        @NotNull Long ingredientId,
        Long recipeId) {

    public static ReplacementDto from(IngredientReplacement replacement) {
        return new ReplacementDto(replacement.getIngredientId(), replacement.getRecipeId());
    }
}
