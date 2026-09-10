# RecipeCatalogue

The catalogue of **recipes** — ordered steps with tools, structured ingredient
lines, optional / replaceable ingredients with substitutes, and the shared tag
vocabulary.

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-brightgreen)
![Java](https://img.shields.io/badge/Java-17-orange)
![Build](https://img.shields.io/badge/build-Gradle-blue)
![DB](https://img.shields.io/badge/MySQL-8.4-blue)
![Port](https://img.shields.io/badge/port-8083-lightgrey)

> **Shared code** lives in [`../catalogue-common`](../catalogue-common) (Gradle
> composite build): the `Tag` entity + its service/repo, `PageResponse`, the
> RFC-9457 exception advice, the CSV reader, the Zipkin sender. The only
> Recipe-specific tag code left here is `RecipeTagOwnerCleanup` (clears tags off
> recipes before a delete/merge).

## Model

- **`Recipe`** — `id`, `name`, `creator`, `version`, `List<RecipeStep>`,
  `List<RecipeIngredient>`, `Set<Tag>`.
- **`RecipeStep`** — `position`, `text`, `Set<String> tools`.
- **`RecipeIngredient`** — `ingredientId` (cross-service, **no FK**),
  free-text `quantity` *(display)* **plus** structured **`amount`** + **`unit`**
  (a `Unit` enum: MASS→g, VOLUME→ml, COUNT→no weight — the BFF calculators bridge
  VOLUME→mass using the ingredient's `densityGPerMl`), `optional`, `replaceable`,
  `List<IngredientReplacement>` (`ingredientId` and/or `recipeId`).
- **`Tag`** — same as IngredientCatalogue (separate table).

## API — `/api/recipes`

| | |
|---|---|
| `GET /` | `?name=`, `?tag=` (+`?match=all\|any`), `?notTag=` (repeatable — exclude; ANDed with `tag`), `?ingredientId=` (repeatable), paged. Tag terms match a tag name **anywhere** — `?tag=vegan` finds `diet:vegan` recipes |
| `GET /random` | one random recipe from the filtered set — same params incl. `?notTag=` (404 if none) |
| `GET /{id}` | one |
| `GET /by-ids?id=1&id=2` | batch resolve |
| `POST /` / `PUT /{id}` | create / replace (`steps[]`, `ingredients[]` with `amount`/`unit`, `tags[]`) |
| `DELETE /{id}` | delete |
| `POST /import` (multipart `file`) | CSV skeleton import (`ImportResult`; steps added later via `PUT`) |

`/api/tags` mirrors IngredientCatalogue (`?prefix=` is a case-insensitive
**substring** match — "thai" finds "cuisine:thai"). Contract at `/openapi.yaml`.
`PageResponse<T>` envelope.

## Run

```bash
cd .. && docker compose up --build recipe-catalogue

docker compose up -d mysql        # local
./gradlew bootRun
```

## Configuration (env)

| Var | Default |
|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://localhost:3308/recipe_catalogue` |
| `SPRING_DATASOURCE_USERNAME` / `_PASSWORD` | `myuser` / `secret` |
| `SERVER_PORT` | `8083` |
| `ZIPKIN_ENDPOINT` | `http://localhost:9411/api/v2/spans` |

Flyway: `V1` full recipe model, `V2` structured-quantity columns.
`ddl-auto=validate`.

## Tests

```bash
./gradlew test   # Testcontainers MySQL: recipe/ingredient round-trip, tag
                 # search, Unit conversions, structured-quantity validation,
                 # random selector, CSV import, OpenAPI contract, ops,
                 # list-endpoint query count
```

## Performance

`RecipeResponse.from` reads five lazy collections per row (steps, step tools,
ingredients, replacements, tags), so a page of *N* recipes used to fire *N*+1
selects per collection. `spring.jpa.properties.hibernate.default_batch_fetch_size=64`
makes Hibernate load each one for up to 64 parents in a single
`… where parent_id in (?, ?, …)`. Batch fetching rather than a collection
`@EntityGraph` on purpose: join-fetching a collection with `Pageable` would push
pagination into memory. `RecipeListQueryCountDataJpaTest` pins a page of 8 to
≤ 12 queries.

## Security

No Spring Security — **every write is unauthenticated** (finding C2 in
`../IngredientCatalogue/SECURITY.md`). Keep `:8083` off untrusted networks.

## Status

**v1 complete** + Phase 4 (structured quantities, random, CSV import). Consumed
by the BFF (composed view + all three calculators) and written by
`../recipe-crawler` on URL import (recipes tagged `source:imported`, `creator`
`import:<host>`).
