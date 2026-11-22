package io.github.jvpgjava.creatingj.knowledge.adapter.out.embedding;

import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;
import io.github.jvpgjava.creatingj.knowledge.application.port.out.EmbeddingPort;
import io.github.jvpgjava.creatingj.knowledge.domain.EmbeddingVector;
import org.springframework.ai.transformers.TransformersEmbeddingModel;

import java.util.Map;

public final class SpringAiTransformersEmbeddingAdapter implements EmbeddingPort, AutoCloseable {

    private final String versionTag;
    private final String modelUri;
    private final String tokenizerUri;
    private final String cacheDirectory;
    private final String modelOutputName;
    private final String documentPrefix;
    private final String queryPrefix;
    private final int expectedDimensions;
    private volatile TransformersEmbeddingModel delegate;

    public SpringAiTransformersEmbeddingAdapter(
            String versionTag,
            String modelUri,
            String tokenizerUri,
            String cacheDirectory,
            String modelOutputName,
            String documentPrefix,
            String queryPrefix,
            int expectedDimensions
    ) {
        this.versionTag = versionTag;
        this.modelUri = modelUri;
        this.tokenizerUri = tokenizerUri;
        this.cacheDirectory = cacheDirectory;
        this.modelOutputName = modelOutputName;
        this.documentPrefix = documentPrefix == null ? "" : documentPrefix;
        this.queryPrefix = queryPrefix == null ? "" : queryPrefix;
        this.expectedDimensions = expectedDimensions;
    }

    @Override
    public String embeddingVersion() {
        return versionTag;
    }

    @Override
    public EmbeddingVector embed(String text) {
        if (text == null || text.isBlank()) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Texto para embedding obrigatorio.");
        }
        try {
            float[] vector = getOrInitModel().embed(text.trim());
            return EmbeddingVector.ofExpectedDimensions(vector, expectedDimensions);
        } catch (DomainException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new DomainException(
                    ErrorCode.PROVIDER_UNAVAILABLE,
                    "Falha ao executar modelo local ONNX de embeddings."
            );
        }
    }

    @Override
    public EmbeddingVector embedDocument(String text) {
        if (text == null || text.isBlank()) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Texto para embedding obrigatorio.");
        }
        return embed(documentPrefix + text.trim());
    }

    @Override
    public EmbeddingVector embedQuery(String text) {
        if (text == null || text.isBlank()) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Texto para embedding obrigatorio.");
        }
        return embed(queryPrefix + text.trim());
    }

    private TransformersEmbeddingModel getOrInitModel() throws Exception {
        TransformersEmbeddingModel current = delegate;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            if (delegate == null) {
                TransformersEmbeddingModel model = new TransformersEmbeddingModel();
                model.setModelResource(modelUri);
                model.setTokenizerResource(tokenizerUri);
                model.setModelOutputName(modelOutputName);
                if (cacheDirectory != null && !cacheDirectory.isBlank()) {
                    model.setResourceCacheDirectory(cacheDirectory);
                }
                model.setTokenizerOptions(Map.of(
                        "padding", "true",
                        "truncation", "true",
                        "maxLength", "512"
                ));
                model.afterPropertiesSet();
                delegate = model;
            }
            return delegate;
        }
    }

    @Override
    public void close() {
        TransformersEmbeddingModel current = delegate;
        if (current != null) {
            try {
                current.close();
            } catch (Exception ignored) {
                delegate = null;
            }
        }
    }
}
