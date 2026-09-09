rootProject.name = "RecipeCatalogue"

// Shared with IngredientCatalogue: Tag lifecycle, the page envelope, the RFC-9457
// exception advice, the CSV reader, the Zipkin sender. Composite build so it
// always compiles from source — no publish step.
includeBuild("../catalogue-common")
