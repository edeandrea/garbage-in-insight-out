# Plan 000: Project Skeleton

Status: Approved

## Approach

Use `quarkus_create` from the Quarkus Agent MCP tooling to scaffold the
project as a single Quarkus application — this ensures correct project
structure, latest BOM version, and idiomatic defaults. Adjust the
generated POM to match the spec's coordinates and conventions.

CI and Dependabot live under `.github/`. One README at the root.

## Version policy (R8)

All versions (Quarkus BOM, Maven plugins, AssertJ, GitHub Actions,
etc.) must be resolved at implementation time by checking the actual
upstream sources (Maven Central, GitHub Marketplace) — not from
training-data assumptions. Pin to the latest stable release of each.

## Files to create

```
pom.xml
src/main/java/dev/ericdeandrea/docling/DoclingDemoApp.java
src/main/resources/application.properties
src/test/java/dev/ericdeandrea/docling/DoclingDemoAppTest.java
.github/workflows/build.yml
.github/workflows/dependabot-auto-merge.yml
.github/dependabot.yml
README.md
```

## POM (`pom.xml`)

- `<groupId>dev.ericdeandrea</groupId>`,
  `<artifactId>garbage-in-insight-out</artifactId>`,
  `<version>1.0.0</version>`
- Properties:
  - `maven.compiler.release` = `25`
  - `quarkus.platform.group-id` = `io.quarkus.platform`
  - `quarkus.platform.version` = (latest stable)
  - `compiler-plugin.version`, `surefire-plugin.version` = (latest stable)
- `<dependencyManagement>`: import `io.quarkus.platform:quarkus-bom`,
  plus `org.assertj:assertj-core` (test scope, version looked up from
  Maven Central at implementation time per R8)
- `<pluginManagement>`: `quarkus-maven-plugin` (with
  `<extensions>true</extensions>`), `maven-compiler-plugin`,
  `maven-surefire-plugin`, `maven-failsafe-plugin`

## Smoke-test class

**`DoclingDemoApp.java`** — a package-private CDI bean. Placeholder
only; spec 001 replaces it with real functionality.

```java
package dev.ericdeandrea.docling;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
class DoclingDemoApp {
}
```

**`DoclingDemoAppTest.java`** — a `@QuarkusTest` that injects the bean
and verifies it is not null.

```java
package dev.ericdeandrea.docling;

import static org.assertj.core.api.Assertions.assertThat;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
class DoclingDemoAppTest {
    @Inject
    DoclingDemoApp app;

    @Test
    void appStarts() {
        assertThat(app).isNotNull();
    }
}
```

## GitHub Actions CI (`.github/workflows/build.yml`)

Single build job — no matrix needed for a single module.

```yaml
name: Build

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

jobs:
  build:
    runs-on: ubuntu-latest
    name: Build
    steps:
      - name: Checkout
        uses: actions/checkout@v7
      - name: Set up Java
        uses: actions/setup-java@v5
        with:
          distribution: temurin
          java-version: '25'
          cache: maven
      - name: Build and test
        run: ./mvnw -B verify
```

`setup-java`'s built-in `cache: maven` handles `~/.m2/repository`
caching — no separate `actions/cache` step needed.

## Dependabot auto-merge (`.github/workflows/dependabot-auto-merge.yml`)

A second workflow that auto-approves and auto-merges Dependabot PRs
after CI passes.

```yaml
name: Dependabot auto-merge

on: pull_request

permissions:
  contents: write
  pull-requests: write

jobs:
  auto-merge:
    runs-on: ubuntu-latest
    if: github.actor == 'dependabot[bot]'
    steps:
      - name: Approve PR
        run: gh pr review --approve "$PR_URL"
        env:
          PR_URL: ${{ github.event.pull_request.html_url }}
          GH_TOKEN: ${{ secrets.GITHUB_TOKEN }}
      - name: Enable auto-merge
        run: gh pr merge --auto --squash "$PR_URL"
        env:
          PR_URL: ${{ github.event.pull_request.html_url }}
          GH_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

## Branch protection (via `gh` CLI)

After the skeleton is implemented and CI is passing, configure branch
protection on `main` using the GitHub API.

**Bypass list:**
- Organization admins
- Repository admins

**Require PRs before merging:**
- Dismiss stale PR approvals upon new commits
- Squash is the only allowed merge method

**Require status checks to pass:**
- Require branches to be up to date before merging
- Required check: "Build"

**Repo setting:**
- Enable "Allow auto-merge" (required for the Dependabot auto-merge
  workflow's `gh pr merge --auto` to work)

All of the above is done via `gh` CLI / GitHub API commands at the end
of implementation, not as a repo artifact. The exact commands will be
determined at implementation time based on the current GitHub API.

## Dependabot (`.github/dependabot.yml`)

```yaml
version: 2
updates:
  - package-ecosystem: maven
    directory: /
    schedule:
      interval: weekly
  - package-ecosystem: github-actions
    directory: /
    schedule:
      interval: weekly
```

## README

**Root `README.md`**: project title ("Garbage In, Insight Out"), one
paragraph tying it to the talk, sections for Prerequisites (Java 25,
Maven, Docker), Build (`./mvnw verify`), and Fixtures (what's in the
directory and why).

## Tradeoffs considered

1. **Single module vs. multi-module.** Collapsed to a single module.
   Ingestion and chat code are separated by package for on-screen
   readability (spec 001 requirement 7), not by Maven module.

2. **`actions/cache` vs. `setup-java` built-in caching.** Using
   `setup-java`'s `cache: maven` — simpler, one fewer action to
   maintain, and functionally equivalent for Maven projects.
