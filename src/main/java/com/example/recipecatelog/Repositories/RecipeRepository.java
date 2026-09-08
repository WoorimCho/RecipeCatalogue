package com.example.recipecatelog.Repositories;

import com.example.recipecatelog.Model.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface RecipeRepository
        extends JpaRepository<Recipe, Long>, JpaSpecificationExecutor<Recipe> {

    /** Every recipe linked to any of the given tag ids (used by tag delete / merge). */
    List<Recipe> findByTags_IdIn(Collection<Long> tagIds);

    /** Highest existing version for a (name, creator), or 0 if none - used to assign the next one. */
    @Query("select coalesce(max(r.version), 0) from Recipe r where r.name = :name and r.creator = :creator")
    int findMaxVersion(@Param("name") String name, @Param("creator") String creator);
}
