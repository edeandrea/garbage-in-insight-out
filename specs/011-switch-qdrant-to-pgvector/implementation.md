# Implementation: Spec 011 — Switch Qdrant Back to pgvector

## Task 1: Swap Maven dependency

Replace `quarkus-langchain4j-qdrant` with `quarkus-langchain4j-pgvector`
in `pom.xml`. Both are managed by the `quarkus-langchain4j-bom`, so no
version is needed.

## Task 2: Replace Qdrant config with pgvector config

Remove the `quarkus.langchain4j.qdrant` block from `application.yml`
and replace with pgvector named-store config. Three named stores
(`naive`, `docling-naive`, `docling-hybrid`) with explicit `table` and
`dimension: 768`. `default-store-enabled: false` since we only use
named stores. Each named store also requires `datasource: "<default>"`
as a workaround for [#2754](https://github.com/quarkiverse/quarkus-langchain4j/issues/2754)
(decision #2).

## Task 3: Add `hasExistingData()` to pipeline hierarchy

Add `hasExistingData()` to `IngestionPipeline` interface and implement
in `AbstractIngestionPipeline`. Uses `EmbeddingStore.search()` with a
unit vector (`Arrays.fill(probe, 1.0f)`) of `embeddingModel.dimension()`
length, `maxResults(1)`, and `minScore(0.0)`. A zero vector can't be
used because cosine similarity with a zero vector is undefined (NaN)
and pgvector filters out NaN results (decision #3).

## Task 4: Rename `collectionName()` to `storeName()`

Rename the method in `IngestionPipeline` interface and update the call
site in `IngestionStartup`.

## Task 5: Rewrite `IngestionStartup`

Remove all Qdrant-specific code. Constructor shrinks to
`List<IngestionPipeline>` + `DemoConfig`. The `onStart` method no
longer opens a `QdrantClient` — it just iterates pipelines. The
`runPipeline` method uses `pipeline.hasExistingData()` for the skip
check and no longer creates collections. `resolveCollectionName()` and
`createCollection()` are removed entirely.

## Task 6: Rewrite `IngestionStartupTest`

Remove all Qdrant imports and the `QdrantEmbeddingStoreConfig` field.
Inject the three named `EmbeddingStore<TextSegment>` instances via
`@EmbeddingStoreName`. Rewrite both tests:
- `allStoresPopulatedAfterStartup()`: unit-vector search against each
  store, assert matches are non-empty.
- Removed `storesConfiguredWithCorrectDimension()` — it only tested
  config passthrough (asserting the value we set in YAML flows to the
  config object), not application logic.

## Task 7: Update Javadoc and comments

Replace "Qdrant collection" references with provider-neutral terminology
in `ModeAwareRetrievalAugmentor`, `AbstractIngestionPipeline`, and
`IngestionPipeline`.
