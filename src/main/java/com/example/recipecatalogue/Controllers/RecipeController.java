package com.example.recipecatalogue.Controllers;

import com.example.recipecatalogue.Dto.ImportResult;
import com.example.cataloguecommon.PageResponse;
import com.example.recipecatalogue.Dto.RecipeRequest;
import com.example.recipecatalogue.Dto.RecipeResponse;
import com.example.cataloguecommon.tag.TagNamesRequest;
import com.example.recipecatalogue.Services.CsvRecipeImporter;
import com.example.recipecatalogue.Services.RecipeService;
import jakarta.validation.Valid;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/recipes")
public class RecipeController {

    private final RecipeService recipeService;
    private final CsvRecipeImporter csvImporter;

    public RecipeController(RecipeService recipeService, CsvRecipeImporter csvImporter) {
        this.recipeService = recipeService;
        this.csvImporter = csvImporter;
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
    public PageResponse<RecipeResponse> list(
            @RequestParam(required = false) String name,
            @RequestParam(name = "tag", required = false) List<String> tags,
            @RequestParam(name = "match", defaultValue = "all") String match,
            @RequestParam(name = "ingredientId", required = false) List<Long> ingredientIds,
            // Sort by id, not name: search uses SELECT DISTINCT with joins, and some
            // databases reject ordering by a column outside the distinct projection.
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return PageResponse.of(recipeService.search(name, tags, match, ingredientIds, pageable));
    }

    /**
     * One random recipe, optionally from a filtered set (same params as the
     * list). 404 when nothing matches.
     * <pre>
     * GET /api/recipes/random
     * GET /api/recipes/random?tag=cuisine:thai&amp;match=any&amp;ingredientId=10
     * </pre>
     */
    @GetMapping("/random")
    public RecipeResponse random(
            @RequestParam(required = false) String name,
            @RequestParam(name = "tag", required = false) List<String> tags,
            @RequestParam(name = "match", defaultValue = "all") String match,
            @RequestParam(name = "ingredientId", required = false) List<Long> ingredientIds) {
        return recipeService.random(name, tags, match, ingredientIds);
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

    /**
     * Bulk-create recipe skeletons from a CSV upload ({@code multipart/form-data},
     * part {@code file}). Columns: {@code name} (required), {@code creator},
     * {@code tags} ({@code ;}-separated), {@code ingredients} ({@code ;}-separated
     * {@code <id>} / {@code <id>:<amount>:<unit>} / {@code …:opt} tokens). Steps
     * aren't imported — add them with {@code PUT /api/recipes/{id}}. Always 200.
     */
    @PostMapping(path = "/import", consumes = "multipart/form-data")
    public ImportResult importCsv(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("the uploaded file is empty");
        }
        try {
            return csvImporter.importCsv(new String(file.getBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException("could not read the uploaded file", e);
        }
    }
}
