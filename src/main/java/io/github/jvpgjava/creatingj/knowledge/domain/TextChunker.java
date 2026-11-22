package io.github.jvpgjava.creatingj.knowledge.domain;

import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;

import java.util.ArrayList;
import java.util.List;

public final class TextChunker {

    public static final int DEFAULT_CHUNK_SIZE = 800;
    public static final int DEFAULT_OVERLAP = 120;
    public static final int MAX_CHUNKS_PER_SOURCE = 100;

    private TextChunker() {
    }

    public static List<String> split(String normalizedContent) {
        return split(normalizedContent, DEFAULT_CHUNK_SIZE, DEFAULT_OVERLAP);
    }

    public static List<String> split(String normalizedContent, int chunkSize, int overlap) {
        if (normalizedContent == null || normalizedContent.isBlank()) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Conteudo para chunking obrigatorio.");
        }
        if (chunkSize <= 0 || overlap < 0 || overlap >= chunkSize) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Parametros de chunking invalidos.");
        }
        List<String> chunks = new ArrayList<>();
        int length = normalizedContent.length();
        int start = 0;
        while (start < length) {
            int end = Math.min(length, start + chunkSize);
            if (end < length) {
                int breakPoint = findBoundary(normalizedContent, start, end);
                if (breakPoint > start + chunkSize / 3) {
                    end = breakPoint;
                }
            }
            String piece = normalizedContent.substring(start, end).trim();
            if (!piece.isEmpty()) {
                chunks.add(piece);
                if (chunks.size() > MAX_CHUNKS_PER_SOURCE) {
                    throw new DomainException(
                            ErrorCode.VALIDATION_ERROR,
                            "Fonte excede o limite maximo de " + MAX_CHUNKS_PER_SOURCE + " chunks."
                    );
                }
            }
            if (end >= length) {
                break;
            }
            int nextStart = end - overlap;
            start = nextStart <= start ? end : nextStart;
        }
        return List.copyOf(chunks);
    }

    private static int findBoundary(String text, int start, int end) {
        int newline = text.lastIndexOf('\n', end - 1);
        if (newline > start) {
            return newline + 1;
        }
        int space = text.lastIndexOf(' ', end - 1);
        if (space > start) {
            return space + 1;
        }
        return end;
    }
}
