package com.example.recipecatelog.Model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * A reusable, free-form label that can be attached to many {@link Recipe}s.
 *
 * <p>Names are normalised to trimmed lower-case by the service layer, so
 * {@code "Quick"}, {@code "quick "} and {@code "quick"} collapse to one tag. The
 * optional {@code namespace} groups related tags for filtering ("cuisine",
 * "diet", "difficulty"); it is metadata only and is not parsed out of the name.
 *
 * <p>There is deliberately no {@code Set<Recipe>} back-reference: a popular tag
 * could be linked to thousands of recipes and we never want to load them all
 * just because someone touched the tag.
 */
@Entity
@Table(name = "tag", indexes = {
        @Index(name = "ix_tag_name", columnList = "name", unique = true),
        @Index(name = "ix_tag_namespace", columnList = "namespace")
})
@Getter
public class Tag {

    @Id
    @GeneratedValue
    private Long id;

    @Setter
    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Setter
    @Column(name = "namespace", length = 60)
    private String namespace;

    @Setter
    @Column(name = "description", length = 500)
    private String description;

    protected Tag() {
        // for JPA
    }

    public Tag(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Tag other)) {
            return false;
        }
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        // Constant: id is null until persisted, and a Tag's identity must not
        // change when it gets an id while sitting in a HashSet.
        return Tag.class.hashCode();
    }
}
