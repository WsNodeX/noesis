package noesis.core.llm;

import java.util.ArrayList;
import java.util.List;

import noesis.core.json.Json;
import noesis.core.json.Json.ObjectBuilder;

/**
 * A chat completion request.
 *
 * <p>Build requests fluently and include the function definitions produced by a
 * {@link noesis.core.tools.ToolRegistry} so the model can decide when to call
 * them.</p>
 */
public final class ChatRequest {
    private final String model;
    private final List<ChatMessage> messages;
    private final List<Object> tools;
    private final Double temperature;
    private final Boolean stream;

    private ChatRequest(Builder b) {
        this.model = b.model;
        this.messages = List.copyOf(b.messages);
        this.tools = b.tools == null ? null : List.copyOf(b.tools);
        this.temperature = b.temperature;
        this.stream = b.stream;
    }

    public String model() {
        return model;
    }

    public List<ChatMessage> messages() {
        return messages;
    }

    /** Serialise the request to the endpoint wire format. */
    public ObjectBuilder toWire() {
        ObjectBuilder b = Json.object().put("model", model);
        List<Object> wireMessages = new ArrayList<>();
        for (ChatMessage m : messages) wireMessages.add(m.toWire().build());
        b.put("messages", wireMessages);
        if (tools != null && !tools.isEmpty()) {
            b.put("tools", tools);
            b.put("tool_choice", "auto");
        }
        if (temperature != null) b.put("temperature", temperature);
        if (stream != null) b.put("stream", stream);
        return b;
    }

    public static Builder builder(String model) {
        return new Builder(model);
    }

    public static final class Builder {
        private String model;
        private final List<ChatMessage> messages = new ArrayList<>();
        private List<Object> tools;
        private Double temperature;
        private Boolean stream;

        Builder(String model) {
            this.model = model;
        }

        public Builder message(ChatMessage m) {
            messages.add(m);
            return this;
        }

        public Builder messages(List<ChatMessage> ms) {
            messages.addAll(ms);
            return this;
        }

        public Builder tools(List<Object> functionDefs) {
            this.tools = functionDefs;
            return this;
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder temperature(double t) {
            this.temperature = t;
            return this;
        }

        public Builder stream(boolean s) {
            this.stream = s;
            return this;
        }

        public ChatRequest build() {
            return new ChatRequest(this);
        }
    }

    /** Copy this request into a new builder for further tweaking. */
    public Builder toBuilder() {
        Builder b = new Builder(model);
        b.messages.addAll(messages);
        b.tools = tools;
        b.temperature = temperature;
        b.stream = stream;
        return b;
    }
}
