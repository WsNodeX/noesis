package noesis.core.llm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.function.Consumer;

/**
 * Server-Sent Events (SSE) parsing helpers used by the streaming client.
 *
 * <p>OpenAI-compatible endpoints emit one JSON payload per {@code data:} line
 * and terminate the stream with {@code data: [DONE]}. This utility extracts the
 * payload of each data line and detects the terminal marker, while remaining
 * fully decoupled from any HTTP library.</p>
 */
public final class SseParser {

    private SseParser() {}

    /**
     * Extract the payload from a single SSE line.
     *
     * @return the decoded data payload, or {@code null} when the line is blank,
     *         a comment (starts with {@code :}), or not a {@code data:} field.
     */
    public static String dataOf(String line) {
        if (line == null) return null;
        String trimmed = line.trim();
        if (trimmed.isEmpty()) return null;
        if (trimmed.startsWith(":")) return null;
        if (!trimmed.startsWith("data:")) return null;
        String payload = trimmed.substring("data:".length()).trim();
        return payload.isEmpty() ? null : payload;
    }

    /** Whether the supplied payload marks the end of the stream. */
    public static boolean isDone(String payload) {
        return "[DONE]".equals(payload);
    }

    /**
     * Read a full SSE stream from {@code reader}, invoking {@code onData} for
     * every non-terminal {@code data:} payload. The {@code [DONE]} marker and
     * blank lines are consumed silently.
     */
    public static void consume(Reader reader, Consumer<String> onData) throws IOException {
        try (BufferedReader br = new BufferedReader(reader)) {
            String line;
            while ((line = br.readLine()) != null) {
                String payload = dataOf(line);
                if (payload == null) continue;
                if (isDone(payload)) return;
                onData.accept(payload);
            }
        }
    }
}
