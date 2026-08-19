# Spec 011: Switch Qdrant Back to pgvector

**Status:** Approved

## Summary

Replace the Qdrant embedding store with pgvector, reverting the
workaround adopted in spec 001
[decision #44](../001-three-mode-rag-demo/decisions.md#L649). The
original pgvector named-stores bug
([decisions #42–#43](../001-three-mode-rag-demo/decisions.md#L613),
quarkiverse/quarkus-langchain4j#2690 — duplicate
`PgVectorAgroalPoolInterceptor` bean when multiple named stores share
the same datasource) was fixed by PR #2693 and is available in the
project's current quarkus-langchain4j 1.13.0.

## Motivation

Qdrant was never the preferred choice. The project switched to it
([decision #44](../001-three-mode-rag-demo/decisions.md#L649),
2026-07-22) solely because pgvector's named-store support was broken.
Now that the fix has shipped, switching back removes:

- **The manual collection-creation workaround** in `IngestionStartup`
  ([decision #46](../001-three-mode-rag-demo/decisions.md#L681)) —
  pgvector auto-creates tables via dev services and Hibernate,
  eliminating the `QdrantClient` / gRPC ceremony.
- **Operational complexity** — pgvector runs inside a single PostgreSQL
  container (started by Quarkus dev services) which is a more familiar
  operational target than Qdrant's separate gRPC-based container.
- **Qdrant-specific test infrastructure** — `IngestionStartupTest`
  currently constructs a `QdrantClient` to verify collections; pgvector
  verification uses standard JDBC/datasource queries.

## Requirements

1. Replace the `quarkus-langchain4j-qdrant` Maven dependency with
   `quarkus-langchain4j-pgvector`.
2. Replace all Qdrant configuration in `application.yml` with the
   equivalent pgvector named-store configuration, preserving the three
   named stores: `naive`, `docling-naive`, and `docling-hybrid`.
3. Remove the manual Qdrant collection-creation code from
   `IngestionStartup` (and its `QdrantClient` / `QdrantGrpcClient` /
   gRPC imports). pgvector tables should be created automatically.
4. Update `IngestionStartupTest` to verify embedding-store state via
   the PostgreSQL datasource rather than a `QdrantClient`.
5. Update any Javadoc or code comments that reference Qdrant to reflect
   pgvector / PostgreSQL.
6. The three RAG modes (A / B / C) must continue to function identically
   from the user's perspective — same ingestion, same retrieval, same
   chat behavior.
7. The full test suite (`mvn verify`) must pass, including the
   WireMock-gated Docling tests.
8. Dev services must start a PostgreSQL instance with the pgvector
   extension enabled (Quarkus pgvector dev services handles this
   automatically).

## Out of scope

- Bumping quarkus-langchain4j to a newer version (already on 1.13.0,
  which contains the fix).
- Changing the embedding model, dimension, or distance metric.
- Any UI changes — the embedding store is invisible to the frontend.
- Addressing PR #2691 (Qdrant dev services `create-collections`
  simplification) — no longer relevant after this switch.

## Open questions

_None at this time._
