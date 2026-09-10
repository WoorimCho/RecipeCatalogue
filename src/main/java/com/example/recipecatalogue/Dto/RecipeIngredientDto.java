package com.example.recipecatalogue.Dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;

/**
 * A catalogue entry a recipe uses, on the way in. {@code ingredientId} is an id in
 * the Ingredient catalogue — it must be a real, positive id; the UI resolves a
 * typed name to an existing id (or tells you to add the ingredient first) before
 * submitting. {@code optional} / {@code replaceable} are boxed so they can be
 * omitted from the request (absent = false). If {@code replaceable} is true,
 * {@code replacements} must be non-empty.
 *
 * <p>{@code quantity} is free text for display. {@code amount} + {@code unit} are
 * the machine-readable pair the calculators use; supply both or neither (the
 * service rejects an amount without a unit). {@code unit} is lenient — {@code
 * "g"}, {@code "grams"}, {@code "tbsp"}, {@code "tablespoon"}, {@code "cup"}, …
 */
public record RecipeIngredientDto(
        @NotNull @Positive Long ingredientId,
        String quantity,
        @PositiveOrZero Double amount,
        String unit,
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
