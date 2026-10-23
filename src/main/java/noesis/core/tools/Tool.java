package noesis.core.tools;

import java.util.Map;

/**
 * A unit of work the model can decide to invoke.
 *
 * <p>Implementations receive the JSON arguments emitted by the model (already
 * parsed into a {@link Map}) and return a textual result that is fed back to the
 * model as a {@code tool} message. Implementations may throw; the executor
 * turns the exception into a recoverable error message rather than crashing the
 * agent loop.</p>
 */
@FunctionalInterface
public interface Tool {
    String run(Map<String, Object> arguments) throws Exception;
}
