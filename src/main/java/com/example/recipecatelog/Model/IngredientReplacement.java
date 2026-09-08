package com.example.recipecatelog.Model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * A substitute for a {@link RecipeIngredient}.
 *
 * <p>{@code ingredientId} is the substitute catalog entry. {@code recipeId} is
 * optional: if the substitute can be made rather than bought, it links to a
 * recipe in this catalog that produces it (e.g. buttermilk).
 */
@Entity
@Table(name = "ingredient_replacement")
@Getter
public class IngredientReplacement {

    @Id
    @GeneratedValue
    private Long id;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipe_ingredient_id", nullable = false)
    private RecipeIngredient recipeIngredient;

    @Setter
    @Column(name = "ingredient_id", nullable = false)
    private Long ingredientId;

    @Setter
    @Column(name = "recipe_id")
    private Long recipeId;

    protected IngredientReplacement() {
        // for JPA
    }

    public IngredientReplacement(Long ingredientId, Long recipeId) {
        this.ingredientId = ingredientId;
        this.recipeId = recipeId;
    }
}
