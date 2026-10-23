package noesis.core.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import noesis.core.json.Json;
import noesis.core.llm.ChatMessage;
import noesis.core.llm.ToolCall;

/**
 * Runs the tool calls requested by a model and packages their results as
 * {@code tool} messages ready to be sent back to the provider.
 *
 * <p>Every invocation is isolated: if a tool throws or its arguments cannot be
 * parsed, the failure becomes a textual error result (prefixed with
 * {@code ERROR:}) instead of aborting the agent loop, giving the model a chance
 * to recover or explain.</p>
 */
public final class ToolExecutor {

    private ToolExecutor() {}

    public static List<ChatMessage> execute(List<ToolCall> calls, ToolRegistry registry) {
        List<ChatMessage> results = new ArrayList<>();
        if (calls == null) return results;
        for (ToolCall call : calls) {
            results.add(executeOne(call, registry));
        }
        return results;
    }

    private static ChatMessage executeOne(ToolCall call, ToolRegistry registry) {
        ToolSpec spec = registry.get(call.name());
        if (spec == null) {
            return ChatMessage.tool(call.id(), "ERROR: unknown tool '" + call.name() + "'");
        }
        try {
            Map<String, Object> args = parseArguments(call.arguments());
            String result = spec.handler().run(args);
            return ChatMessage.tool(call.id(), result == null ? "" : result);
        } catch (Exception e) {
            return ChatMessage.tool(call.id(), "ERROR: " + e.getMessage());
        }
    }

    private static Map<String, Object> parseArguments(String arguments) {
        if (arguments == null || arguments.isBlank()) {
            return Map.of();
        }
        try {
            return Json.parseObject(arguments);
        } catch (RuntimeException e) {
            // Some models occasionally emit malformed argument blobs; tolerate.
            return Map.of("_raw", arguments);
        }
    }
}
