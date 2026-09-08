package com.example.recipecatelog.Dto;

import com.example.recipecatelog.Model.RecipeIngredient;

import java.util.List;

public record RecipeIngredientResponse(
        Long id,
        Long ingredientId,
        String quantity,
        boolean optional,
        boolean replaceable,
        List<ReplacementDto> replacements) {

    public static RecipeIngredientResponse from(RecipeIngredient ingredient) {
        List<ReplacementDto> replacements = ingredient.getReplacements().stream()
                .map(ReplacementDto::from)
                .toList();
        return new RecipeIngredientResponse(
                ingredient.getId(),
                ingredient.getIngredientId(),
                ingredient.getQuantity(),
                ingredient.isOptional(),
                ingredient.isReplaceable(),
                replacements);
    }
}
