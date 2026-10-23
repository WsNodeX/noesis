package noesis.core.llm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import noesis.core.json.Json;

/**
 * A chat completion response returned by an {@link LlmProvider}.
 *
 * <p>Instances are typically built from the JSON body of an HTTP response via
 * {@link #fromJson(java.util.Map)}, or constructed directly in tests and
 * offline demos through the {@link #builder()} factory.</p>
 */
public final class ChatResponse {
    private final List<ChatMessage> choices;
    private final Usage usage;

    private ChatResponse(Builder b) {
        this.choices = b.choices == null ? List.of() : List.copyOf(b.choices);
        this.usage = b.usage;
    }

    public List<ChatMessage> choices() {
        return choices;
    }

    /** The first assistant message, or {@code null} when there are no choices. */
    public ChatMessage firstMessage() {
        return choices.isEmpty() ? null : choices.get(0);
    }

    public Usage usage() {
        return usage;
    }

    @SuppressWarnings("unchecked")
    public static ChatResponse fromJson(Map<String, Object> root) {
        List<Object> choicesJson = Json.getArray(root, "choices");
        List<ChatMessage> choices = new ArrayList<>();
        if (choicesJson != null) {
            for (Object c : choicesJson) {
                Map<String, Object> choice = (Map<String, Object>) c;
                Map<String, Object> msg = Json.getObject(choice, "message");
                choices.add(messageFromJson(msg));
            }
        }
        Usage usage = null;
        Map<String, Object> u = Json.getObject(root, "usage");
        if (u != null) {
            usage = new Usage(
                    (int) Json.getDouble(u, "prompt_tokens"),
                    (int) Json.getDouble(u, "completion_tokens"),
                    (int) Json.getDouble(u, "total_tokens"));
        }
        return builder().choices(choices).usage(usage).build();
    }

    @SuppressWarnings("unchecked")
    private static ChatMessage messageFromJson(Map<String, Object> msg) {
        Role role = Role.fromWire(Json.getString(msg, "role"));
        String content = Json.getString(msg, "content");
        List<ToolCall> toolCalls = null;
        List<Object> tcJson = Json.getArray(msg, "tool_calls");
        if (tcJson != null) {
            toolCalls = new ArrayList<>();
            for (Object o : tcJson) {
                Map<String, Object> tc = (Map<String, Object>) o;
                String id = Json.getString(tc, "id");
                Map<String, Object> fn = Json.getObject(tc, "function");
                String name = Json.getString(fn, "name");
                String args = Json.getString(fn, "arguments");
                toolCalls.add(new ToolCall(id, name, args));
            }
        }
        return ChatMessage.builder(role)
                .content(content)
                .toolCalls(toolCalls)
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private List<ChatMessage> choices;
        private Usage usage;

        public Builder choices(List<ChatMessage> choices) {
            this.choices = choices;
            return this;
        }

        public Builder usage(Usage usage) {
            this.usage = usage;
            return this;
        }

        public ChatResponse build() {
            return new ChatResponse(this);
        }
    }

    /** Token accounting for a single completion. */
    public record Usage(int promptTokens, int completionTokens, int totalTokens) {}
}
