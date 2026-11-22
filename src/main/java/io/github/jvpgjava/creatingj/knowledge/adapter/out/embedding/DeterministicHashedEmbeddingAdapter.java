package io.github.jvpgjava.creatingj.knowledge.adapter.out.embedding;

import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;
import io.github.jvpgjava.creatingj.knowledge.application.port.out.EmbeddingPort;
import io.github.jvpgjava.creatingj.knowledge.domain.EmbeddingVector;

import java.text.Normalizer;
import java.util.Locale;

public final class DeterministicHashedEmbeddingAdapter implements EmbeddingPort {

    public static final String VERSION = "local-hash-v1-384";

    @Override
    public String embeddingVersion() {
        return VERSION;
    }

    @Override
    public EmbeddingVector embed(String text) {
        if (text == null || text.isBlank()) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Texto para embedding obrigatorio.");
        }
        String normalized = normalize(text);
        float[] vector = new float[EmbeddingVector.EXPECTED_DIMENSIONS];
        String[] tokens = normalized.split("[^a-z0-9_]+");
        for (String token : tokens) {
            if (token.isEmpty()) {
                continue;
            }
            accumulateFeature(vector, "t:" + token, 2.0f);
            if (token.length() >= 3) {
                for (int i = 0; i <= token.length() - 3; i++) {
                    accumulateFeature(vector, "g:" + token.substring(i, i + 3), 0.75f);
                }
            }
        }
        normalizeL2(vector);
        return new EmbeddingVector(vector);
    }

    private static String normalize(String input) {
        String decomposed = Normalizer.normalize(input, Normalizer.Form.NFD);
        StringBuilder sb = new StringBuilder(decomposed.length());
        for (int i = 0; i < decomposed.length(); i++) {
            char c = decomposed.charAt(i);
            if (Character.getType(c) != Character.NON_SPACING_MARK) {
                sb.append(Character.toLowerCase(c));
            }
        }
        return sb.toString().toLowerCase(Locale.ROOT);
    }

    private static void accumulateFeature(float[] vector, String feature, float weight) {
        int h1 = fnv1a32(feature, 0x811c9dc5);
        int h2 = fnv1a32(feature, 0x9e3779b9);
        int index = Math.floorMod(h1, vector.length);
        float sign = (h2 & 1) == 0 ? 1.0f : -1.0f;
        vector[index] += sign * weight;
    }

    private static int fnv1a32(String value, int seed) {
        int hash = seed;
        for (int i = 0; i < value.length(); i++) {
            hash ^= value.charAt(i);
            hash *= 0x01000193;
        }
        return hash;
    }

    private static void normalizeL2(float[] vector) {
        double sumSq = 0.0;
        for (float v : vector) {
            sumSq += (double) v * v;
        }
        if (sumSq == 0.0) {
            vector[0] = 1.0f;
            return;
        }
        float norm = (float) Math.sqrt(sumSq);
        for (int i = 0; i < vector.length; i++) {
            vector[i] /= norm;
        }
    }
}
