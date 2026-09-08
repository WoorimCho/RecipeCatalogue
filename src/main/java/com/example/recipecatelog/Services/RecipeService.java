package com.example.recipecatelog.Services;

import com.example.recipecatelog.Dto.RecipeRequest;
import com.example.recipecatelog.Dto.RecipeResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface RecipeService {

    /**
     * List recipes, optionally filtered. All present filters are ANDed.
     *
     * @param name          case-insensitive substring of the recipe name
     * @param tags          tag names; {@code match} = "all" (default) or "any"
     * @param ingredientIds recipes that use any of these catalog entries
     */
    Page<RecipeResponse> search(String name, Collection<String> tags, String match,
                                Collection<Long> ingredientIds, Pageable pageable);

    RecipeResponse get(long id);

    /** Batch lookup by id for cross-service resolution (e.g. the BFF listing a user's favourites). */
    List<RecipeResponse> getByIds(Collection<Long> ids);

    RecipeResponse create(RecipeRequest request);

    RecipeResponse update(long id, RecipeRequest request);

    void delete(long id);

    RecipeResponse addTags(long id, Set<String> tagNames);

    RecipeResponse removeTag(long id, String tagName);
}
