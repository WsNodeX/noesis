package noesis.core.retrieval;

import java.util.List;

/**
 * High-level semantic search over a {@link VectorStore}.
 *
 * <p>Given a natural-language query, the retriever embeds it and returns the
 * most relevant passages. It is the retrieval half of a RAG pipeline and can
 * also be used standalone for "find similar" features.</p>
 */
public final class Retriever {
    private final VectorStore store;
    private final Embedding embedding;
    private final int topK;

    public Retriever(VectorStore store, Embedding embedding) {
        this(store, embedding, 4);
    }

    public Retriever(VectorStore store, Embedding embedding, int topK) {
        this.store = store;
        this.embedding = embedding;
        this.topK = topK;
    }

    public List<ScoredDocument> retrieve(String query) {
        return store.search(embedding.embed(query), topK);
    }

    public int topK() {
        return topK;
    }
}
