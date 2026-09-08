package com.example.recipecatelog.Model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * A catalog entry used by a {@link Recipe}, with its role in that recipe.
 *
 * <p>{@code ingredientId} is an id in the Ingredient catalog (no FK).
 * {@code optional} means the recipe works without it. {@code replaceable} means
 * one of {@link #getReplacements()} may be substituted; when {@code replaceable}
 * is true the service requires at least one replacement.
 */
@Entity
@Table(name = "recipe_ingredient")
@Getter
public class RecipeIngredient {

    @Id
    @GeneratedValue
    private Long id;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;

    @Setter
    @Column(name = "ingredient_id", nullable = false)
    private Long ingredientId;

    /** Free-text amount ("2 cups", "a pinch"); null if unspecified. Structured quantity is a later concern. */
    @Setter
    @Column(name = "quantity", length = 100)
    private String quantity;

    @Setter
    @Column(name = "optional", nullable = false)
    private boolean optional = false;

    @Setter
    @Column(name = "replaceable", nullable = false)
    private boolean replaceable = false;

    @OneToMany(mappedBy = "recipeIngredient", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<IngredientReplacement> replacements = new ArrayList<>();

    protected RecipeIngredient() {
        // for JPA
    }

    public RecipeIngredient(Long ingredientId) {
        this.ingredientId = ingredientId;
    }

    public void replaceReplacements(List<IngredientReplacement> newReplacements) {
        replacements.clear();
        newReplacements.forEach(replacement -> replacement.setRecipeIngredient(this));
        replacements.addAll(newReplacements);
    }
}
