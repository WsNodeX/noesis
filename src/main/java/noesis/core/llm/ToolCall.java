package noesis.core.llm;

/**
 * A request, issued by the model, to invoke one of the registered tools.
 *
 * <p>The {@code arguments} field is a JSON string as returned by the model and
 * must be parsed by the tool executor before the underlying handler runs.</p>
 */
public final class ToolCall {
    private final String id;
    private final String name;
    private final String arguments;

    public ToolCall(String id, String name, String arguments) {
        this.id = id;
        this.name = name;
        this.arguments = arguments == null ? "{}" : arguments;
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String arguments() {
        return arguments;
    }

    @Override
    public String toString() {
        return "ToolCall{id='" + id + "', name='" + name + "'}";
    }
}
