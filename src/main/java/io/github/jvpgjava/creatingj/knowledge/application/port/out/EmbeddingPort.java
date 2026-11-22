package io.github.jvpgjava.creatingj.knowledge.application.port.out;

import io.github.jvpgjava.creatingj.knowledge.domain.EmbeddingVector;

public interface EmbeddingPort {

    String embeddingVersion();

    EmbeddingVector embed(String text);

    default EmbeddingVector embedDocument(String text) {
        return embed(text);
    }

    default EmbeddingVector embedQuery(String text) {
        return embed(text);
    }
}
