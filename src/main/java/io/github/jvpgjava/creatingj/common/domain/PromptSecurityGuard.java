package io.github.jvpgjava.creatingj.common.domain;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PromptSecurityGuard {

    public static final String STANDARD_REFUSAL_MESSAGE =
            "Nao posso atender a essa solicitacao por violar as diretrizes de seguranca da plataforma CreatingJ. "
                    + "Posso ajudar com desenvolvimento de software, arquitetura, testes e engenharia segura.";

    private static final Pattern COMBINING_MARKS = Pattern.compile("\\p{M}+");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern INVISIBLE_CHARS = Pattern.compile(
            "[\\u200B-\\u200F\\u202A-\\u202E\\u2060-\\u2064\\uFEFF]"
    );
    private static final Pattern ROLE_SPOOFING = Pattern.compile(
            "(?:^|[.!?\\n])\\s*(?:system|assistant|developer|admin|sistema)\\s*:\\s*"
                    + "(?:modo|ignore|rules|regras|voce|you|restrictions|restricoes|ativad)",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
    );
    private static final Pattern DELIMITER_INJECTION = Pattern.compile(
            "</?\\s*(?:system|instructions?|prompt|guardrails?)\\s*>|\\[/?(?:inst|system)\\]|<\\|im_(?:start|end)\\|>",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern SPACED_LETTERS = Pattern.compile("(?:\\b[a-zA-Z]\\b[ .\\-_]){12,}");
    private static final Pattern BASE64_RUN = Pattern.compile("[A-Za-z0-9+/_-]{32,}={0,2}");
    private static final Pattern BINARY_RUN = Pattern.compile("(?:\\b[01]{8}\\b\\s*){6,}");
    private static final Pattern HEX_RUN = Pattern.compile(
            "(?:\\b[0-9a-fA-F]{2}\\b[\\s,]*){12,}|\\b0x[0-9a-fA-F]{2}(?:[\\s,]+0x[0-9a-fA-F]{2}){7,}"
    );
    private static final Pattern DECODE_AND_OBEY = Pattern.compile(
            "\\b(?:decodif\\w*|decode|invert\\w*|reverse|desembaralh\\w*)\\b[^.!?\\n]{0,50}"
                    + "\\b(?:obedec\\w*|siga|execut\\w*|faca|follow|obey|execute)\\b"
    );

    private static final Pattern EXTRACTION_PATTERN = Pattern.compile(
            "\\b(?:mostr\\w*|revel\\w*|repit\\w*|exib\\w*|imprim\\w*|traduz\\w*|codifiq\\w*|"
                    + "reveal|show|print|repeat|output|translate|encode|dump)\\b(?:\\s+\\w+){0,5}?\\s+"
                    + "(?:(?:suas?|seus?|tuas?|your|the|o|seu)(?:\\s+\\w+){0,2}?\\s+"
                    + "(?:instruc\\w*|regras|rules?|prompt|diretrizes|guidelines|system prompt|system message)"
                    + "|system prompt\\b|prompt (?:do|de) sistema\\b|prompt inicial\\b)"
                    + "|\\b(?:quais sao|what are)\\b(?:\\s+\\w+){0,3}?\\s+"
                    + "(?:suas? (?:instruc\\w*|regras) (?:internas?|ocultas?|secretas?|iniciais)"
                    + "|your system prompt)\\b"
    );

    private static final Pattern INJECTION_PATTERN = Pattern.compile(
            "\\b(?:ignor\\w*|esquec\\w*|desconsider\\w*|descart\\w*|desobedec\\w*)\\b"
                    + "(?:\\s+\\w+){0,4}?\\s+(?:instruc\\w*|regras?|ordens|diretrizes|restric\\w*|prompt|guardrails?)"
                    + "|\\b(?:ignore|disregard|forget|bypass|override)\\b(?:\\s+\\w+){0,4}?\\s+"
                    + "(?:instructions?|rules?|prompt|guidelines|directives|safety|guardrails?)"
                    + "|\\b(?:do anything now|dan mode|atue como dan|voce agora e (?:a )?dan|jailbreak\\w*|"
                    + "developer mode|god mode|sudo mode|modo (?:desenvolvedor|dev|deus|sem filtros?|irrestrito))\\b"
                    + "|\\b(?:responda|aja|atue|fale|funcione) sem (?:nenhuma |qualquer )?"
                    + "(?:regras?|restricoes|filtros?|censura|limites?)\\b"
                    + "|\\b(?:finj[ae]|faca de conta|aja como se|pretend)\\b[^.!?\\n]{0,40}"
                    + "\\b(?:nao (?:tem|exist\\w*|houvesse) regras|sem restric\\w*|unrestricted|no rules)\\b"
                    + "|\\b(?:confirme|confirm|diga|say)\\b[^.!?\\n]{0,30}"
                    + "\\b(?:dan ativad[ao]|modo desenvolvedor ativado|developer mode enabled)\\b"
    );

    private static final Pattern MALICIOUS_CODE_PATTERN = Pattern.compile(
            "\\b(?:escrev\\w*|crie\\w*|ger\\w*|fac\\w*|desenvolv\\w*|monte\\w*|implement\\w*|"
                    + "write|create|build|generate|code)\\b[^.!?\\n]{0,70}\\b(?:keylogger|ransomware|trojan|rootkit|"
                    + "spyware|credential stealer|cookie stealer|token stealer|botnet|reverse shell|"
                    + "script de ddos|ataque ddos|exploit para invadir|fork bomb)\\b"
                    + "|\\b(?:keylogger|ransomware|credential stealer|cookie stealer)\\b[^.!?\\n]{0,60}"
                    + "\\b(?:completo|funcional|furtivo|escondido|hidden|stealth|em python|em java|em rust|em c)\\b"
    );

    private static final List<String> DECODE_HINTS = List.of("decod", "decode", "base64", "hex", "binario");

    private PromptSecurityGuard() {
    }

    public static GuardVerdict inspectInput(String rawText) {
        return inspectInternal(rawText, 0);
    }

    private static GuardVerdict inspectInternal(String rawText, int depth) {
        if (rawText == null || rawText.isBlank()) {
            return GuardVerdict.allowed();
        }
        if (countInvisibleChars(rawText) >= 3) {
            return GuardVerdict.blocked(
                    GuardCategory.OBFUSCATED_PAYLOAD,
                    "Caracteres invisiveis ou de controle de direcao detectados na entrada."
            );
        }
        if (DELIMITER_INJECTION.matcher(rawText).find() || ROLE_SPOOFING.matcher(rawText).find()) {
            return GuardVerdict.blocked(
                    GuardCategory.PROMPT_INJECTION,
                    "Tentativa de injecao de delimitadores ou rotulos de sistema detectada."
            );
        }

        String normalized = normalize(rawText);
        String target = normalized + "\n" + normalizeLeetspeak(normalized);

        if (EXTRACTION_PATTERN.matcher(target).find()) {
            return GuardVerdict.blocked(
                    GuardCategory.PROMPT_EXTRACTION,
                    "Tentativa de extracao de instrucoes internas ou system prompt bloqueada."
            );
        }
        if (INJECTION_PATTERN.matcher(target).find()) {
            return GuardVerdict.blocked(
                    GuardCategory.PROMPT_INJECTION,
                    "Tentativa de manipulacao de regras ou jailbreak detectada."
            );
        }
        if (MALICIOUS_CODE_PATTERN.matcher(target).find()) {
            return GuardVerdict.blocked(
                    GuardCategory.MALICIOUS_CODE_REQUEST,
                    "Solicitacao de geracao de malware ou codigo ofensivo bloqueada."
            );
        }
        if (DECODE_AND_OBEY.matcher(normalized).find() || SPACED_LETTERS.matcher(rawText).find()) {
            return GuardVerdict.blocked(
                    GuardCategory.OBFUSCATED_PAYLOAD,
                    "Tentativa de execucao de instrucoes ofuscadas ou invertidas bloqueada."
            );
        }
        return depth == 0 ? inspectEncodedPayloads(rawText, normalized) : GuardVerdict.allowed();
    }

    private static GuardVerdict inspectEncodedPayloads(String rawText, String normalizedOuter) {
        boolean asksToDecode = DECODE_HINTS.stream().anyMatch(normalizedOuter::contains);

        Matcher b64Matcher = BASE64_RUN.matcher(rawText);
        while (b64Matcher.find()) {
            String decoded = tryDecodeBase64Text(b64Matcher.group());
            if (isDecodedThreat(decoded, asksToDecode)) {
                return GuardVerdict.blocked(
                        GuardCategory.OBFUSCATED_PAYLOAD,
                        "Conteudo codificado em Base64 com instrucoes bloqueadas detectado."
                );
            }
        }

        Matcher binMatcher = BINARY_RUN.matcher(rawText);
        if (binMatcher.find() && isDecodedThreat(tryDecodeBinaryText(binMatcher.group()), asksToDecode)) {
            return GuardVerdict.blocked(
                    GuardCategory.OBFUSCATED_PAYLOAD,
                    "Payload binario ofuscado detectado."
            );
        }

        Matcher hexMatcher = HEX_RUN.matcher(rawText);
        if (hexMatcher.find() && isDecodedThreat(tryDecodeHexText(hexMatcher.group()), asksToDecode)) {
            return GuardVerdict.blocked(
                    GuardCategory.OBFUSCATED_PAYLOAD,
                    "Payload hexadecimal ofuscado detectado."
            );
        }
        return GuardVerdict.allowed();
    }

    private static boolean isDecodedThreat(String decoded, boolean asksToDecode) {
        return decoded != null && (asksToDecode || inspectInternal(decoded, 1).blocked());
    }

    public static String normalize(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String decomposed = Normalizer.normalize(text.toLowerCase(Locale.ROOT), Normalizer.Form.NFKD);
        String withoutAccents = COMBINING_MARKS.matcher(decomposed).replaceAll("");
        return WHITESPACE.matcher(withoutAccents).replaceAll(" ").trim();
    }

    private static String normalizeLeetspeak(String normalized) {
        return normalized
                .replace('0', 'o')
                .replace('1', 'i')
                .replace('3', 'e')
                .replace('4', 'a')
                .replace('5', 's')
                .replace('7', 't');
    }

    private static int countInvisibleChars(String text) {
        int count = 0;
        Matcher matcher = INVISIBLE_CHARS.matcher(text);
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private static String tryDecodeBase64Text(String candidate) {
        int pad = (4 - (candidate.length() % 4)) % 4;
        String padded = candidate.replace('-', '+').replace('_', '/') + "=".repeat(pad);
        try {
            byte[] bytes = Base64.getDecoder().decode(padded);
            String text = new String(bytes, StandardCharsets.UTF_8);
            return isMostlyPrintableText(text) ? text : null;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static String tryDecodeBinaryText(String binaryRun) {
        String[] tokens = binaryRun.trim().split("\\s+");
        byte[] bytes = new byte[tokens.length];
        for (int i = 0; i < tokens.length; i++) {
            bytes[i] = (byte) Integer.parseInt(tokens[i], 2);
        }
        String text = new String(bytes, StandardCharsets.UTF_8);
        return isMostlyPrintableText(text) ? text : null;
    }

    private static String tryDecodeHexText(String hexRun) {
        String cleaned = hexRun.replaceAll("(?i)0x|[\\s,]+", "");
        byte[] bytes = new byte[cleaned.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            int idx = i * 2;
            bytes[i] = (byte) Integer.parseInt(cleaned.substring(idx, idx + 2), 16);
        }
        String text = new String(bytes, StandardCharsets.UTF_8);
        return isMostlyPrintableText(text) ? text : null;
    }

    private static boolean isMostlyPrintableText(String text) {
        int lettersOrSpaces = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c < 32 && c != '\n' && c != '\t') {
                return false;
            }
            if (Character.isLetter(c) || c == ' ') {
                lettersOrSpaces++;
            }
        }
        return text.length() >= 12 && ((double) lettersOrSpaces / text.length()) >= 0.65;
    }

    public enum GuardCategory {
        NONE,
        PROMPT_INJECTION,
        PROMPT_EXTRACTION,
        OBFUSCATED_PAYLOAD,
        MALICIOUS_CODE_REQUEST
    }

    public record GuardVerdict(
            boolean blocked,
            GuardCategory category,
            String reason,
            String refusalMessage
    ) {

        public static GuardVerdict allowed() {
            return new GuardVerdict(false, GuardCategory.NONE, null, null);
        }

        public static GuardVerdict blocked(GuardCategory category, String reason) {
            return new GuardVerdict(true, category, reason, STANDARD_REFUSAL_MESSAGE);
        }
    }
}
