package io.github.jvpgjava.creatingj.knowledge.domain;

import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TextChunker {

    public static final int DEFAULT_CHUNK_SIZE = 800;
    public static final int DEFAULT_OVERLAP = 120;
    public static final int MAX_CHUNKS_PER_SOURCE = 100;

    private static final Pattern HEADING_LINE = Pattern.compile("(?m)^(#{1,3})\\s+(.+)$");

    private static final Pattern CODE_TYPE_DECL = Pattern.compile(
            "(?m)^\\s*(?:(?:public|private|protected|internal|abstract|final|sealed|export|default|open|data)\\s+)*"
                    + "(?:class|interface|record|enum|struct|trait|impl|module|namespace|protocol)\\s+"
                    + "([A-Za-z_][A-Za-z0-9_<>,\\.]*)"
    );

    private static final Pattern CODE_FUNC_DECL = Pattern.compile(
            "(?m)^\\s*(?:(?:public|private|protected|internal|static|final|async|export|pub|override|virtual)\\s+)*"
                    + "(?:def|fn|func|function|fun)\\s+([A-Za-z_][A-Za-z0-9_]*)"
                    + "|^\\s*(?:public|protected|private)\\s+(?:static\\s+)?[A-Za-z0-9_<>,\\[\\]\\?]+\\s+"
                    + "([A-Za-z_][A-Za-z0-9_]*)\\s*\\("
    );

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
        String[] activeHeadings = new String[3];
        String[] activeCodeContext = new String[2];

        while (start < length) {
            String prefix = buildContextPrefix(activeHeadings, activeCodeContext, chunkSize);
            int contentBudget = Math.max(1, chunkSize - prefix.length());
            int rawSlicePreviewEnd = Math.min(length, start + contentBudget);
            boolean startsWithHeading = normalizedContent.substring(start, rawSlicePreviewEnd)
                    .stripLeading().startsWith("#");
            if (startsWithHeading) {
                prefix = "";
                contentBudget = chunkSize;
            }

            int end = Math.min(length, start + contentBudget);
            if (end < length) {
                int breakPoint = findBoundary(normalizedContent, start, end);
                if (breakPoint > start + contentBudget / 3) {
                    end = breakPoint;
                }
            }
            String rawPiece = normalizedContent.substring(start, end);
            updateContextMarkers(rawPiece, activeHeadings, activeCodeContext);
            String piece = rawPiece.trim();
            if (!piece.isEmpty()) {
                String formatted = (!prefix.isEmpty() && !piece.startsWith("#")
                        && prefix.length() + piece.length() <= chunkSize)
                        ? prefix + piece
                        : piece;
                chunks.add(formatted);
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
            int effectiveOverlap = Math.min(overlap, Math.max(0, (end - start) - 1));
            int nextStart = end - effectiveOverlap;
            start = nextStart <= start ? end : nextStart;
        }
        return List.copyOf(chunks);
    }

    private static void updateContextMarkers(
            String textSlice,
            String[] activeHeadings,
            String[] activeCodeContext
    ) {
        Matcher headingMatcher = HEADING_LINE.matcher(textSlice);
        while (headingMatcher.find()) {
            int level = headingMatcher.group(1).length();
            String title = headingMatcher.group(2).trim();
            if (level >= 1 && level <= 3 && !title.isEmpty()) {
                activeHeadings[level - 1] = title;
                for (int i = level; i < 3; i++) {
                    activeHeadings[i] = null;
                }
            }
        }
        Matcher typeMatcher = CODE_TYPE_DECL.matcher(textSlice);
        while (typeMatcher.find()) {
            activeCodeContext[0] = typeMatcher.group(1);
            activeCodeContext[1] = null;
        }
        Matcher funcMatcher = CODE_FUNC_DECL.matcher(textSlice);
        while (funcMatcher.find()) {
            String fn = funcMatcher.group(1) != null ? funcMatcher.group(1) : funcMatcher.group(2);
            if (fn != null && !List.of("if", "for", "while", "switch", "catch").contains(fn)) {
                activeCodeContext[1] = fn;
            }
        }
    }

    private static String buildContextPrefix(
            String[] activeHeadings,
            String[] activeCodeContext,
            int chunkSize
    ) {
        if (chunkSize < 160) {
            return "";
        }
        List<String> headingParts = new ArrayList<>(3);
        for (String h : activeHeadings) {
            if (h != null && !h.isBlank()) {
                headingParts.add(h);
            }
        }
        if (!headingParts.isEmpty()) {
            String prefix = "[Secao: " + String.join(" > ", headingParts) + "]\n";
            return prefix.length() <= chunkSize / 4 ? prefix : "";
        }
        List<String> codeParts = new ArrayList<>(2);
        for (String c : activeCodeContext) {
            if (c != null && !c.isBlank()) {
                codeParts.add(c);
            }
        }
        if (!codeParts.isEmpty()) {
            String prefix = "[Codigo: " + String.join(" > ", codeParts) + "]\n";
            return prefix.length() <= chunkSize / 4 ? prefix : "";
        }
        return "";
    }

    private static int findBoundary(String text, int start, int end) {
        int doubleNewline = text.lastIndexOf("\n\n", end - 1);
        if (doubleNewline > start + (end - start) / 2) {
            return doubleNewline + 2;
        }
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
