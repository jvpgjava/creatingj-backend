package io.github.jvpgjava.creatingj.knowledge.domain;

import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;

import java.util.Arrays;

public final class EmbeddingVector {

    public static final int EXPECTED_DIMENSIONS = 384;

    private final float[] values;

    public EmbeddingVector(float[] values) {
        if (values == null || values.length != EXPECTED_DIMENSIONS) {
            throw new DomainException(
                    ErrorCode.VALIDATION_ERROR,
                    "Vetor de embedding deve possuir exatamente " + EXPECTED_DIMENSIONS + " dimensoes."
            );
        }
        float[] copy = Arrays.copyOf(values, values.length);
        for (float v : copy) {
            if (!Float.isFinite(v)) {
                throw new DomainException(ErrorCode.VALIDATION_ERROR, "Vetor de embedding contem valor invalido.");
            }
        }
        this.values = copy;
    }

    public float[] values() {
        return Arrays.copyOf(values, values.length);
    }

    public int dimensions() {
        return values.length;
    }

    public double cosineSimilarity(EmbeddingVector other) {
        if (other == null) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Vetor de comparacao obrigatorio.");
        }
        double dot = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < values.length; i++) {
            double a = values[i];
            double b = other.values[i];
            dot += a * b;
            normA += a * a;
            normB += b * b;
        }
        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    public String toPgVectorLiteral() {
        StringBuilder sb = new StringBuilder(values.length * 10 + 2);
        sb.append('[');
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(values[i]);
        }
        sb.append(']');
        return sb.toString();
    }
}
