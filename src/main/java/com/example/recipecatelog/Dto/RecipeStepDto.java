package com.example.recipecatelog.Dto;

import jakarta.validation.constraints.NotBlank;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * One recipe step. On input {@code position} is ignored (array order wins); on
 * output it is the step's index.
 */
public record RecipeStepDto(
        Integer position,
        @NotBlank String text,
        Set<String> tools) {

    public Set<String> toolsOrEmpty() {
        return tools == null ? Set.of() : tools;
    }

    public static RecipeStepDto of(int position, String text, Set<String> tools) {
        return new RecipeStepDto(position, text, new LinkedHashSet<>(tools));
    }
}
