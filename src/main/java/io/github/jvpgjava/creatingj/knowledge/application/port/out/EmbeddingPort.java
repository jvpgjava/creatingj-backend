package io.github.jvpgjava.creatingj.knowledge.application.port.out;

import io.github.jvpgjava.creatingj.knowledge.domain.EmbeddingVector;

public interface EmbeddingPort {

    String embeddingVersion();

    EmbeddingVector embed(String text);
}
