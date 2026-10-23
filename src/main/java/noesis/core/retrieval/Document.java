package noesis.core.retrieval;

import java.util.Collections;
import java.util.Map;

/**
 * A retrievable unit of text with optional metadata.
 *
 * <p>Documents are usually produced by chunking a larger source with
 * {@link TextChunker}; the {@link #metadata()} map can carry provenance such as
 * the source file name or page number so answers can cite their origin.</p>
 */
public final class Document {
    private final String id;
    private final String text;
    private final Map<String, String> metadata;

    public Document(String id, String text) {
        this(id, text, Map.of());
    }

    public Document(String id, String text, Map<String, String> metadata) {
        this.id = id;
        this.text = text;
        this.metadata = metadata == null ? Map.of() : Collections.unmodifiableMap(metadata);
    }

    public String id() {
        return id;
    }

    public String text() {
        return text;
    }

    public Map<String, String> metadata() {
        return metadata;
    }

    @Override
    public String toString() {
        return "Document{id='" + id + "', text='" + truncate(text, 48) + "'}";
    }

    private static String truncate(String s, int n) {
        if (s == null) return "";
        return s.length() <= n ? s : s.substring(0, n) + "...";
    }
}
