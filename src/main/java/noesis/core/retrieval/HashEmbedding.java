package noesis.core.retrieval;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * A deterministic, dependency-free embedding based on the hashing trick.
 *
 * <p>Each token is hashed into a fixed number of buckets; its contribution is
 * signed by the token hash and weighted by its (log) frequency. The resulting
 * vector is L2-normalised, so cosine similarity between two texts correlates
 * with their lexical overlap. This is intentionally simple &mdash; it lets the
 * retrieval and RAG layers be exercised end-to-end without any network call or
 * external model, and it is trivially replaced by a state-of-the-art embedding
 * in production.</p>
 */
public final class HashEmbedding implements Embedding {

    private final int dimensions;
    private final boolean caseSensitive;

    public HashEmbedding() {
        this(256, false);
    }

    public HashEmbedding(int dimensions, boolean caseSensitive) {
        this.dimensions = dimensions;
        this.caseSensitive = caseSensitive;
    }

    @Override
    public float[] embed(String text) {
        float[] vector = new float[dimensions];
        Map<String, Integer> counts = tokenize(text);
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            int hash = entry.getKey().hashCode();
            int bucket = Math.floorMod(hash, dimensions);
            int sign = hash < 0 ? -1 : 1;
            double weight = 1.0 + Math.log1p(entry.getValue());
            vector[bucket] += sign * weight;
        }
        normalize(vector);
        return vector;
    }

    private Map<String, Integer> tokenize(String text) {
        Map<String, Integer> counts = new HashMap<>();
        if (text == null) return counts;
        String normalized = caseSensitive ? text : text.toLowerCase(Locale.ROOT);
        // Keep runs of letters/numbers as tokens; drop punctuation.
        StringBuilder token = new StringBuilder();
        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                token.append(c);
            } else if (token.length() > 0) {
                counts.merge(token.toString(), 1, Integer::sum);
                token.setLength(0);
            }
        }
        if (token.length() > 0) {
            counts.merge(token.toString(), 1, Integer::sum);
        }
        return counts;
    }

    private static void normalize(float[] v) {
        double norm = 0.0;
        for (float x : v) norm += (double) x * x;
        norm = Math.sqrt(norm);
        if (norm == 0.0) return;
        for (int i = 0; i < v.length; i++) v[i] /= (float) norm;
    }
}
