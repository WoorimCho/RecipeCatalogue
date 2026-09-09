package com.example.recipecatalogue.Dto;

import com.example.recipecatalogue.Model.RecipeIngredient;

import java.util.List;

public record RecipeIngredientResponse(
        Long id,
        Long ingredientId,
        String quantity,
        Double amount,
        String unit,
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
                ingredient.getAmount(),
                ingredient.getUnit() == null ? null : ingredient.getUnit().token(),
                ingredient.isOptional(),
                ingredient.isReplaceable(),
                replacements);
    }
}
