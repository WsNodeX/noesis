package noesis.core.retrieval;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Splits long documents into smaller, retrievable passages.
 *
 * <p>Text is first broken into sentences on sentence boundaries; sentences are
 * then greedily packed into chunks that stay under {@code maxChars} while
 * carrying a configurable character {@code overlap} from the previous chunk so
 * that information spanning a boundary is not lost.</p>
 */
public final class TextChunker {
    private static final Pattern SENTENCE = Pattern.compile("(?<=[.!?。！？])\\s+");

    private final int maxChars;
    private final int overlap;

    public TextChunker() {
        this(400, 80);
    }

    public TextChunker(int maxChars, int overlap) {
        if (overlap >= maxChars) {
            throw new IllegalArgumentException("overlap must be smaller than maxChars");
        }
        this.maxChars = maxChars;
        this.overlap = overlap;
    }

    public List<String> chunk(String text) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) return chunks;

        String[] sentences = SENTENCE.split(text.trim());
        StringBuilder current = new StringBuilder();
        for (String sentence : sentences) {
            String piece = sentence.trim();
            if (piece.isEmpty()) continue;
            if (current.length() > 0 && current.length() + piece.length() + 1 > maxChars) {
                chunks.add(current.toString().trim());
                // Start the next chunk by carrying over the tail for context.
                String carried = tail(current.toString(), overlap);
                current.setLength(0);
                current.append(carried);
                if (!carried.isEmpty()) current.append(' ');
            }
            if (current.length() > 0) current.append(' ');
            current.append(piece);
        }
        if (current.length() > 0) {
            chunks.add(current.toString().trim());
        }
        return chunks;
    }

    private static String tail(String s, int n) {
        if (s.length() <= n) return s;
        return s.substring(s.length() - n);
    }
}
