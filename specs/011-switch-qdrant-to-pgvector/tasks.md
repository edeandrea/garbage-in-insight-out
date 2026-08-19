# Tasks: Spec 011 — Switch Qdrant Back to pgvector

**Status:** Approved

## Checklist

- [x] 1. **Swap Maven dependency** — Replace `quarkus-langchain4j-qdrant`
      with `quarkus-langchain4j-pgvector` in `pom.xml`. Verify the
      project compiles (`mvn compile` will fail on Qdrant imports — that's
      expected; this task just swaps the dependency).

- [x] 2. **Replace Qdrant config with pgvector config** — In
      `application.yml`, remove the entire `quarkus.langchain4j.qdrant`
      block and replace it with the pgvector named-store configuration
      (`default-store-enabled: false`, three named stores with `table`
      and `dimension: 768`).

- [x] 3. **Add `hasExistingData()` to the pipeline hierarchy** — Add the
      method to `IngestionPipeline` (interface) and implement in
      `AbstractIngestionPipeline` using a zero-vector
      `EmbeddingStore.search()` with `maxResults(1)`. Update the `store`
      and `embeddingModel` field visibility in `AbstractIngestionPipeline`
      if needed.

- [x] 4. **Rename `collectionName()` to `storeName()`** — Rename the
      method in `IngestionPipeline` and update the Javadoc. Update the
      call site in `IngestionStartup`.

- [x] 5. **Rewrite `IngestionStartup`** — Remove all Qdrant imports,
      fields, and methods (`QdrantClient`, `QdrantGrpcClient`,
      `QdrantEmbeddingStoreConfig`, `VECTOR_SIZE`,
      `resolveCollectionName()`, `createCollection()`). Simplify
      constructor to just `List<IngestionPipeline>` + `DemoConfig`.
      Rewrite `onStart`, `runPipeline`, `ingestParallel`, and
      `ingestSequential` to use `pipeline.hasExistingData()` instead of
      Qdrant collection checks. Remove the try-with-resources /
      `ExecutionException` handling around the gRPC client.

- [x] 6. **Rewrite `IngestionStartupTest`** — Remove all Qdrant imports
      and the `QdrantEmbeddingStoreConfig` field. Inject the three named
      `EmbeddingStore<TextSegment>` instances. Rewrite both test methods
      to use provider-agnostic verification (zero-vector search for data
      presence, embedding dimension check from search results).

- [x] 7. **Update Javadoc and comments** — Replace "Qdrant collection"
      references in `ModeAwareRetrievalAugmentor.java`,
      `AbstractIngestionPipeline.java`, and `IngestionPipeline.java` with
      provider-neutral terminology ("embedding store" / "pgvector table").

- [x] 8. **Run full test suite** — Run `mvn verify` to confirm all tests
      pass, including the WireMock-gated Docling tests
      (`-Duse.wiremock.docling=true`).
