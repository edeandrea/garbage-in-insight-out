# Plan: Switch Qdrant Back to pgvector

**Status:** Approved

## Approach

A straight dependency and configuration swap. The `@EmbeddingStoreName`
injection qualifiers (`"naive"`, `"docling-naive"`, `"docling-hybrid"`)
remain identical — only the backing store provider changes. The main
structural simplification is in `IngestionStartup`, which currently
manages Qdrant collections manually via `QdrantClient` / gRPC; pgvector
auto-creates tables, so that machinery is removed entirely.

## Files changed

### 1. `pom.xml`

Replace the Qdrant dependency with pgvector:

```xml
<!-- remove -->
<dependency>
    <groupId>io.quarkiverse.langchain4j</groupId>
    <artifactId>quarkus-langchain4j-qdrant</artifactId>
</dependency>

<!-- add -->
<dependency>
    <groupId>io.quarkiverse.langchain4j</groupId>
    <artifactId>quarkus-langchain4j-pgvector</artifactId>
</dependency>
```

No additional JDBC or Agroal dependencies are needed —
`quarkus-langchain4j-pgvector` transitively pulls in
`quarkus-jdbc-postgresql` and `quarkus-agroal`.

### 2. `src/main/resources/application.yml`

Replace the `quarkus.langchain4j.qdrant` block with pgvector
configuration. The pgvector runtime config
(`PgVectorStoreRuntimeConfig`) supports these key properties per store:

| Property       | Default        | Notes                              |
|----------------|----------------|------------------------------------|
| `table`        | `"embeddings"` | Table name for this store          |
| `dimension`    | (none)         | Must match embedding model output  |
| `create-table` | `true`         | Auto-creates the table at startup  |

The build-time config (`PgVectorEmbeddingStoreBuildTimeConfig`) supports:

| Property                | Default  | Notes                             |
|-------------------------|----------|-----------------------------------|
| `default-store-enabled` | `true`   | Set to `false` for named-only use |

Target configuration:

```yaml
quarkus:
  langchain4j:
    pgvector:
      default-store-enabled: false
      naive:
        datasource: "<default>"
        table: naive
        dimension: 768
      docling-naive:
        datasource: "<default>"
        table: docling_naive_chunk
        dimension: 768
      docling-hybrid:
        datasource: "<default>"
        table: docling_hybrid_chunk
        dimension: 768
```

