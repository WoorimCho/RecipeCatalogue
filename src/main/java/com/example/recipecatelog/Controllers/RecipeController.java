package com.example.recipecatelog.Controllers;

import com.example.recipecatelog.Dto.RecipeRequest;
import com.example.recipecatelog.Dto.RecipeResponse;
import com.example.recipecatelog.Dto.TagNamesRequest;
import com.example.recipecatelog.Services.RecipeService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/recipes")
public class RecipeController {

    private final RecipeService recipeService;

    public RecipeController(RecipeService recipeService) {
        this.recipeService = recipeService;
    }

    /**
     * List recipes; every supplied filter is ANDed.
     * <pre>
     * GET /api/recipes
     * GET /api/recipes?name=curry
     * GET /api/recipes?tag=cuisine:thai&amp;tag=quick               has ALL of them (default)
     * GET /api/recipes?tag=cuisine:thai&amp;tag=quick&amp;match=any   has ANY of them
     * GET /api/recipes?ingredientId=10&amp;ingredientId=20           uses any of those entries
     * GET /api/recipes?page=0&amp;size=20&amp;sort=name,asc
     * </pre>
     */
    @GetMapping
    public Page<RecipeResponse> list(
            @RequestParam(required = false) String name,
            @RequestParam(name = "tag", required = false) List<String> tags,
            @RequestParam(name = "match", defaultValue = "all") String match,
            @RequestParam(name = "ingredientId", required = false) List<Long> ingredientIds,
            // Sort by id, not name: search uses SELECT DISTINCT with joins, and some
            // databases reject ordering by a column outside the distinct projection.
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return recipeService.search(name, tags, match, ingredientIds, pageable);
    }

    @GetMapping("/{id}")
    public RecipeResponse get(@PathVariable long id) {
        return recipeService.get(id);
    }

    /**
     * Batch lookup by id — for cross-service resolution (e.g. the BFF listing a
     * user's favourite recipes). Unknown ids are omitted.
     * <pre>GET /api/recipes/by-ids?id=1&amp;id=2</pre>
     */
    @GetMapping("/by-ids")
    public List<RecipeResponse> byIds(@RequestParam("id") List<Long> ids) {
        return recipeService.getByIds(ids);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RecipeResponse create(@Valid @RequestBody RecipeRequest request) {
        return recipeService.create(request);
    }

    @PutMapping("/{id}")
    public RecipeResponse update(@PathVariable long id, @Valid @RequestBody RecipeRequest request) {
        return recipeService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) {
        recipeService.delete(id);
    }

    /** Attach tags (get-or-create by name) to a recipe. */
    @PostMapping("/{id}/tags")
    public RecipeResponse addTags(@PathVariable long id, @Valid @RequestBody TagNamesRequest request) {
        return recipeService.addTags(id, request.tags());
    }

    /** Detach a single tag from a recipe. The tag itself is left alone. */
    @DeleteMapping("/{id}/tags/{tagName}")
    public RecipeResponse removeTag(@PathVariable long id, @PathVariable String tagName) {
        return recipeService.removeTag(id, tagName);
    }
}
