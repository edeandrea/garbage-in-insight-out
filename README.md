# Garbage In, Insight Out

Demo application for the talk *"Garbage In, Insight Out: Document Intelligence for AI-Infused Java Applications"*. It demonstrates how document extraction quality and chunking strategy directly affect RAG (Retrieval-Augmented Generation) answer accuracy, using Quarkus, LangChain4j, and Docling Java.

## Prerequisites

- Java 25 (Temurin recommended)
- Maven 3.9+ (wrapper included: `./mvnw`)
- Docker (for Quarkus dev services)

## Build

```shell
./mvnw verify
```

## Fixtures

The `fixtures/` directory contains demo documents used for ingestion:

- `doclaynet-2206.01062v1.pdf` — the DocLayNet paper (arXiv:2206.01062v1), used as the primary fixture for all three RAG modes.
- `docling.pptx` — supplementary presentation material.
