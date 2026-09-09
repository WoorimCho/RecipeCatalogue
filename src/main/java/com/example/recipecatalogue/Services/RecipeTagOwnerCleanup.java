package com.example.recipecatalogue.Services;

import com.example.cataloguecommon.tag.Tag;
import com.example.cataloguecommon.tag.TagOwnerCleanup;
import com.example.recipecatalogue.Model.Recipe;
import com.example.recipecatalogue.Repositories.RecipeRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * The Recipe side of {@link TagOwnerCleanup}: there is no DB cascade on the
 * {@code recipe_tag} join table, so links are cleared in code before a tag is
 * deleted or merged (see {@code catalogue-common}'s {@code TagServiceImpl}).
 */
@Component
class RecipeTagOwnerCleanup implements TagOwnerCleanup {

    private final RecipeRepository recipes;

    RecipeTagOwnerCleanup(RecipeRepository recipes) {
        this.recipes = recipes;
    }

    @Override
    public void detachAll(Set<Long> tagIds) {
        List<Recipe> linked = recipes.findByTags_IdIn(tagIds);
        linked.forEach(recipe -> recipe.removeTagsById(tagIds));
        recipes.saveAll(linked);
        recipes.flush();
    }

    @Override
    public void retag(Set<Long> fromIds, Tag into) {
        List<Recipe> affected = recipes.findByTags_IdIn(fromIds);
        for (Recipe recipe : affected) {
            recipe.removeTagsById(fromIds);
            recipe.addTag(into);
        }
        recipes.saveAll(affected);
        recipes.flush();
    }
}
