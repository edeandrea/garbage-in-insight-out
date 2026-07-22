package dev.ericdeandrea.docling.mapping;

import java.time.Instant;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import dev.ericdeandrea.docling.model.ChunkMetadata;
import dev.ericdeandrea.docling.model.Mode;
import dev.ericdeandrea.docling.model.RetrievedChunk;
import dev.langchain4j.data.segment.TextSegment;

@Mapper(componentModel = MappingConstants.ComponentModel.CDI)
public interface ChunkMapper {

    default RetrievedChunk toRetrievedChunk(TextSegment segment, double relevanceScore, Instant timestamp) {
        var metadata = segment.metadata();
        var pageNumber = metadata.getInteger("page_number");
        var elementType = metadata.getString("element_type");
        var elementLabel = metadata.getString("element_label");
        var modeString = metadata.getString("mode");
        var mode = modeString != null ? Mode.valueOf(modeString) : null;

        return new RetrievedChunk(
            segment.text(),
            new ChunkMetadata(pageNumber, elementType, elementLabel, mode, relevanceScore, timestamp)
        );
    }
}
