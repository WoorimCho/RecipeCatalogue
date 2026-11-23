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
- **`RecipeIngredient`** — `ingredientId` (cross-service, **no FK**; must be a
  **positive** id — see validation below), free-text `quantity` *(display)*
  **plus** structured **`amount`** + **`unit`**
  (a `Unit` enum: MASS→g, VOLUME→ml, COUNT→no weight — the BFF calculators bridge
  VOLUME→mass using the ingredient's `densityGPerMl`), `optional`, `replaceable`,
  `List<IngredientReplacement>` (`ingredientId` and/or `recipeId`).
- **`Tag`** — same as IngredientCatalogue (separate table).

## API — `/api/recipes`

| | |
|---|---|
| `GET /` | `?name=`, `?tag=` (+`?match=all\|any`), `?notTag=` (repeatable — exclude; ANDed with `tag`), `?ingredientId=` (repeatable), paged, `?sort=` (`name` / `creator` / `version`). Tag terms match a tag name **anywhere** — `?tag=vegan` finds `diet:vegan` recipes |
| `GET /random` | one random recipe from the filtered set — same params incl. `?notTag=` (404 if none) |
| `GET /{id}` | one |
| `GET /by-ids?id=1&id=2` | batch resolve |
| `POST /` / `PUT /{id}` | create / replace (`steps[]`, `ingredients[]` with `amount`/`unit`, `tags[]`) |
| `DELETE /{id}` | delete |
| `POST /import` (multipart `file`) | CSV skeleton import (`ImportResult`; steps added later via `PUT`) |

**Validation** (create / replace / each CSV row): a recipe needs **≥ 1
ingredient** (empty → 400 "a recipe needs at least one ingredient"); every
`ingredientId` (lines *and* replacements) must be **positive** (`0` / negative →
400). Existence of the referenced ingredient is enforced at the UI, not here —
the catalogues stay decoupled (no RecipeCatalogue → IngredientCatalogue call).

**Limits** (finding N4; `Dto/Limits`): ≤ 200 steps (text ≤ 20 000 chars, ≤ 30
tools of ≤ 100 chars), ≤ 100 ingredient lines (≤ 10 replacements each, `quantity`
≤ 100 chars), ≤ 50 tags of ≤ 100 chars, `name` / `creator` ≤ 255. Over a limit →
400 with the offending field; `RecipeServiceImpl.apply` re-checks the counts so a
CSV row can't bypass Bean Validation. CSV: ≤ 5 000 rows, ≤ 100 fields per row.

`/api/tags` mirrors IngredientCatalogue (`?prefix=` is a case-insensitive
**substring** match — "thai" finds "cuisine:thai"). Contract at `/openapi.yaml`.
`PageResponse<T>` envelope.

## Run

```bash
# once: DB passwords + the HMAC secret live in a git-ignored .env, never in the repo
cp .env.example .env              # fill in MYSQL_ROOT_PASSWORD, MYSQL_PASSWORD, INTERNAL_AUTH_SECRET (openssl rand -hex 24 each)

cd .. && docker compose up --build recipe-catalogue    # root stack: reads ../.env

docker compose up -d mysql        # local
./gradlew bootRun                 # Spring Boot's docker-compose support reads .env
```

`compose.yaml` refuses to start while a password or the secret is unset or empty
(findings H4, N1). `bootRun` also reads `INTERNAL_AUTH_SECRET` from `.env` (via
`spring.config.import`), so one file serves both.
If a container from before this change still exists, its old password survives
the new `.env` — `docker compose down -v` first (this wipes that dev database).

## Configuration (env)

| Var | Default |
|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://localhost:3308/recipe_catalogue` |
| `SPRING_DATASOURCE_USERNAME` | `myuser` |
| `SPRING_DATASOURCE_PASSWORD` | **none** — set from `MYSQL_PASSWORD` (`.env`); the app fails to connect without it |
| `INTERNAL_AUTH_SECRET` | **none** — the HMAC secret every service shares (at least 32 characters), from `.env`; the service refuses to start without it (finding N1) |
| `SERVER_PORT` | `8083` |
| `ZIPKIN_ENDPOINT` | `http://localhost:9411/api/v2/spans` |

Flyway: `V1` full recipe model, `V2` structured-quantity columns.
`ddl-auto=validate`.

## Tests

```bash
./gradlew test   # Testcontainers MySQL: recipe/ingredient round-trip, tag
                 # search, Unit conversions, structured-quantity + ingredient-
                 # count + positive-id validation, random selector, CSV import,
                 # OpenAPI contract, ops, list-endpoint query count
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

Reads are open; every write (`POST` / `PUT` / `DELETE` / `PATCH` under `/api/**`)
needs a valid HMAC `X-Internal-Auth` header from an internal caller — the shared
`catalogue-common` `InternalAuthFilter` (finding C2, fixed 2026-09-09). The
secret (`INTERNAL_AUTH_SECRET`) has **no default** (finding N1, fixed 2026-11-02): the
service refuses to start without a random value of at least 32 characters, from
`.env` locally or the environment in a container. See
`../IngredientCatalogue/SECURITY.md`.

## Status

**v1 complete** + Phase 4 (structured quantities, random, CSV import). Consumed
by the BFF (composed view + all three calculators) and written by
`../recipe-crawler` on URL import (recipes tagged `source:imported`, `creator`
`import:<host>`).
