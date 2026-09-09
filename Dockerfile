# syntax=docker/dockerfile:1
#
# Build context is the PARENT directory (Projects/), not this folder, because the
# Gradle build is a composite that pulls in ../catalogue-common. The root
# compose.yaml sets:
#     build: { context: ., dockerfile: RecipeCatalogue/Dockerfile }
# To build standalone:  docker build -f RecipeCatalogue/Dockerfile .   (from Projects/)

# ── Stage 1: build the boot jar ───────────────────────────────────────────────
FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace

# Shared module first (its own layer — changes rarely).
COPY catalogue-common/settings.gradle.kts catalogue-common/build.gradle.kts ./catalogue-common/
COPY catalogue-common/src ./catalogue-common/src

WORKDIR /workspace/RecipeCatalogue
# Wrapper first so the Gradle download layer caches independently of sources.
COPY RecipeCatalogue/gradlew ./
COPY RecipeCatalogue/gradle ./gradle
RUN chmod +x ./gradlew

COPY RecipeCatalogue/settings.gradle.kts RecipeCatalogue/build.gradle.kts ./
COPY RecipeCatalogue/src ./src

# --mount=type=cache keeps the Gradle dependency cache across builds (BuildKit).
RUN --mount=type=cache,target=/root/.gradle,id=recipecatalogue-gradle ./gradlew --no-daemon clean bootJar -x test

# ── Stage 2: minimal runtime ─────────────────────────────────────────────────
FROM eclipse-temurin:17-jre AS runtime
WORKDIR /app

RUN groupadd --system spring && useradd --system --gid spring spring
USER spring

COPY --from=build /workspace/RecipeCatalogue/build/libs/*.jar app.jar

EXPOSE 8083
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
