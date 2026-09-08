package com.example.recipecatelog.Dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * A catalog entry a recipe uses, on the way in. {@code ingredientId} is an id in
 * the Ingredient catalog. {@code optional} / {@code replaceable} are boxed so
 * they can be omitted from the request (absent = false). If {@code replaceable}
 * is true, {@code replacements} must be non-empty.
 */
public record RecipeIngredientDto(
        @NotNull Long ingredientId,
        String quantity,
        Boolean optional,
        Boolean replaceable,
        @Valid List<ReplacementDto> replacements) {

    public boolean isOptional() {
        return Boolean.TRUE.equals(optional);
    }

    public boolean isReplaceable() {
        return Boolean.TRUE.equals(replaceable);
    }

    public List<ReplacementDto> replacementsOrEmpty() {
        return replacements == null ? List.of() : replacements;
    }
}
