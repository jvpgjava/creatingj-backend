package io.github.jvpgjava.creatingj.agent.adapter.out.parser;

import io.github.jvpgjava.creatingj.agent.application.port.out.AgentPlanParserPort;
import io.github.jvpgjava.creatingj.agent.domain.ParsedAgentPlan;
import io.github.jvpgjava.creatingj.agent.domain.ProposedToolAction;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

public final class JacksonAgentPlanParserAdapter implements AgentPlanParserPort {

    private final ObjectMapper objectMapper;

    public JacksonAgentPlanParserAdapter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public ParsedAgentPlan parse(String rawModelOutput) {
        if (rawModelOutput == null || rawModelOutput.isBlank()) {
            return new ParsedAgentPlan("Nenhuma acao proposta pelo modelo.", List.of());
        }
        String candidate = extractJsonObject(rawModelOutput.trim());
        if (candidate == null) {
            return new ParsedAgentPlan(rawModelOutput.trim(), List.of());
        }
        try {
            JsonNode root = objectMapper.readTree(candidate);
            String summary = root.path("planSummary").asString("").trim();
            if (summary.isEmpty()) {
                summary = "Plano gerado pelo agente.";
            }
            List<ProposedToolAction> proposals = new ArrayList<>();
            JsonNode proposalsNode = root.path("proposals");
            if (proposalsNode.isArray()) {
                for (JsonNode item : proposalsNode) {
                    String tool = item.path("tool").asString("");
                    String targetPath = item.hasNonNull("targetPath") ? item.path("targetPath").asString("") : null;
                    String command = item.hasNonNull("command") ? item.path("command").asString("") : null;
                    String rationale = item.path("rationale").asString("");
                    proposals.add(new ProposedToolAction(tool, targetPath, command, rationale));
                }
            }
            return new ParsedAgentPlan(summary, proposals);
        } catch (RuntimeException ex) {
            return new ParsedAgentPlan(rawModelOutput.trim(), List.of());
        }
    }

    private static String extractJsonObject(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return null;
        }
        return text.substring(start, end + 1);
    }
}
