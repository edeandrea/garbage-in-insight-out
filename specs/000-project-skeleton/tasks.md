# Tasks 000: Project Skeleton

Status: Approved

## Checklist

- [x] 1. **Scaffold project** — Use `quarkus_create` to generate the
      project. Adjust the POM to match spec coordinates
      (`dev.ericdeandrea:garbage-in-insight-out:1.0.0`). Look up latest
      stable versions of all dependencies from upstream sources (R8).
      Verify `./mvnw compile` succeeds.

- [x] 2. **Add smoke-test class** — Write `DoclingDemoApp.java`
      (`@ApplicationScoped` CDI bean) and `DoclingDemoAppTest.java`
      (`@QuarkusTest` injecting and asserting the bean). Verify
      `./mvnw verify` passes.

- [x] 3. **Create GitHub Actions CI workflow** — Write
      `.github/workflows/build.yml`. Look up latest stable versions of
      `actions/checkout` and `actions/setup-java` from GitHub
      Marketplace (R8).

- [x] 4. **Create Dependabot config** — Write
      `.github/dependabot.yml` watching `maven` and `github-actions`
      ecosystems, weekly schedule.

- [x] 5. **Create Dependabot auto-merge workflow** — Write
      `.github/workflows/dependabot-auto-merge.yml` that auto-approves
      and squash-merges Dependabot PRs after CI passes.

- [x] 6. **Write README** — Root `README.md` (title, description,
      prerequisites, build, fixtures).

- [x] 7. **Remove old multi-module structure** — Delete `ingestion/`
      and `chat/` directories, remove parent POM's `<modules>` section,
      consolidate into single project.

- [x] 8. **Push and verify CI** — Push to `main`, confirm CI job
      passes on GitHub Actions.

- [x] 9. **Configure branch protection** — Via `gh` CLI / GitHub API:
      enable auto-merge repo setting, update branch protection on
      `main` (require PRs, dismiss stale approvals, squash-only,
      require "Build" check, require branches up to date, bypass for
      org and repo admins).
