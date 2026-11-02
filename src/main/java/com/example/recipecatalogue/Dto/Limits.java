package com.example.recipecatalogue.Dto;

/**
 * Upper bounds on a recipe request (SECURITY.md N4). They sit far above any real
 * recipe and, where a column exists, line up with it (see {@code V1__init.sql}), so
 * an oversized value is a clean 400 instead of a database error or a request
 * thread busy persisting 50 000 child rows.
 *
 * <p>The UI mirrors these in {@code RecipeForm}; this service stays authoritative
 * (the CSV importer builds requests that never pass through Bean Validation, so
 * {@code RecipeServiceImpl.apply} re-checks the counts).
 */
public final class Limits {

    public static final int NAME_LENGTH = 255;            // recipe.name / recipe.creator varchar(255)
    public static final int STEPS = 200;
    public static final int STEP_TEXT_LENGTH = 20_000;    // RecipeStep.text length
    public static final int TOOLS_PER_STEP = 30;
    public static final int TOOL_LENGTH = 100;            // recipe_step_tool.tool varchar(100)
    public static final int INGREDIENTS = 100;
    public static final int REPLACEMENTS_PER_INGREDIENT = 10;
    public static final int QUANTITY_LENGTH = 100;        // recipe_ingredient.quantity varchar(100)
    public static final int UNIT_INPUT_LENGTH = 40;       // free-text unit ("tablespoon"); stored as a short canonical code

    private Limits() {
    }
}
