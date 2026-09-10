package com.example.recipecatalogue.Repositories;

import com.example.recipecatalogue.Model.Recipe;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collection;
import java.util.Locale;

/**
 * Composable filters for the recipe search endpoint. Each is written to keep
 * Spring Data's generated count query well-behaved (no {@code group by} /
 * {@code having}); "has all tags" is expressed as one join per term rather than a
 * grouped count.
 *
 * <p>Tag terms match a tag name <em>anywhere</em> (case-insensitive substring),
 * to line up with the tag search / autocomplete — typing "vegan" finds recipes
 * tagged {@code diet:vegan}.
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

    /** Recipe has at least one tag whose name contains any of the given terms. */
    public static Specification<Recipe> hasAnyTag(Collection<String> tagTerms) {
        return (root, q, cb) -> {
            q.distinct(true);
            Expression<String> name = cb.lower(root.join("tags").get("name"));
            Predicate any = cb.disjunction();
            for (String term : tagTerms) {
                any = cb.or(any, cb.like(name, like(term)));
            }
            return any;
        };
    }

    /** For every term, the recipe has some tag whose name contains it. */
    public static Specification<Recipe> hasAllTags(Collection<String> tagTerms) {
        return (root, q, cb) -> {
            q.distinct(true);
            Predicate all = cb.conjunction();
            for (String term : tagTerms) {
                // A fresh join per term => an AND of "some tag row matches this term".
                all = cb.and(all, cb.like(cb.lower(root.join("tags").get("name")), like(term)));
            }
            return all;
        };
    }

    /**
     * Recipes with <em>no</em> tag matching any of the terms — "not vegan, not
     * spicy". A {@code NOT IN (subquery)} rather than a negated join, so it
     * doesn't disturb the main query's joins or its {@code distinct}.
     */
    public static Specification<Recipe> lacksAllTags(Collection<String> tagTerms) {
        return (root, q, cb) -> {
            Subquery<Long> tagged = q.subquery(Long.class);
            Root<Recipe> other = tagged.from(Recipe.class);
            Expression<String> name = cb.lower(other.join("tags").get("name"));
            Predicate anyMatch = cb.disjunction();
            for (String term : tagTerms) {
                anyMatch = cb.or(anyMatch, cb.like(name, like(term)));
            }
            tagged.select(other.get("id")).where(anyMatch);
            return cb.not(root.get("id").in(tagged));
        };
    }

    private static String like(String term) {
        return "%" + term.trim().toLowerCase(Locale.ROOT) + "%";
    }
}
