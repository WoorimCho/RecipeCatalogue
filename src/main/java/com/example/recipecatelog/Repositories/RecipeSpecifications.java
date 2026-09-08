package com.example.recipecatelog.Repositories;

import com.example.recipecatelog.Model.Recipe;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collection;
import java.util.Locale;

/**
 * Composable filters for the recipe search endpoint. Each is written to keep
 * Spring Data's generated count query well-behaved (no {@code group by} /
 * {@code having}); "has all tags" is expressed as one join per tag rather than a
 * grouped count.
 */
public final class RecipeSpecifications {

    private RecipeSpecifications() {
    }

    public static Specification<Recipe> nameContains(String query) {
        String needle = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
        return (root, q, cb) -> cb.like(cb.lower(root.get("name")), needle);
    }

    public static Specification<Recipe> usesAnyIngredient(Collection<Long> ingredientIds) {
        return (root, q, cb) -> {
            q.distinct(true);
            return root.join("ingredients").get("ingredientId").in(ingredientIds);
        };
    }

    public static Specification<Recipe> hasAnyTag(Collection<String> tagNames) {
        return (root, q, cb) -> {
            q.distinct(true);
            return root.join("tags").get("name").in(tagNames);
        };
    }

    public static Specification<Recipe> hasAllTags(Collection<String> tagNames) {
        return (root, q, cb) -> {
            q.distinct(true);
            Predicate all = cb.conjunction();
            for (String name : tagNames) {
                // A fresh join per tag => an AND of "some tag row equals this name".
                all = cb.and(all, cb.equal(root.join("tags").get("name"), name));
            }
            return all;
        };
    }
}