Note: `datasource` is a build-time property required for each named
store so SmallRye Config can discover the store key at build time
([decision #2](decisions.md)).

Dev services automatically starts a `pgvector/pgvector:pg17` container
(set by the extension's `DevServicesConfigBuilderCustomizer` at priority
50, overridable). No manual datasource configuration is needed for dev
or test profiles.

### 3. `src/main/java/.../ai/ingestion/IngestionStartup.java`

**This is the biggest change.** Current responsibilities:

1. Connect to Qdrant via `QdrantClient` / `QdrantGrpcClient`
2. List existing collections
3. For each pipeline: check if its collection exists → skip or create +
   ingest

With pgvector, table creation is handled automatically
(`create-table: true`). The skip-if-already-ingested check moves into
the pipeline itself (see section 5 below), so `IngestionStartup` no
longer needs any store-specific knowledge.

**Revised approach:**

- Remove all Qdrant imports (`QdrantClient`, `QdrantGrpcClient`,
  `Distance`, `VectorParams`, `QdrantEmbeddingStoreConfig`).
- Remove the `qdrantConfig` field and constructor parameter.
- Remove `resolveCollectionName()` and `createCollection()` methods.
- Remove the `VECTOR_SIZE` constant (dimension is now in config).
- The `ingestParallel` / `ingestSequential` / `runPipeline` methods
  lose their `QdrantClient` and `List<String> existingCollections`
  parameters. `runPipeline` calls `pipeline.hasExistingData()` instead
  of checking `existingCollections.contains(collectionName)`.
- The constructor shrinks to just `List<IngestionPipeline>` +
  `DemoConfig` — no store-specific types at all.

**Simplified `onStart`:**

```java
void onStart(@Observes StartupEvent event) {
    var documentPath = Path.of(this.demoConfig.rag().fixturePath());

    Log.infof("Starting ingestion for document: %s", documentPath);

    if (this.demoConfig.rag().ingestion().parallel()) {
        Log.info("Running ingestion in parallel");
        ingestParallel(documentPath);
    }
    else {
        Log.info("Running ingestion sequentially");
        ingestSequential(documentPath);
    }

    Log.info("Ingestion complete");
}
```

No try-with-resources block for a gRPC client, no
`ExecutionException | InterruptedException` catch — the method becomes
straightforward.

### 4. `src/test/java/.../ai/ingestion/IngestionStartupTest.java`

Replace Qdrant-based verification with provider-agnostic verification:

- Remove all Qdrant imports (`QdrantClient`, `QdrantGrpcClient`,
  `Distance`, `VectorParams`, `QdrantEmbeddingStoreConfig`).
- Inject the three named `EmbeddingStore<TextSegment>` instances via
  `@EmbeddingStoreName` (same qualifiers the pipelines use).
- `skipsIngestionWhenCollectionExists()` → verify each store has data
  using the same zero-vector search approach that `hasExistingData()`
  uses. Rename to something like `allStoresPopulatedAfterStartup()`.
- `collectionsCreatedWithCorrectConfig()` → verify that at least one
  stored embedding has the expected dimensionality (768) by inspecting
  the `Embedding` returned from a search result. Rename to something
  like `storedEmbeddingsHaveCorrectDimension()`.

### 5. `src/main/java/.../ai/ingestion/pipeline/IngestionPipeline.java` and `AbstractIngestionPipeline.java`

Two changes (see [decision #1](decisions.md)):

**a) Add `hasExistingData()` to the pipeline hierarchy.**

Add a default method on `IngestionPipeline` (or implement in
`AbstractIngestionPipeline`) that searches the store with a zero-filled
`Embedding` of the configured dimension and `maxResults(1)`. If any
match comes back, data exists. This is provider-agnostic — it uses the
langchain4j `EmbeddingStore` API, not SQL or any store-specific type.

The dimension (768) can be obtained from the `EmbeddingModel` already
injected into `AbstractIngestionPipeline`, or passed as a constant.
`EmbeddingModel.dimension()` is the cleanest source if available.

```java
boolean hasExistingData() {
    var zeroVector = new float[this.embeddingModel.dimension()];
    var result = this.store.search(
        EmbeddingSearchRequest.builder()
            .queryEmbedding(Embedding.from(zeroVector))
            .maxResults(1)
            .build());

    return !result.matches().isEmpty();
}
```

**b) Rename `collectionName()` to `storeName()`** — "collection" is
Qdrant-specific. The default implementation already delegates to
`mode().storeName()`, so only the method name and its Javadoc change.
Update the single call site in `IngestionStartup`.

### 6. Javadoc and comment updates

Files with Qdrant references in comments or Javadoc:

| File | Change |
|------|--------|
| `ModeAwareRetrievalAugmentor.java` | Lines 22, 34, 58: "Qdrant collection" → "pgvector table" (or just "embedding store") |
| `AbstractIngestionPipeline.java` | Line 19: "storing into Qdrant" → "storing into pgvector" |
| `IngestionPipeline.java` | Line 15: "Qdrant collection" → "embedding store table" (line 31 handled by the `storeName()` rename above) |

### 7. No changes needed

These files use `@EmbeddingStoreName` and `EmbeddingStore<TextSegment>`
abstractions — they are provider-agnostic and require no modification:

- `TikaNaiveIngestionPipeline.java`
- `DoclingNaiveIngestionPipeline.java`
- `DoclingHybridIngestionPipeline.java`
- `ModeAwareRetrievalAugmentor.java` (aside from comments)
- `Mode.java`

## Key trade-offs

1. **Provider-agnostic skip check via zero-vector search**
   ([decision #1](decisions.md)). Uses `EmbeddingStore.search()` with a
   zero-filled vector instead of raw SQL against the datasource. Slightly
   less efficient than `SELECT EXISTS`, but negligible for a one-time
   startup check and keeps `IngestionStartup` completely decoupled from
   the store provider. If pgvector is ever swapped again, the skip check
   needs no changes.

2. **Table naming.** We reuse the existing collection names (`naive`,
   `docling_naive_chunk`, `docling_hybrid_chunk`) as pgvector table
   names for continuity with the decision log. These are set via the
   `table` config property on each named store.

## Open questions

_None at this time._
