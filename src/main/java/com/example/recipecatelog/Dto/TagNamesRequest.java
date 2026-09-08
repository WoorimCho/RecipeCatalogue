package com.example.recipecatelog.Dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

/** Body for attaching tags to a recipe: {@code {"tags": ["cuisine:thai", "quick"]}}. */
public record TagNamesRequest(@NotEmpty Set<String> tags) {
}
