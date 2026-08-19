# Decisions: Spec 011 — Switch Qdrant Back to pgvector

## 1. [2026-08-18 18:45 EDT]: Skip-ingestion check — provider-agnostic via EmbeddingStore, not raw SQL

**Question:** How should `IngestionStartup` check whether a store
already has data (to skip re-ingestion on restart)? The Qdrant
implementation used `QdrantClient.listCollectionsAsync()` — what replaces
it for pgvector?

**Options considered:**

1. **Raw SQL via `DataSource` + `PgVectorEmbeddingStoreConfig`** —
   Inject the datasource and config, resolve the table name per named
   store, run `SELECT EXISTS (SELECT 1 FROM "<table>")`. Direct and
   explicit, but couples `IngestionStartup` to both pgvector config
   types and raw JDBC.

2. **`EmbeddingStore.search()` with a zero vector** — Search with a
   zero-filled `Embedding` of the correct dimension and `maxResults(1)`.
   If matches come back, data exists. Fully provider-agnostic, no SQL or
   JDBC. Slightly less efficient than a SQL `EXISTS` but negligible for a
   one-time startup check.

3. **Push the check into `IngestionPipeline` / `AbstractIngestionPipeline`** —
   Add a `hasExistingData()` method on the pipeline interface, implemented
   in `AbstractIngestionPipeline` using the `EmbeddingStore` it already
   holds. `IngestionStartup` calls `pipeline.hasExistingData()` and
   doesn't need to know about tables, datasources, or config resolution.

**Decision:** Options 2 + 3 combined. Add `hasExistingData()` to
`IngestionPipeline` (default method on the interface, implemented in
`AbstractIngestionPipeline` using a zero-vector search). This keeps the
check provider-agnostic, removes `IngestionStartup`'s dependency on any
store-specific config types, and follows the existing pattern where
pipeline responsibilities stay inside the pipeline hierarchy.
`IngestionStartup` no longer needs to inject `DataSource` or
`PgVectorEmbeddingStoreConfig` — it only needs `List<IngestionPipeline>`
and `DemoConfig`, which it already has.

## 2. [2026-08-18 20:22 EDT]: pgvector named stores require explicit `datasource` build-time property

**Question:** Build failed with `UnsatisfiedResolutionException` for all
three named `EmbeddingStore` beans. The pgvector extension's build-time
processor wasn't creating any named store beans. Why?

**Root cause:** `PgVectorEmbeddingStoreProcessor.createBean()` discovers
named stores by iterating `buildTimeConfig.namedConfig().entrySet()`.
The build-time named store config (`PgVectorNamedStoreBuildTimeConfig`)
has exactly one property: `datasource`. SmallRye Config populates the
`@WithDefaults` map by scanning config sources for keys matching the
build-time config group's properties. Since our YAML only set runtime
properties (`table`, `dimension`), the build-time map was empty — no
beans were created.

Every named-store test in the quarkus-langchain4j repo sets `datasource`
explicitly, including
`PgVectorMultipleNamedStoresSameDataStoreTest.java` which uses
`datasource: "<default>"` for stores sharing the default datasource.

**Decision:** Add `datasource: "<default>"` to each named store in
`application.yml` as a workaround. The `<default>` sentinel is
recognized by `NamedConfigUtil.isDefault()` and tells the processor to
use the default Agroal datasource. Filed upstream as
[quarkiverse/quarkus-langchain4j#2754](https://github.com/quarkiverse/quarkus-langchain4j/issues/2754) —
named stores should be discoverable from runtime properties alone.

## 3. [2026-08-18 20:22 EDT]: Use unit vector instead of zero vector for `hasExistingData()` probe

**Question:** The `hasExistingData()` zero-vector search returned no
results even when the pgvector table had data. Why?

**Root cause:** pgvector uses cosine distance. Cosine similarity with a
zero vector is undefined (0/0 → NaN). Results with NaN scores are
filtered out by `EmbeddingSearchRequest`'s `minScore` threshold.

**Decision:** Use a vector filled with `1.0f` instead of `0.0f`. This
produces valid cosine similarity values with any stored vector. Also set
`minScore(0.0)` to accept any match — we only care about existence, not
relevance.
