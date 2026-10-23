package noesis.core.llm;

import java.util.ArrayList;
import java.util.List;

import noesis.core.json.Json;
import noesis.core.json.Json.ObjectBuilder;

/**
 * A single message in a chat conversation.
 *
 * <p>A message can be a plain user/assistant/system turn, or a special
 * {@link Role#TOOL} message that carries the result of a tool invocation back
 * to the model. When the assistant decides to call tools, the message holds a
 * non-empty {@link #toolCalls()} list and may have a {@code null} content.</p>
 */
public final class ChatMessage {
    private final Role role;
    private final String content;
    private final String name;
    private final String toolCallId;
    private final List<ToolCall> toolCalls;

    private ChatMessage(Builder b) {
        this.role = b.role;
        this.content = b.content;
        this.name = b.name;
        this.toolCallId = b.toolCallId;
        this.toolCalls = b.toolCalls == null ? null : List.copyOf(b.toolCalls);
    }

    public Role role() {
        return role;
    }

    public String content() {
        return content;
    }

    public String name() {
        return name;
    }

    public String toolCallId() {
        return toolCallId;
    }

    public List<ToolCall> toolCalls() {
        return toolCalls;
    }

    public boolean hasToolCalls() {
        return toolCalls != null && !toolCalls.isEmpty();
    }

    /** Serialise this message to the wire format expected by the endpoint. */
    public ObjectBuilder toWire() {
        ObjectBuilder b = Json.object().put("role", role.wire());
        if (content != null) b.put("content", content);
        if (name != null) b.put("name", name);
        if (toolCallId != null) b.put("tool_call_id", toolCallId);
        if (toolCalls != null && !toolCalls.isEmpty()) {
            List<Object> calls = new ArrayList<>();
            for (ToolCall tc : toolCalls) {
                calls.add(Json.object()
                        .put("id", tc.id())
                        .put("type", "function")
                        .put("function", Json.object()
                                .put("name", tc.name())
                                .put("arguments", tc.arguments())));
            }
            b.put("tool_calls", calls);
        }
        return b;
    }

    // ----------------------------------------------------------- factory API

    public static ChatMessage system(String content) {
        return builder(Role.SYSTEM).content(content).build();
    }

    public static ChatMessage user(String content) {
        return builder(Role.USER).content(content).build();
    }

    public static ChatMessage assistant(String content) {
        return builder(Role.ASSISTANT).content(content).build();
    }

    /** A tool result message that references the originating tool call. */
    public static ChatMessage tool(String toolCallId, String content) {
        return builder(Role.TOOL).toolCallId(toolCallId).content(content).build();
    }

    public static Builder builder(Role role) {
        return new Builder(role);
    }

    public static final class Builder {
        private final Role role;
        private String content;
        private String name;
        private String toolCallId;
        private List<ToolCall> toolCalls;

        Builder(Role role) {
            this.role = role;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder toolCallId(String toolCallId) {
            this.toolCallId = toolCallId;
            return this;
        }

        public Builder toolCalls(List<ToolCall> toolCalls) {
            this.toolCalls = toolCalls;
            return this;
        }

        public ChatMessage build() {
            return new ChatMessage(this);
        }
    }
}
