package io.github.jvpgjava.creatingj.agent.application.port.out;

import io.github.jvpgjava.creatingj.agent.domain.ParsedAgentPlan;

public interface AgentPlanParserPort {

    ParsedAgentPlan parse(String rawModelOutput);
}
