package noesis.core.llm;

/** The speaker of a chat message in a conversation with an LLM. */
public enum Role {
    SYSTEM("system"),
    USER("user"),
    ASSISTANT("assistant"),
    TOOL("tool");

    private final String wire;

    Role(String wire) {
        this.wire = wire;
    }

    /** The value sent over the wire to an OpenAI-compatible endpoint. */
    public String wire() {
        return wire;
    }

    public static Role fromWire(String wire) {
        for (Role r : values()) {
            if (r.wire.equals(wire)) return r;
        }
        throw new IllegalArgumentException("Unknown role: " + wire);
    }
}
