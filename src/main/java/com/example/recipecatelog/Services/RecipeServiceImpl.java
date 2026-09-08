package com.example.recipecatelog.Services;

import com.example.recipecatelog.Dto.RecipeIngredientDto;
import com.example.recipecatelog.Dto.RecipeRequest;
import com.example.recipecatelog.Dto.RecipeResponse;
import com.example.recipecatelog.Dto.RecipeStepDto;
import com.example.recipecatelog.Exception.NotFoundException;
import com.example.recipecatelog.Model.IngredientReplacement;
import com.example.recipecatelog.Model.Recipe;
import com.example.recipecatelog.Model.RecipeIngredient;
import com.example.recipecatelog.Model.RecipeStep;
import com.example.recipecatelog.Repositories.RecipeRepository;
import com.example.recipecatelog.Repositories.RecipeSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class RecipeServiceImpl implements RecipeService {

    private static final String DEFAULT_CREATOR = "anonymous";

    private final RecipeRepository recipeRepository;
    private final TagService tagService;

    public RecipeServiceImpl(RecipeRepository recipeRepository, TagService tagService) {
        this.recipeRepository = recipeRepository;
        this.tagService = tagService;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RecipeResponse> search(String name, Collection<String> tags, String match,
                                       Collection<Long> ingredientIds, Pageable pageable) {
        List<Specification<Recipe>> parts = new ArrayList<>();

        if (StringUtils.hasText(name)) {
            parts.add(RecipeSpecifications.nameContains(name));
        }
        if (ingredientIds != null && !ingredientIds.isEmpty()) {
            parts.add(RecipeSpecifications.usesAnyIngredient(ingredientIds));
        }
        Set<String> tagNames = tags == null ? Set.of() : tags.stream()
                .map(TagService::normalise)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (!tagNames.isEmpty()) {
            parts.add("any".equalsIgnoreCase(match)
                    ? RecipeSpecifications.hasAnyTag(tagNames)
                    : RecipeSpecifications.hasAllTags(tagNames));
        }

        Page<Recipe> page;
        if (parts.isEmpty()) {
            page = recipeRepository.findAll(pageable);
        } else {
            Specification<Recipe> spec = parts.get(0);
            for (int i = 1; i < parts.size(); i++) {
                spec = spec.and(parts.get(i));
            }
            page = recipeRepository.findAll(spec, pageable);
        }
        return page.map(RecipeResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public RecipeResponse get(long id) {
        return RecipeResponse.from(require(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecipeResponse> getByIds(Collection<Long> ids) {
        if (ids.size() > 500) {
            throw new IllegalArgumentException("at most 500 ids per request");
        }
        return recipeRepository.findAllById(ids).stream()
                .map(RecipeResponse::from)
                .toList();
    }

    @Override
    public RecipeResponse create(RecipeRequest request) {
        String creator = StringUtils.hasText(request.creator()) ? request.creator().trim() : DEFAULT_CREATOR;
        int version = request.version() != null
                ? request.version()
                : recipeRepository.findMaxVersion(request.name().trim(), creator) + 1;

        Recipe recipe = new Recipe(request.name().trim());
        recipe.setCreator(creator);
        recipe.setVersion(version);
        apply(recipe, request);
        return RecipeResponse.from(recipeRepository.save(recipe));
    }

    @Override
    public RecipeResponse update(long id, RecipeRequest request) {
        Recipe recipe = require(id);
        recipe.setName(request.name().trim());
        if (StringUtils.hasText(request.creator())) {
            recipe.setCreator(request.creator().trim());
        }
        if (request.version() != null) {
            recipe.setVersion(request.version());
        }
        apply(recipe, request);
        return RecipeResponse.from(recipeRepository.save(recipe));
    }

    @Override
    public void delete(long id) {
        recipeRepository.delete(require(id));
    }

    @Override
    public RecipeResponse addTags(long id, Set<String> tagNames) {
        Recipe recipe = require(id);
        tagService.resolve(tagNames).forEach(recipe::addTag);
        return RecipeResponse.from(recipeRepository.save(recipe));
    }

    @Override
    public RecipeResponse removeTag(long id, String tagName) {
        Recipe recipe = require(id);
        String normalised = TagService.normalise(tagName);
        recipe.getTags().removeIf(tag -> tag.getName().equals(normalised));
        return RecipeResponse.from(recipeRepository.save(recipe));
    }

    private void apply(Recipe recipe, RecipeRequest request) {
        recipe.replaceSteps(buildSteps(request.stepsOrEmpty()));
        recipe.replaceIngredients(buildIngredients(request.ingredientsOrEmpty()));
        recipe.replaceTags(tagService.resolve(request.tagsOrEmpty()));
    }

    private static List<RecipeStep> buildSteps(List<RecipeStepDto> dtos) {
        return dtos.stream()
                .map(dto -> new RecipeStep(dto.text().trim(), dto.toolsOrEmpty()))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private static List<RecipeIngredient> buildIngredients(List<RecipeIngredientDto> dtos) {
        List<RecipeIngredient> result = new ArrayList<>();
        for (RecipeIngredientDto dto : dtos) {
            if (dto.isReplaceable() && dto.replacementsOrEmpty().isEmpty()) {
                throw new IllegalArgumentException(
                        "ingredient " + dto.ingredientId() + " is marked replaceable but lists no replacements");
            }
            RecipeIngredient ingredient = new RecipeIngredient(dto.ingredientId());
            ingredient.setQuantity(StringUtils.hasText(dto.quantity()) ? dto.quantity().trim() : null);
            ingredient.setOptional(dto.isOptional());
            ingredient.setReplaceable(dto.isReplaceable());
            ingredient.replaceReplacements(dto.replacementsOrEmpty().stream()
                    .map(replacement -> new IngredientReplacement(replacement.ingredientId(), replacement.recipeId()))
                    .collect(Collectors.toCollection(ArrayList::new)));
            result.add(ingredient);
        }
        return result;
    }

    private Recipe require(long id) {
        return recipeRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("Recipe", id));
    }
}
