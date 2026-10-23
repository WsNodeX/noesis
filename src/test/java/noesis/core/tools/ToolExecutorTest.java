package noesis.core.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import noesis.core.json.Json;
import noesis.core.llm.ChatMessage;
import noesis.core.llm.Role;
import noesis.core.llm.ToolCall;

import org.junit.jupiter.api.Test;

class ToolExecutorTest {

    private ToolRegistry calculator() {
        ToolSpec spec = ToolSpec.of("calc", "compute", Json.object()
                .put("type", "object")
                .put("properties", Json.object()
                        .put("a", Json.object().put("type", "number").build())
                        .put("b", Json.object().put("type", "number").build())
                        .build())
                .put("required", Json.array().add("a").add("b").build())
                .build(), args -> {
            double a = ((Number) args.get("a")).doubleValue();
            double b = ((Number) args.get("b")).doubleValue();
            return String.valueOf(a * b);
        });
        return new ToolRegistry().register(spec);
    }

    @Test
    void executesToolAndReturnsResultMessage() {
        ToolRegistry reg = calculator();
        ToolCall call = new ToolCall("c1", "calc", "{\"a\":6,\"b\":7}");
        List<ChatMessage> results = ToolExecutor.execute(List.of(call), reg);
        assertEquals(1, results.size());
        ChatMessage msg = results.get(0);
        assertEquals(Role.TOOL, msg.role());
        assertEquals("c1", msg.toolCallId());
        assertEquals("42.0", msg.content());
    }

    @Test
    void unknownToolProducesErrorMessage() {
        ToolRegistry reg = calculator();
        ToolCall call = new ToolCall("c2", "missing", "{}");
        List<ChatMessage> results = ToolExecutor.execute(List.of(call), reg);
        assertTrue(results.get(0).content().startsWith("ERROR:"));
    }

    @Test
    void malformedArgumentsDoNotCrash() {
        ToolRegistry reg = calculator();
        ToolCall call = new ToolCall("c3", "calc", "not-json");
        List<ChatMessage> results = ToolExecutor.execute(List.of(call), reg);
        assertTrue(results.get(0).content().startsWith("ERROR:") || results.get(0).content() != null);
    }

    @Test
    void registryProducesFunctionDefinitions() {
        ToolRegistry reg = calculator();
        assertEquals(1, reg.functionDefs().size());
        @SuppressWarnings("unchecked")
        Map<String, Object> def = (Map<String, Object>) reg.functionDefs().get(0);
        assertEquals("function", def.get("type"));
    }
}
