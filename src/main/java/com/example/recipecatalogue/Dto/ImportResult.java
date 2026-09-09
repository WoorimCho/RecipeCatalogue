package com.example.recipecatalogue.Dto;

import java.util.List;

/**
 * Outcome of a bulk import. Always HTTP 200 — per-row problems are in
 * {@link #errors}, not fatal.
 *
 * @param rows     data rows seen (header excluded)
 * @param imported rows that created a recipe
 * @param skipped  rows that did not (see {@code errors})
 */
public record ImportResult(int rows, int imported, int skipped, List<RowError> errors) {

    public record RowError(long line, String message) {
    }
}
