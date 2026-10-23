package noesis.core.tools;

import java.util.Map;

import noesis.core.json.Json;

/**
 * The declarative description of a {@link Tool}: its name, a human-readable
 * description, the JSON-schema of its parameters, and the handler itself.
 *
 * <p>The {@link #toFunctionDef()} method produces the exact object shape an
 * OpenAI-compatible endpoint expects inside a {@code tools} array.</p>
 */
public final class ToolSpec {
    private final String name;
    private final String description;
    private final Map<String, Object> parameters;
    private final Tool handler;

    public ToolSpec(String name, String description, Map<String, Object> parameters, Tool handler) {
        this.name = name;
        this.description = description;
        this.parameters = parameters == null ? Map.of() : parameters;
        this.handler = handler;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public Map<String, Object> parameters() {
        return parameters;
    }

    public Tool handler() {
        return handler;
    }

    /** Build the wire-format entry for this tool. */
    public Map<String, Object> toFunctionDef() {
        return Json.object()
                .put("type", "function")
                .put("function", Json.object()
                        .put("name", name)
                        .put("description", description)
                        .put("parameters", parameters))
                .build();
    }

    /** Convenience factory for a tool with no parameters. */
    public static ToolSpec of(String name, String description, Tool handler) {
        return new ToolSpec(name, description, Json.object().put("type", "object").put("properties", Json.object().build()).build(), handler);
    }

    public static ToolSpec of(String name, String description, Map<String, Object> parameters, Tool handler) {
        return new ToolSpec(name, description, parameters, handler);
    }
}
