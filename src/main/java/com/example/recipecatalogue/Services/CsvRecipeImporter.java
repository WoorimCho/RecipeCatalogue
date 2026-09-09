package com.example.recipecatalogue.Services;

import com.example.cataloguecommon.Csv;
import com.example.recipecatalogue.Dto.ImportResult;
import com.example.recipecatalogue.Dto.ImportResult.RowError;
import com.example.recipecatalogue.Dto.RecipeIngredientDto;
import com.example.recipecatalogue.Dto.RecipeRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Bulk-creates recipe skeletons from a CSV — one row per recipe.
 *
 * <p>Header row required; columns matched by name (case-insensitive):
 * {@code name} (required), {@code creator}, {@code tags} ({@code ;}-separated),
 * {@code ingredients} ({@code ;}-separated tokens, each
 * {@code <id>} or {@code <id>:<amount>:<unit>} or
 * {@code <id>:<amount>:<unit>:opt} for optional).
 *
 * <p>Steps are not imported (too structured for a flat row) — add them with
 * {@code PUT /api/recipes/{id}} afterward.
 */
@Service
public class CsvRecipeImporter {

    private final RecipeService recipeService;

    public CsvRecipeImporter(RecipeService recipeService) {
        this.recipeService = recipeService;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ImportResult importCsv(String csv) {
        List<String[]> rows = Csv.parse(csv);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("the file has no rows");
        }
        Map<String, Integer> col = headerIndex(rows.get(0));
        if (!col.containsKey("name")) {
            throw new IllegalArgumentException("CSV must have a 'name' column");
        }

        int imported = 0;
        List<RowError> errors = new ArrayList<>();
        for (int i = 1; i < rows.size(); i++) {
            long line = i + 1L;
            try {
                recipeService.create(toRequest(rows.get(i), col));
                imported++;
            } catch (RuntimeException ex) {
                errors.add(new RowError(line, rootMessage(ex)));
            }
        }
        return new ImportResult(rows.size() - 1, imported, errors.size(), errors);
    }

    private static Map<String, Integer> headerIndex(String[] header) {
        return IntStream.range(0, header.length).boxed().collect(Collectors.toMap(
                i -> header[i].trim().toLowerCase(Locale.ROOT), i -> i, (a, b) -> a));
    }

    private static RecipeRequest toRequest(String[] row, Map<String, Integer> col) {
        String name = cell(row, col, "name");
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("missing name");
        }
        String creator = cell(row, col, "creator");
        Set<String> tags = splitList(cell(row, col, "tags"));
        List<RecipeIngredientDto> ingredients = new ArrayList<>();
        for (String token : splitList(cell(row, col, "ingredients"))) {
            ingredients.add(parseIngredient(token));
        }
        return new RecipeRequest(
                name.trim(),
                StringUtils.hasText(creator) ? creator.trim() : null,
                null, List.of(), ingredients, tags);
    }

    private static RecipeIngredientDto parseIngredient(String token) {
        String[] p = token.split(":");
        long id;
        try {
            id = Long.parseLong(p[0].trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("bad ingredient token '" + token + "' (need a numeric id)");
        }
        if (p.length == 2) {
            throw new IllegalArgumentException("ingredient '" + token + "' has an amount but no unit (use id:amount:unit)");
        }
        Double amount = null;
        String unit = null;
        if (p.length >= 3) {
            try {
                amount = Double.parseDouble(p[1].trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("ingredient '" + token + "' has a non-numeric amount");
            }
            unit = p[2].trim();
        }
        Boolean optional = p.length >= 4 ? Boolean.TRUE : null;
        return new RecipeIngredientDto(id, null, amount, unit, optional, null, null);
    }

    private static Set<String> splitList(String cell) {
        Set<String> out = new LinkedHashSet<>();
        if (StringUtils.hasText(cell)) {
            for (String s : cell.split(";")) {
                if (StringUtils.hasText(s)) {
                    out.add(s.trim());
                }
            }
        }
        return out;
    }

    private static String cell(String[] row, Map<String, Integer> col, String name) {
        Integer i = col.get(name);
        return (i == null || i >= row.length) ? null : row[i];
    }

    private static String rootMessage(Throwable t) {
        Throwable c = t;
        while (c.getCause() != null && c.getCause() != c) {
            c = c.getCause();
        }
        String m = c.getMessage();
        if (m != null && m.toLowerCase(Locale.ROOT).contains("uk_recipe_name_creator_version")) {
            return "a recipe with that name + creator + version already exists";
        }
        return m != null ? m : t.getClass().getSimpleName();
    }
}
