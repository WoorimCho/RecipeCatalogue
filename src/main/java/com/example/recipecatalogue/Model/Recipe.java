package com.example.recipecatalogue.Model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.example.cataloguecommon.tag.Tag;

/**
 * A recipe: an ordered list of {@link RecipeStep}s (each carrying the tools it
 * needs), the catalogue entries it uses ({@link RecipeIngredient}, any of which may
 * be optional or replaceable), and tags.
 *
 * <p>All cross-service references are by id. {@code ingredientId} points into the
 * Ingredient catalogue; {@code recipeId} (on a replacement) points back into this
 * catalogue. No foreign keys cross the service boundary.
 *
 * <p>Recipes that share a name are variations, distinguished by
 * {@code (name, creator, version)}. The service assigns the next {@code version}
 * for a given {@code (name, creator)} when the request leaves it null.
 *
 * <p>"Recipe-exclusive" tags (tools, difficulty, time taken, category) are just
 * tags, by convention namespaced: {@code tool:beater}, {@code difficulty:easy},
 * {@code time:90min}, {@code category:snack}. Nothing structural distinguishes
 * them from tags inherited from ingredients.
 */
@Entity
@Table(name = "recipe", uniqueConstraints = @UniqueConstraint(
        name = "uk_recipe_name_creator_version",
        columnNames = {"name", "creator", "version"}))
@Getter
public class Recipe {

    @Id
    @GeneratedValue
    private Long id;

    @Setter
    @Column(name = "name", nullable = false)
    private String name;

    /** Owner of this variation. Free text until the User service exists; defaults to "anonymous". */
    @Setter
    @Column(name = "creator", nullable = false)
    private String creator = "anonymous";

    /** Variation number within a (name, creator). Assigned by the service when not supplied. */
    @Setter
    @Column(name = "version", nullable = false)
    private int version = 1;

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderColumn(name = "position")
    private List<RecipeStep> steps = new ArrayList<>();

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RecipeIngredient> ingredients = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "recipe_tag",
            joinColumns = @JoinColumn(name = "recipe_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags = new LinkedHashSet<>();

    protected Recipe() {
        // for JPA
    }

    public Recipe(String name) {
        this.name = name;
    }

    public void replaceSteps(List<RecipeStep> newSteps) {
        steps.clear();
        newSteps.forEach(step -> step.setRecipe(this));
        steps.addAll(newSteps);
    }

    public void replaceIngredients(List<RecipeIngredient> newIngredients) {
        ingredients.clear();
        newIngredients.forEach(ingredient -> ingredient.setRecipe(this));
        ingredients.addAll(newIngredients);
    }

    public void addTag(Tag tag) {
        tags.add(tag);
    }

    public void removeTag(Tag tag) {
        tags.remove(tag);
    }

    public void removeTagsById(Set<Long> tagIds) {
        tags.removeIf(tag -> tagIds.contains(tag.getId()));
    }

    public void replaceTags(Set<Tag> newTags) {
        tags.clear();
        tags.addAll(newTags);
    }
}
