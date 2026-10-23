package noesis.core.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import noesis.core.json.Json;
import noesis.core.llm.ChatMessage;
import noesis.core.llm.ChatResponse;
import noesis.core.llm.Role;
import noesis.core.llm.ToolCall;
import noesis.core.tools.Tool;
import noesis.core.tools.ToolRegistry;
import noesis.core.tools.ToolSpec;
import noesis.examples.StubProvider;

import org.junit.jupiter.api.Test;

class AgentTest {

    private ToolRegistry calculator() {
        ToolSpec spec = ToolSpec.of("calc", "multiply two numbers",
                Json.object()
                        .put("type", "object")
                        .put("properties", Json.object()
                                .put("a", Json.object().put("type", "number").build())
                                .put("b", Json.object().put("type", "number").build())
                                .build())
                        .put("required", Json.array().add("a").add("b").build())
                        .build(),
                args -> {
                    double a = ((Number) args.get("a")).doubleValue();
                    double b = ((Number) args.get("b")).doubleValue();
                    return String.valueOf(a * b);
                });
        return new ToolRegistry().register(spec);
    }

    @Test
    void runsToolThenReturnsFinalAnswer() {
        ToolRegistry reg = calculator();
        StubProvider provider = new StubProvider();
        provider.then(toolCall("call_1", "calc", Map.of("a", 6, "b", 7)))
                .fallback(ChatResponse.builder()
                        .choices(List.of(ChatMessage.assistant("The product of 6 and 7 is 42.")))
                        .build());

        Agent agent = new Agent(AgentConfig.builder().model("stub").maxSteps(5).build(),
                provider, reg).system("You are a maths assistant.");

        String answer = agent.run("What is 6 times 7?");
        assertEquals("The product of 6 and 7 is 42.", answer);

        // The transcript must contain the assistant tool request and the result.
        boolean sawToolResult = agent.transcript().stream()
                .anyMatch(m -> m.role() == Role.TOOL && "42.0".equals(m.content()));
        assertTrue(sawToolResult, "tool result should be appended to the transcript");
    }

    @Test
    void answersDirectlyWhenNoToolNeeded() {
        ToolRegistry reg = calculator();
        StubProvider provider = new StubProvider()
                .fallback(ChatResponse.builder()
                        .choices(List.of(ChatMessage.assistant("I can help with that.")))
                        .build());
        Agent agent = new Agent(AgentConfig.builder().model("stub").build(), provider, reg);
        assertEquals("I can help with that.", agent.run("Hello"));
    }

    private static ChatResponse toolCall(String id, String name, Map<String, Object> args) {
        ChatMessage message = ChatMessage.builder(Role.ASSISTANT)
                .toolCalls(List.of(new ToolCall(id, name, Json.write(args))))
                .build();
        return ChatResponse.builder().choices(List.of(message)).build();
    }
}
