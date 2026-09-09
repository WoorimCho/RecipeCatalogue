package com.example.recipecatalogue.Model;

import java.util.Locale;
import java.util.Map;

/**
 * The units a {@link RecipeIngredient} amount can be expressed in.
 *
 * <p>Each carries a {@link Dimension} and a factor into that dimension's base
 * unit — grams for {@code MASS}, millilitres for {@code VOLUME}. {@code COUNT}
 * units (pieces, cloves, …) have no mass/volume equivalent without a per-item
 * weight, so the Phase 4 calculators treat those lines as "not weighable".
 *
 * <p>The API accepts unit tokens leniently (see {@link #parse}); responses emit
 * the lower-cased enum name.
 */
public enum Unit {

    // mass -> grams
    MG(Dimension.MASS, 0.001),
    G(Dimension.MASS, 1.0),
    KG(Dimension.MASS, 1000.0),
    OZ(Dimension.MASS, 28.349523125),
    LB(Dimension.MASS, 453.59237),

    // volume -> millilitres
    ML(Dimension.VOLUME, 1.0),
    L(Dimension.VOLUME, 1000.0),
    TSP(Dimension.VOLUME, 4.92892159375),
    TBSP(Dimension.VOLUME, 14.78676478125),
    CUP(Dimension.VOLUME, 236.5882365),
    FL_OZ(Dimension.VOLUME, 29.5735295625),

    // count -> "items" (base factor 1; not convertible to mass/volume here)
    PIECE(Dimension.COUNT, 1.0),
    CLOVE(Dimension.COUNT, 1.0),
    SLICE(Dimension.COUNT, 1.0),
    PINCH(Dimension.COUNT, 1.0);

    public enum Dimension { MASS, VOLUME, COUNT }

    private final Dimension dimension;
    private final double baseFactor;

    Unit(Dimension dimension, double baseFactor) {
        this.dimension = dimension;
        this.baseFactor = baseFactor;
    }

    public Dimension dimension() {
        return dimension;
    }

    /** {@code amount} of this unit, expressed in the dimension's base unit (grams or millilitres). */
    public double toBase(double amount) {
        return amount * baseFactor;
    }

    /** Lower-cased name, e.g. {@code "g"}, {@code "fl_oz"} — the form the API emits. */
    public String token() {
        return name().toLowerCase(Locale.ROOT);
    }

    private static final Map<String, Unit> ALIASES = Map.ofEntries(
            Map.entry("milligram", MG), Map.entry("milligrams", MG),
            Map.entry("gram", G), Map.entry("grams", G), Map.entry("gr", G),
            Map.entry("kilogram", KG), Map.entry("kilograms", KG), Map.entry("kilo", KG),
            Map.entry("ounce", OZ), Map.entry("ounces", OZ),
            Map.entry("pound", LB), Map.entry("pounds", LB), Map.entry("lbs", LB),
            Map.entry("millilitre", ML), Map.entry("millilitres", ML),
            Map.entry("milliliter", ML), Map.entry("milliliters", ML),
            Map.entry("litre", L), Map.entry("litres", L),
            Map.entry("liter", L), Map.entry("liters", L),
            Map.entry("teaspoon", TSP), Map.entry("teaspoons", TSP), Map.entry("t", TSP),
            Map.entry("tablespoon", TBSP), Map.entry("tablespoons", TBSP), Map.entry("tbs", TBSP), Map.entry("tbl", TBSP),
            Map.entry("cups", CUP), Map.entry("c", CUP),
            Map.entry("fluid ounce", FL_OZ), Map.entry("fluid ounces", FL_OZ), Map.entry("floz", FL_OZ),
            Map.entry("pieces", PIECE), Map.entry("pc", PIECE), Map.entry("ea", PIECE), Map.entry("each", PIECE),
            Map.entry("cloves", CLOVE),
            Map.entry("slices", SLICE),
            Map.entry("pinches", PINCH));

    /**
     * Parses a unit token, leniently: case-insensitive, trims, accepts the enum
     * name ({@code "tbsp"}) or a common alias ({@code "tablespoon"}, {@code "c"}).
     *
     * @throws IllegalArgumentException if the token is not recognised
     */
    public static Unit parse(String raw) {
        String key = raw.trim().toLowerCase(Locale.ROOT);
        for (Unit u : values()) {
            if (u.token().equals(key)) {
                return u;
            }
        }
        Unit alias = ALIASES.get(key);
        if (alias != null) {
            return alias;
        }
        throw new IllegalArgumentException("unknown unit '" + raw + "'");
    }
}
