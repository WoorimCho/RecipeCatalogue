package com.example.recipecatalogue.Dto;

import com.example.cataloguecommon.tag.TagService;
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
        @NotBlank @Size(max = Limits.NAME_LENGTH) String name,
        @Size(max = Limits.NAME_LENGTH) String creator,
        Integer version,
        @Valid @Size(max = Limits.STEPS) List<RecipeStepDto> steps,
        @Valid @Size(max = Limits.INGREDIENTS) List<RecipeIngredientDto> ingredients,
        @Size(max = TagService.MAX_TAGS_PER_REQUEST) Set<@Size(max = TagService.MAX_TAG_LENGTH) String> tags) {

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
