package dev.ericdeandrea.docling.mapping;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants.ComponentModel;

import dev.ericdeandrea.docling.model.ChunkMetadata;
import dev.ericdeandrea.docling.model.Mode;
import dev.ericdeandrea.docling.model.RetrievedChunk;
import dev.langchain4j.data.segment.TextSegment;

@Mapper(componentModel = ComponentModel.CDI)
public interface ChunkMapper {

    @Mapping(target = "text", expression = "java(segment.text())")
    @Mapping(target = "metadata", expression = "java(toMetadata(segment, relevanceScore))")
    RetrievedChunk toRetrievedChunk(TextSegment segment, double relevanceScore);

    default ChunkMetadata toMetadata(TextSegment segment, double relevanceScore) {
        var metadata = segment.metadata();

        return new ChunkMetadata(
            metadata.getInteger("page_number"),
            metadata.getString("element_type"),
            metadata.getString("element_label"),
            toMode(metadata.getString("mode")),
            relevanceScore
        );
    }

    default Mode toMode(String modeString) {
        return (modeString != null) ? Mode.valueOf(modeString) : null;
    }
}
