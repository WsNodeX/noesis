package noesis.core.retrieval;

import java.util.ArrayList;
import java.util.List;

/**
 * An in-memory dense vector index with exact cosine-similarity search.
 *
 * <p>It is intentionally a single-file, allocation-light index: every document
 * is stored alongside its embedding and a query scans all of them, ranking by
 * cosine similarity. For the corpora typical of a RAG demo or a small knowledge
 * base (tens of thousands of passages) this brute-force scan is both fast and
 * exact &mdash; and the {@link Embedding} seam makes swapping in an approximate
 * index (HNSW, IVF) a local change.</p>
 */
public final class VectorStore {
    private final List<float[]> vectors = new ArrayList<>();
    private final List<Document> documents = new ArrayList<>();

    /** Index a document using a pre-computed embedding. */
    public void add(Document document, float[] embedding) {
        if (embedding == null) throw new IllegalArgumentException("embedding is null");
        vectors.add(embedding);
        documents.add(document);
    }

    /** Index a document by computing its embedding on the fly. */
    public void add(Document document, Embedding embedding) {
        add(document, embedding.embed(document.text()));
    }

    public int size() {
        return documents.size();
    }

    /**
     * Return the {@code k} documents most similar to {@code query}, highest
     * score first.
     */
    public List<ScoredDocument> search(float[] query, int k) {
        if (query == null) throw new IllegalArgumentException("query is null");
        List<ScoredDocument> scored = new ArrayList<>(documents.size());
        for (int i = 0; i < documents.size(); i++) {
            double sim = cosine(query, vectors.get(i));
            scored.add(new ScoredDocument(documents.get(i), sim));
        }
        scored.sort((a, b) -> Double.compare(b.score(), a.score()));
        return scored.subList(0, Math.min(k, scored.size()));
    }

    /** Cosine similarity between two L2-normalised-or-not vectors. */
    public static double cosine(float[] a, float[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("vector dimension mismatch");
        }
        double dot = 0.0, na = 0.0, nb = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot += (double) a[i] * b[i];
            na += (double) a[i] * a[i];
            nb += (double) b[i] * b[i];
        }
        double denom = Math.sqrt(na) * Math.sqrt(nb);
        return denom == 0.0 ? 0.0 : dot / denom;
    }
}
