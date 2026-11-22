package io.github.jvpgjava.creatingj.common.domain;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class StreamingBlockOutputGuard {

    public static final int MIN_BLOCK_CHARS = 64;
    public static final int MAX_LINE_BLOCK_CHARS = 220;

    private static final Pattern SENTENCE_END = Pattern.compile(
            "(?<!\\b[A-Z])(?<!\\bProf)(?<!\\bDr)(?<!\\bSr)(?<!\\bSra)(?<!\\bart)(?<!\\bv1)(?<!\\betc)[.!?](?=\\s)"
    );

    private static final Pattern SUSPICIOUS_HOLD_TRIGGER = Pattern.compile(
            "(?i)(?:\\[|-----BEGIN|\\bAKIA|\\b(?:ghp|gho|ghu|ghs|ghr)_|\\bgithub_pat_|\\bsk-|"
                    + "\\b(?:sk|stac|crj)_|CRJ_API_KEY|STAC_API_KEY|AWS_SECRET_ACCESS_KEY|CREATINGJ_DB_PASSWORD|"
                    + "REGRAS\\s+INTERNAS|MODO\\s+ESTRITO|\\bDAN\\b|modo\\s+desenvolvedor|developer\\s+mode|"
                    + "rm\\s+-rf|:\\(\\)\\{|mkfs\\.)"
    );

    private static final Pattern PARTIAL_TRIGGER_SUFFIX = Pattern.compile(
            "(?i)(?:\\[|\\b(?:sk|crj|stac|ghp|gho|ghu|ghs|ghr|AKIA|DAN|rm|REGRAS|MODO)|-----)$"
    );

    private final StringBuilder buffer = new StringBuilder();
    private final StringBuilder releasedText = new StringBuilder();
    private final Set<String> flags = new LinkedHashSet<>();
    private boolean replacedByRefusal;

    public List<String> feed(String piece) {
        if (replacedByRefusal || piece == null || piece.isEmpty()) {
            return List.of();
        }
        buffer.append(piece);
        String current = buffer.toString();

        if (!requiresHold(current)) {
            buffer.setLength(0);
            String out = sanitizeBlock(current);
            return out.isEmpty() ? List.of() : List.of(out);
        }

        List<String> readyBlocks = extractReadyBlocks();
        List<String> emitted = new ArrayList<>(readyBlocks.size());
        for (String rawBlock : readyBlocks) {
            String out = sanitizeBlock(rawBlock);
            if (!out.isEmpty()) {
                emitted.add(out);
            }
            if (replacedByRefusal) {
                buffer.setLength(0);
                break;
            }
        }
        return List.copyOf(emitted);
    }

    public List<String> finish() {
        if (replacedByRefusal) {
            buffer.setLength(0);
            return List.of();
        }
        String remaining = buffer.toString();
        buffer.setLength(0);
        if (remaining.isBlank()) {
            if (releasedText.isEmpty()) {
                String fallback = "(sem resposta textual)";
                releasedText.append(fallback);
                return List.of(fallback);
            }
            return List.of();
        }
        String out = sanitizeBlock(remaining);
        return out.isEmpty() ? List.of() : List.of(out);
    }

    public String releasedContent() {
        String content = releasedText.toString();
        return content.isBlank() ? "(sem resposta textual)" : content;
    }

    public List<String> flags() {
        return List.copyOf(flags);
    }

    public boolean replacedByRefusal() {
        return replacedByRefusal;
    }

    private static boolean requiresHold(String text) {
        String trimmed = text.stripTrailing();
        int lastOpenBracket = text.lastIndexOf('[');
        boolean unclosedBracket = lastOpenBracket >= 0 && text.indexOf(']', lastOpenBracket) < 0;
        return unclosedBracket
                || PARTIAL_TRIGGER_SUFFIX.matcher(trimmed).find()
                || SUSPICIOUS_HOLD_TRIGGER.matcher(text).find();
    }

    private String sanitizeBlock(String rawBlock) {
        String trailingWhitespace = rawBlock.substring(rawBlock.stripTrailing().length());
        OutputSecurityGuard.SanitizedOutput sanitized = OutputSecurityGuard.inspectAndSanitize(rawBlock);
        flags.addAll(sanitized.flags());
        if (sanitized.replaced()) {
            replacedByRefusal = true;
            releasedText.setLength(0);
            releasedText.append(PromptSecurityGuard.STANDARD_REFUSAL_MESSAGE);
            return PromptSecurityGuard.STANDARD_REFUSAL_MESSAGE;
        }
        String content = sanitized.content();
        if ("(sem resposta textual)".equals(content)) {
            return "";
        }
        if (!trailingWhitespace.isEmpty() && !content.endsWith(trailingWhitespace)) {
            content = content.stripTrailing() + trailingWhitespace;
        }
        releasedText.append(content);
        return content;
    }

    private List<String> extractReadyBlocks() {
        List<String> blocks = new ArrayList<>();
        while (true) {
            Integer cut = findCut(buffer.toString());
            if (cut == null) {
                return blocks;
            }
            blocks.add(buffer.substring(0, cut));
            buffer.delete(0, cut);
        }
    }

    private static Integer findCut(String current) {
        int paragraph = current.indexOf("\n\n");
        if (paragraph > 0) {
            return paragraph + 2;
        }
        if (current.length() < MIN_BLOCK_CHARS) {
            return null;
        }
        Matcher matcher = SENTENCE_END.matcher(current);
        if (matcher.find(MIN_BLOCK_CHARS - 1) && matcher.end() < current.length()) {
            return matcher.end() + 1;
        }
        if (current.length() >= MAX_LINE_BLOCK_CHARS) {
            int newline = current.lastIndexOf('\n');
            if (newline >= MIN_BLOCK_CHARS && newline < current.length() - 1) {
                return newline + 1;
            }
        }
        return null;
    }
}
