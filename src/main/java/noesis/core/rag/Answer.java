package noesis.core.rag;

import java.util.List;

import noesis.core.retrieval.ScoredDocument;

/** The result of a RAG query: the generated answer plus its cited sources. */
public final class Answer {
    private final String content;
    private final List<ScoredDocument> sources;

    public Answer(String content, List<ScoredDocument> sources) {
        this.content = content;
        this.sources = sources;
    }

    public String content() {
        return content;
    }

    public List<ScoredDocument> sources() {
        return sources;
    }
}
