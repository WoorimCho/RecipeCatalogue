package com.example.recipecatelog.Model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * One step of a {@link Recipe}. Order is the position of this step in
 * {@code Recipe.steps} (managed by {@code @OrderColumn}). {@code tools} are the
 * implements this particular step needs (e.g. "beater", "candy thermometer").
 */
@Entity
@Table(name = "recipe_step")
@Getter
public class RecipeStep {

    @Id
    @GeneratedValue
    private Long id;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;

    @Setter
    @Column(name = "text", nullable = false, length = 20_000)
    private String text;

    @ElementCollection
    @CollectionTable(name = "recipe_step_tool", joinColumns = @JoinColumn(name = "recipe_step_id"))
    @Column(name = "tool", nullable = false, length = 100)
    private Set<String> tools = new LinkedHashSet<>();

    protected RecipeStep() {
        // for JPA
    }

    public RecipeStep(String text, Set<String> tools) {
        this.text = text;
        if (tools != null) {
            this.tools.addAll(tools);
        }
    }
}
