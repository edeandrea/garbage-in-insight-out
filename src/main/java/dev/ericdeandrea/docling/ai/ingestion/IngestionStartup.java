package dev.ericdeandrea.docling.ai.ingestion;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import io.quarkus.arc.All;
import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;

import io.smallrye.mutiny.Uni;

import dev.ericdeandrea.docling.ai.ingestion.pipeline.IngestionPipeline;
import dev.ericdeandrea.docling.config.DemoConfig;

@ApplicationScoped
class IngestionStartup {

    private final List<IngestionPipeline> pipelines;
    private final DemoConfig demoConfig;

    IngestionStartup(
            @All List<IngestionPipeline> pipelines,
            DemoConfig demoConfig) {
        this.pipelines = pipelines;
        this.demoConfig = demoConfig;
    }

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

    private void ingestParallel(Path documentPath) {
        var unis = this.pipelines.stream()
            .map(pipeline -> runPipeline(pipeline, documentPath))
            .toList();

        Uni.join()
            .all(unis)
            .andCollectFailures()
            .onFailure()
            .invoke(t -> Log.error("Ingestion failed for one or more modes", t))
            .await()
            .atMost(Duration.ofMinutes(10));
    }

    private void ingestSequential(Path documentPath) {
        this.pipelines.forEach(pipeline ->
            runPipeline(pipeline, documentPath)
                .await()
                .atMost(Duration.ofMinutes(10)));
    }

    private Uni<Void> runPipeline(IngestionPipeline pipeline, Path documentPath) {
        if (pipeline.hasExistingData()) {
            Log.infof("%s store '%s' already has data, skipping ingestion",
                pipeline.mode().displayLabel(), pipeline.storeName());
            return Uni.createFrom().voidItem();
        }

        Log.infof("Ingesting %s...", pipeline.mode().displayLabel());

        return pipeline.processAndStore(documentPath)
            .invoke(segments -> Log.infof("%s ingested %d segments",
                pipeline.mode().displayLabel(), segments.size()))
            .replaceWithVoid();
    }
}
