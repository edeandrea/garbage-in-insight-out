---
name: capture-docling-stubs
description: >-
  Re-capture WireMock stubs from a live Docling Serve instance. Run after
  upgrading Docling or changing the fixture PDF.
metadata:
  user-invocable: true
  disable-model-invocation: true
---

Run `CaptureDoclingResponsesTest` against a live Docling Serve instance
to regenerate the WireMock stub files used by tests.

1. Run the capture test:
   ```
   ./mvnw test -pl . -Dtest="CaptureDoclingResponsesTest" -Dcapture.docling.responses=true
   ```
2. Verify both files were updated:
   - `src/test/resources/__files/docling-convert-response.json` (~970KB)
   - `src/test/resources/__files/docling-chunk-response.json` (~1.1MB)
3. Run the WireMock-dependent tests to confirm the new stubs work:
   ```
   ./mvnw test -Duse.wiremock.docling=true
   ```
4. If tests pass, report the file sizes and ask the user if they want
   to commit and push.

Note: This requires the Docling dev service container to be available.
The `CaptureDoclingResponsesTest` uses `includeConvertedDoc(true)` on
the chunk request so the stub includes the embedded `DoclingDocument`.
