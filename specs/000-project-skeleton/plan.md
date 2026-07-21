# Plan 000: Project Skeleton

Status: Approved

## Approach

Hand-write the parent POM (it's a plain aggregator, not a Quarkus app).
Use `quarkus_create` from the Quarkus Agent MCP tooling to scaffold each
child module — this ensures correct Quarkus project structure, latest
BOM version, and idiomatic defaults. After scaffolding, adjust each
child POM to reference the parent and move shared configuration
(dependency/plugin management) up into the parent.

CI and Dependabot live under `.github/`. READMEs at root and in each
module.

No shared/common module — premature at this stage. If ingestion and
chat need shared types later, a spec can add one.

## Version policy (R8)

All versions (Quarkus BOM, Maven plugins, AssertJ, GitHub Actions,
etc.) must be resolved at implementation time by checking the actual
upstream sources (Maven Central, GitHub Marketplace) — not from
training-data assumptions. Pin to the latest stable release of each.

## Files to create

```
pom.xml                                          (parent/reactor)
ingestion/pom.xml
ingestion/src/main/java/dev/ericdeandrea/docling/ingestion/IngestionApp.java
ingestion/src/main/resources/application.properties
ingestion/src/test/java/dev/ericdeandrea/docling/ingestion/IngestionAppTest.java
ingestion/README.md
chat/pom.xml
chat/src/main/java/dev/ericdeandrea/docling/chat/ChatApp.java
chat/src/main/resources/application.properties
chat/src/test/java/dev/ericdeandrea/docling/chat/ChatAppTest.java
chat/README.md
.github/workflows/build.yml
.github/workflows/dependabot-auto-merge.yml
.github/dependabot.yml
README.md
```

## Parent POM (`pom.xml`)

- `<packaging>pom</packaging>`
- `<groupId>dev.ericdeandrea</groupId>`,
  `<artifactId>garbage-in-garbage-out</artifactId>`, `<version>1.0.0</version>`
- Properties:
  - `maven.compiler.release` = `25`
  - `quarkus.platform.group-id` = `io.quarkus.platform`
  - `quarkus.platform.version` = (latest stable)
  - `compiler-plugin.version`, `surefire-plugin.version` = (latest stable)
- `<dependencyManagement>`: import `io.quarkus.platform:quarkus-bom`,
  plus `org.assertj:assertj-core` (test scope, version looked up from
  Maven Central at implementation time per R8) so both modules inherit
  AssertJ for assertions
- `<pluginManagement>`: `quarkus-maven-plugin` (with
  `<extensions>true</extensions>`), `maven-compiler-plugin`,
  `maven-surefire-plugin`, `maven-failsafe-plugin`
- `<modules>`: `ingestion`, `chat`

Surefire and failsafe are configured once in the parent. The
`quarkus-maven-plugin` goes in `<pluginManagement>` in the parent so
child modules can activate it without repeating the version.

## Child modules (scaffolded via `quarkus_create`)

Each module is created using the Quarkus Agent MCP `quarkus_create`
tool, which generates the correct project structure, POM, and
`application.properties` with the latest stable Quarkus version.

After scaffolding, each child POM is adjusted to:

- Set `<parent>` pointing to the root POM (relative path `../pom.xml`)
- Remove duplicated `<dependencyManagement>` and `<pluginManagement>`
  (now in the parent)
- Keep whatever dependencies `quarkus_create` generates (the tooling
  knows the current artifact names — e.g., the test dependency is now
  `quarkus-junit`, not the old `quarkus-junit5`)
- Keep `quarkus-maven-plugin` in `<plugins>` (no version — inherits
  from parent `pluginManagement`)

No feature-specific extensions (REST, LangChain4j, etc.) at this stage.
Those arrive with spec 001.

## Smoke-test classes

### `ingestion` module

**`IngestionApp.java`** — a `@QuarkusMain` command-mode application that
exits immediately with return code 0. This establishes the module's
identity as a CLI/command-mode app from the start.

```java
package dev.ericdeandrea.docling.ingestion;

import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain
public class IngestionApp implements QuarkusApplication {
    @Override
    public int run(String... args) {
        return 0;
    }
}
```

**`IngestionAppTest.java`** — a `@QuarkusTest` annotated with
`@LaunchResult` that verifies the app exits with code 0.

```java
package dev.ericdeandrea.docling.ingestion;

import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusMainTest
class IngestionAppTest {
    @Test
    @Launch
    void appStarts(LaunchResult result) {
        assertThat(result.exitCode()).isZero();
    }
}
```

### `chat` module

**`ChatApp.java`** — a package-private CDI bean with a no-op `@PostConstruct`
method. Placeholder only; spec 001 replaces it with real endpoints.

```java
package dev.ericdeandrea.docling.chat;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ChatApp {
}
```

**`ChatAppTest.java`** — a `@QuarkusTest` that injects the bean and
verifies it is not null.

```java
package dev.ericdeandrea.docling.chat;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class ChatAppTest {
    @Inject
    ChatApp chatApp;

    @Test
    void appStarts() {
        assertThat(chatApp).isNotNull();
    }
}
```

## GitHub Actions CI (`.github/workflows/build.yml`)

Matrix strategy — each module builds in parallel, giving granular
status checks for branch protection.

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
    strategy:
      matrix:
        module: [ingestion, chat]
    name: Build ${{ matrix.module }}
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
        run: ./mvnw -B -f ${{ matrix.module }} verify
```

`-f <module>` points Maven at that module's `pom.xml` directly,
resolving the parent as needed. Each matrix leg produces its own check
status (e.g., "Build ingestion", "Build chat") that branch protection
rules can require individually.

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
      - name: Enable auanythinto-merge
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
- Required checks:
  - "Build ingestion"
  - "Build chat"

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

## READMEs

**Root `README.md`**: project title ("Garbage In, Insight Out"), one
paragraph tying it to the talk, sections for Prerequisites (Java 25,
Maven, Docker), Build (`./mvnw verify`), Modules (brief description of
each), and Fixtures (what's in the directory and why).

**`ingestion/README.md`**: one paragraph — pre-demo ingestion pipeline
that extracts, chunks, embeds, and stores documents.

**`chat/README.md`**: one paragraph — live-demo Quarkus web app serving
the RAG chatbot UI.

## Tradeoffs considered

1. **Single module vs. multi-module.** Single is simpler but spec 001
   explicitly needs ingestion code presentable on its own, separate from
   the chat app. Two modules is the minimum that satisfies that.

2. **Shared/common module.** Not adding one now. If ingestion and chat
   need shared types, a future spec can introduce it. Premature
   abstraction otherwise.

3. **`rest-assured` in skeleton tests.** Omitting it from the chat
   module's skeleton test — the smoke test only checks CDI context
   startup, not HTTP endpoints. It'll arrive with spec 001 when
   real endpoints exist.

4. **`actions/cache` vs. `setup-java` built-in caching.** Using
   `setup-java`'s `cache: maven` — simpler, one fewer action to
   maintain, and functionally equivalent for Maven projects.
