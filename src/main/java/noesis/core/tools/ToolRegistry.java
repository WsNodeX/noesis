package noesis.core.tools;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A named collection of {@link ToolSpec} instances.
 *
 * <p>The agent asks the provider to honour every function definition returned
 * by {@link #functionDefs()}; when the model answers with tool calls, the
 * executor resolves each call name back to a spec held here.</p>
 */
public final class ToolRegistry {
    private final Map<String, ToolSpec> specs = new LinkedHashMap<>();

    public ToolRegistry register(ToolSpec spec) {
        specs.put(spec.name(), spec);
        return this;
    }

    public ToolSpec get(String name) {
        return specs.get(name);
    }

    public boolean contains(String name) {
        return specs.containsKey(name);
    }

    public int size() {
        return specs.size();
    }

    /** The wire-format definitions for every registered tool. */
    public List<Object> functionDefs() {
        List<Object> defs = new ArrayList<>();
        for (ToolSpec spec : specs.values()) {
            defs.add(spec.toFunctionDef());
        }
        return defs;
    }
}
