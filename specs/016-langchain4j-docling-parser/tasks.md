# Tasks — Spec 016: Adopt DoclingDocumentParser in Mode B (DoclingNaiveExtractor)

Status: Approved

- [x] 1. Confirm dependency groundwork: `pom.xml` already carries quarkus-docling
      1.4.3 and `langchain4j-document-parser-docling` pinned to 1.20.0-beta30.
      Verify the project compiles on a single docling-java 0.6.5 classpath
      (`./mvnw -q clean compile`) and that `DoclingDocumentParser` /
      `InBodyConvertDocumentResponse` resolve. (R1)

- [x] 2. Rework `DoclingNaiveExtractor.extract(Path)` to drive Docling through
      `DoclingDocumentParser`:
      - Add a shared `CONVERT_REQUEST` constant (`ConvertDocumentRequest` with
        `ConvertDocumentOptions.toFormat(JSON)`).
      - Build a per-call parser (`.doclingClient(doclingServeApi)`,
        `.documentRequest(CONVERT_REQUEST)`, `.documentExtractor(...)`).
      - Add `toDocument(InBodyConvertDocumentResponse, AtomicReference<DoclingDocument>)`
        as a thin capture step returning `Document.from(buildFullText(doclingDoc))`.
      - Add `parseAsync(parser, path)` opening the `InputStream` and closing it via
        `whenComplete` (NOT try-with-resources).
      - Add `closeQuietly(InputStream)` logging via `io.quarkus.logging.Log`.
      - Compose the `Uni` over `parseAsync`, moving `buildProvenance` into `.map`,
        running against `document.text()`.
      - Remove the hand-built request, the `completionStage(convertFilesAsync)`
        wrapper, the `InBodyConvertDocumentResponse.class::cast`, and the inline
        json map. Keep the four mapping helpers unchanged. (R2, R3, R4, R5, R6)

- [x] 3. Update `DoclingNaiveExtractor` class/method Javadoc to describe the
      parser-driven flow and the capture-holder provenance carry. (R9)

- [x] 4. Add a `DoclingNaiveExtractorTest` case asserting the returned
      `Document` carries the `document_size_bytes` metadata key — proves the
      parser path is exercised and locks in Decision 9. (R5, R7)

- [x] 5. Run the Mode B test suite under WireMock and confirm the four existing
      black-box tests, `DocItemIndexTest`, and `NaiveChunkerTest` pass unchanged
      (`./mvnw -Duse.wiremock.docling=true test` scoped, then full). (R7, R8)

- [x] 6. Grep-verify `DoclingNaiveExtractor` contains no
      `InBodyConvertDocumentResponse.class::cast` and no
      `.await().indefinitely()`. (R3, R6)

- [x] 7. Run `./mvnw -Duse.wiremock.docling=true verify` — full unit + failsafe
      integration suite green, ITs not skipped. (R7, R8)

- [x] 8. (Optional diagnostic) `./mvnw -Drun.simulations=true test` with
      `ModeAvsModeBTest` to confirm Mode B chunk behavior is unchanged.

- [x] 9. Update documentation: `README.md`, `CONTEXT.md` (`CLAUDE.md` symlink)
      spec-016 status → implemented; record any new decision in `decisions.md`
      (with HH:MM timestamp). (R9)

- [ ] 10. Manual verification: run against Docling Serve dev services; confirm the
      Mode B embedding store populates and answers as before. **Pending** — requires
      driving the running app in a browser; browser MCP servers unavailable this
      session. The real-Docling extraction path is already confirmed by task 8
      (`ModeAvsModeBTest` against a live Docling Serve container).
